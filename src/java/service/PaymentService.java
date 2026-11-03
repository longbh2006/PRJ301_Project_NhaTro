package service;

import dal.PaymentDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Invoice;
import model.Payment;
import util.VnPayConfig;
import util.VnPaySigner;

/**
 * UC07 thanh toán hoá đơn qua VNPay. Luật chính: chỉ IPN (đã kiểm chữ ký,
 * đúng số tiền) được chuyển hoá đơn sang PAID; trang return chỉ đọc.
 */
public class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    // VNPay dùng giờ Việt Nam cho vnp_CreateDate/vnp_ExpireDate.
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;
    private static final int DUPLICATE_KEY = 2627;

    public Invoice findInvoiceForTenant(int tenantId, int invoiceId) throws SQLException {
        requirePositive(tenantId, invoiceId);
        try (PaymentDAO dao = new PaymentDAO()) {
            requireConnection(dao);
            return dao.findInvoiceForTenant(invoiceId, tenantId);
        }
    }

    /**
     * Tạo lần thử PENDING rồi dựng URL cổng thanh toán đã ký.
     * Trả null khi hoá đơn không tồn tại hoặc không thuộc người thuê này.
     */
    public String createPaymentUrl(int tenantId, int invoiceId, String clientIp) throws SQLException {
        requirePositive(tenantId, invoiceId);
        // Kiểm cấu hình trước khi ghi CSDL để không để lại lần thử PENDING không dùng được.
        VnPayConfig config = VnPayConfig.load().requireMerchant();
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String txnRef = "INV" + invoiceId + "-" + VNP_TIME.format(now);
        Payment payment;
        try (PaymentDAO dao = new PaymentDAO()) {
            requireConnection(dao);
            Invoice invoice = dao.findInvoiceForTenant(invoiceId, tenantId);
            if (invoice == null) {
                return null;
            }
            if (!Invoice.STATUS_UNPAID.equals(invoice.getStatus())) {
                throw new IllegalStateException("Hoá đơn này đã được thanh toán.");
            }
            try {
                payment = dao.insertPending(invoiceId, tenantId, txnRef);
            } catch (SQLException ex) {
                if (ex.getErrorCode() == DUPLICATE_KEY) {
                    throw new IllegalStateException("Bạn vừa tạo giao dịch cho hoá đơn này. Vui lòng thử lại sau vài giây.");
                }
                throw ex;
            }
            if (payment == null) {
                // IPN của lần thử khác vừa ghi PAID giữa lúc đọc và lúc INSERT.
                throw new IllegalStateException("Hoá đơn này đã được thanh toán.");
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        // VNPay nhận số tiền × 100 (không có phần thập phân).
        params.put("vnp_Amount", payment.getAmount().movePointRight(2).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTxnRef());
        // VNPay yêu cầu nội dung không dấu.
        params.put("vnp_OrderInfo", "Thanh toan hoa don " + invoiceId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", VNP_TIME.format(now));
        params.put("vnp_ExpireDate", VNP_TIME.format(now.plusMinutes(PAYMENT_TIMEOUT_MINUTES)));
        return config.getPayUrl() + "?" + VnPaySigner.signedQuery(params, config.getHashSecret());
    }

    /**
     * Q16b: xử lý IPN. Kiểm chữ ký → tìm lần thử theo txnRef → kiểm số tiền →
     * đã xử lý rồi thì trả 02 → ghi Payment + Invoice trong MỘT transaction.
     */
    public IpnResult handleIpn(Map<String, String[]> params) {
        VnPayConfig config;
        try {
            config = VnPayConfig.load().requireMerchant();
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.SEVERE, "IPN VNPay: thiếu cấu hình", ex);
            return IpnResult.UNKNOWN_ERROR;
        }
        if (!VnPaySigner.verify(params, config.getHashSecret())) {
            return IpnResult.INVALID_SIGNATURE;
        }
        String txnRef = single(params, "vnp_TxnRef");
        String responseCode = single(params, "vnp_ResponseCode");
        String transactionStatus = single(params, "vnp_TransactionStatus");
        BigDecimal vnpAmount;
        try {
            vnpAmount = new BigDecimal(single(params, "vnp_Amount"));
        } catch (NumberFormatException | NullPointerException ex) {
            return IpnResult.INVALID_AMOUNT;
        }
        if (txnRef == null) {
            return IpnResult.ORDER_NOT_FOUND;
        }

        try (PaymentDAO dao = new PaymentDAO()) {
            Connection connection = requireConnection(dao);
            connection.setAutoCommit(false);
            try {
                IpnResult result = applyIpn(dao, txnRef, vnpAmount, responseCode, transactionStatus,
                        single(params, "vnp_TransactionNo"), single(params, "vnp_BankCode"));
                if (result == IpnResult.CONFIRMED) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); }
                catch (SQLException rollbackError) { ex.addSuppressed(rollbackError); }
                throw ex;
            }
        } catch (SQLException | RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "IPN VNPay lỗi với txnRef " + txnRef, ex);
            return IpnResult.UNKNOWN_ERROR;
        }
    }

    private IpnResult applyIpn(PaymentDAO dao, String txnRef, BigDecimal vnpAmount, String responseCode,
            String transactionStatus, String transactionNo, String bankCode) throws SQLException {
        Payment payment = dao.lockByTxnRef(txnRef);
        if (payment == null) {
            return IpnResult.ORDER_NOT_FOUND;
        }
        if (payment.getAmount().movePointRight(2).compareTo(vnpAmount) != 0) {
            return IpnResult.INVALID_AMOUNT;
        }
        // IPN gọi lại lần 2: đã ghi kết quả rồi thì chỉ báo "đã xác nhận", không ghi thêm.
        if (!"PENDING".equals(payment.getStatus())) {
            return IpnResult.ALREADY_CONFIRMED;
        }
        boolean success = "00".equals(responseCode)
                && (transactionStatus == null || "00".equals(transactionStatus));
        if (!dao.finishPending(payment.getPaymentId(), success, transactionNo, bankCode, responseCode)) {
            return IpnResult.ALREADY_CONFIRMED;
        }
        if (success && !dao.markInvoicePaid(payment.getInvoiceId())) {
            // Hoá đơn đã PAID qua một lần thử khác ⇒ huỷ cả transaction, giữ lần thử này PENDING để đối soát/hoàn tiền.
            LOGGER.log(Level.SEVERE, "IPN VNPay: hoá đơn {0} đã PAID trước đó, cần đối soát giao dịch {1}",
                    new Object[]{payment.getInvoiceId(), txnRef});
            return IpnResult.ALREADY_CONFIRMED;
        }
        return IpnResult.CONFIRMED;
    }

    /** Trang return: kiểm chữ ký rồi chỉ ĐỌC trạng thái hiện tại, không bao giờ ghi PAID. */
    public ReturnResult readReturn(Map<String, String[]> params) throws SQLException {
        VnPayConfig config = VnPayConfig.load().requireMerchant();
        if (!VnPaySigner.verify(params, config.getHashSecret())) {
            return new ReturnResult(false, null, null);
        }
        String txnRef = single(params, "vnp_TxnRef");
        Payment payment = null;
        if (txnRef != null) {
            try (PaymentDAO dao = new PaymentDAO()) {
                requireConnection(dao);
                payment = dao.findByTxnRef(txnRef);
            }
        }
        return new ReturnResult(true, single(params, "vnp_ResponseCode"), payment);
    }

    private static String single(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        return values == null || values.length != 1 || values[0].isBlank() ? null : values[0];
    }

    private static void requirePositive(int tenantId, int invoiceId) {
        if (tenantId <= 0 || invoiceId <= 0) {
            throw new IllegalArgumentException("Mã hoá đơn không hợp lệ.");
        }
    }

    private static Connection requireConnection(PaymentDAO dao) throws SQLException {
        if (dao.getConnection() == null) {
            throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
        }
        return dao.getConnection();
    }

    /** Mã trả lời IPN theo tài liệu VNPay. */
    public enum IpnResult {
        CONFIRMED("00", "Confirm Success"),
        ORDER_NOT_FOUND("01", "Order not found"),
        ALREADY_CONFIRMED("02", "Order already confirmed"),
        INVALID_AMOUNT("04", "Invalid amount"),
        INVALID_SIGNATURE("97", "Invalid signature"),
        UNKNOWN_ERROR("99", "Unknown error");

        private final String code;
        private final String message;

        IpnResult(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }

        public String toJson() {
            return "{\"RspCode\":\"" + code + "\",\"Message\":\"" + message + "\"}";
        }
    }

    public static class ReturnResult {
        private final boolean validSignature;
        private final String responseCode;
        private final Payment payment;

        public ReturnResult(boolean validSignature, String responseCode, Payment payment) {
            this.validSignature = validSignature;
            this.responseCode = responseCode;
            this.payment = payment;
        }

        public boolean isValidSignature() { return validSignature; }
        public String getResponseCode() { return responseCode; }
        public Payment getPayment() { return payment; }
    }
}
