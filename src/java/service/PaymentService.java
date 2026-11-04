package service;

import dal.PaymentDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Invoice;
import model.Payment;
import util.FlatJson;
import util.VnPayConfig;
import util.VnPaySigner;

/**
 * UC07 thanh toán hoá đơn qua VNPay. Luật chính: chỉ dữ liệu máy chủ-với-máy chủ
 * đã kiểm chữ ký và đúng số tiền mới chuyển hoá đơn sang PAID — IPN do VNPay gọi,
 * hoặc phản hồi querydr do máy chủ tự hỏi VNPay. Tham số trên URL return chỉ được dùng khi
 * bật vnp_TrustReturnUrl (chạy local với sandbox, mặc định tắt).
 */
public class PaymentService {

    /** Phương thức gửi trong vnp_BankCode; "" = để VNPay cho người dùng chọn. */
    public static final String BANK_QR = "VNPAYQR";
    public static final Set<String> BANK_CODES = Set.of("", BANK_QR, "VNBANK", "INTCARD");

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    private static final String VNP_VERSION = "2.1.0";
    // querydr yêu cầu IP máy chủ gọi API; máy local gọi ra ngoài từ chính nó.
    private static final String QUERY_IP = "127.0.0.1";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
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
    public String createPaymentUrl(int tenantId, int invoiceId, String clientIp, String bankCode)
            throws SQLException {
        requirePositive(tenantId, invoiceId);
        if (bankCode == null || !BANK_CODES.contains(bankCode)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
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
        params.put("vnp_Version", VNP_VERSION);
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
        // Bỏ trống thì VNPay cho người dùng tự chọn phương thức trên trang của VNPay.
        if (!bankCode.isEmpty()) {
            params.put("vnp_BankCode", bankCode);
        }
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

        return applyInTransaction(txnRef, vnpAmount, responseCode, transactionStatus,
                single(params, "vnp_TransactionNo"), single(params, "vnp_BankCode"));
    }

    /** Ghi kết quả VNPay (từ IPN hoặc querydr) trong MỘT transaction; lỗi CSDL trả 99. */
    private IpnResult applyInTransaction(String txnRef, BigDecimal vnpAmount, String responseCode,
            String transactionStatus, String transactionNo, String bankCode) {
        try (PaymentDAO dao = new PaymentDAO()) {
            Connection connection = requireConnection(dao);
            connection.setAutoCommit(false);
            try {
                IpnResult result = applyIpn(dao, txnRef, vnpAmount, responseCode, transactionStatus,
                        transactionNo, bankCode);
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
            LOGGER.log(Level.SEVERE, "Ghi kết quả VNPay lỗi với txnRef " + txnRef, ex);
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

    /**
     * Trang return: kiểm chữ ký rồi đọc trạng thái. Nếu lần thử còn PENDING (VNPay sandbox
     * không gọi được IPN về máy local), máy chủ tự hỏi VNPay qua API querydr và chỉ tin phản hồi
     * có checksum đúng; vẫn PENDING mà bật vnp_TrustReturnUrl thì ghi theo tham số return đã ký.
     */
    public ReturnResult readReturn(Map<String, String[]> params) throws SQLException {
        VnPayConfig config = VnPayConfig.load().requireMerchant();
        if (!VnPaySigner.verify(params, config.getHashSecret())) {
            return new ReturnResult(false, null, null);
        }
        String txnRef = single(params, "vnp_TxnRef");
        String responseCode = single(params, "vnp_ResponseCode");
        Payment payment = txnRef == null ? null : findPayment(txnRef);
        // Cổng giả local đã gọi IPN thật và không có API querydr.
        if (payment != null && "PENDING".equals(payment.getStatus()) && !config.isFakeGateway()) {
            confirmByQueryDr(config, payment, single(params, "vnp_TransactionNo"), responseCode);
            payment = findPayment(txnRef);
            if (payment != null && "PENDING".equals(payment.getStatus()) && config.isTrustReturnUrl()) {
                confirmByReturn(config, params, txnRef);
                payment = findPayment(txnRef);
            }
        }
        return new ReturnResult(true, responseCode, payment);
    }

    /**
     * Chế độ local với sandbox (vnp_TrustReturnUrl=true): ghi kết quả từ tham số return đã kiểm
     * chữ ký HashSecret và đúng TmnCode. Số tiền và trạng thái PENDING vẫn do applyIpn kiểm như IPN,
     * nên tải lại trang return không ghi lần hai. Tắt mặc định vì người dùng có thể không quay về.
     */
    private void confirmByReturn(VnPayConfig config, Map<String, String[]> params, String txnRef) {
        String responseCode = single(params, "vnp_ResponseCode");
        if (responseCode == null || !config.getTmnCode().equals(single(params, "vnp_TmnCode"))) {
            LOGGER.log(Level.WARNING, "return {0}: thiếu vnp_ResponseCode hoặc sai vnp_TmnCode", txnRef);
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(single(params, "vnp_Amount"));
        } catch (NumberFormatException | NullPointerException ex) {
            LOGGER.log(Level.WARNING, "return {0}: vnp_Amount không hợp lệ", txnRef);
            return;
        }
        IpnResult result = applyInTransaction(txnRef, amount, responseCode, single(params, "vnp_TransactionStatus"),
                single(params, "vnp_TransactionNo"), single(params, "vnp_BankCode"));
        LOGGER.log(Level.INFO, "return {0}: mã {1}, ghi kết quả {2}",
                new Object[]{txnRef, responseCode, result.getCode()});
    }

    private Payment findPayment(String txnRef) throws SQLException {
        try (PaymentDAO dao = new PaymentDAO()) {
            requireConnection(dao);
            return dao.findByTxnRef(txnRef);
        }
    }

    /**
     * Gọi API querydr (máy chủ → VNPay) theo tài liệu "Truy vấn và hoàn tiền" 2.1.0.
     * Chỉ ghi kết quả khi phản hồi có checksum đúng, vnp_ResponseCode = 00, đúng
     * TmnCode và TxnRef; số tiền vẫn được applyIpn so với Payment trong CSDL.
     * Lỗi mạng hoặc phản hồi lạ thì giữ PENDING (trang return báo "đang chờ").
     */
    private void confirmByQueryDr(VnPayConfig config, Payment payment, String transactionNo,
            String returnResponseCode) {
        String txnRef = payment.getTxnRef();
        // txnRef = INV<id>-<vnp_CreateDate>; vnp_TransactionDate phải bằng đúng vnp_CreateDate đã gửi.
        String transactionDate = txnRef.substring(txnRef.lastIndexOf('-') + 1);
        if (!transactionDate.matches("[0-9]{14}")) {
            LOGGER.log(Level.WARNING, "querydr: txnRef {0} không chứa ngày tạo", txnRef);
            return;
        }
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = VNP_TIME.format(ZonedDateTime.now(VN_ZONE));
        String orderInfo = "Truy van giao dich " + txnRef;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VNP_VERSION);
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", config.getTmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        if (transactionNo != null && transactionNo.matches("[0-9]{1,15}")) {
            body.put("vnp_TransactionNo", transactionNo);
        }
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", QUERY_IP);
        body.put("vnp_SecureHash", VnPaySigner.pipeChecksum(config.getHashSecret(), requestId, VNP_VERSION,
                "querydr", config.getTmnCode(), txnRef, transactionDate, createDate, QUERY_IP, orderInfo));

        Map<String, String> reply;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.getApiUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(FlatJson.write(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.log(Level.WARNING, "querydr {0}: HTTP {1}", new Object[]{txnRef, response.statusCode()});
                return;
            }
            reply = FlatJson.parse(response.body());
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "querydr " + txnRef + ": không gọi/đọc được phản hồi VNPay", ex);
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }

        String expectedHash = VnPaySigner.pipeChecksum(config.getHashSecret(), reply.get("vnp_ResponseId"),
                reply.get("vnp_Command"), reply.get("vnp_ResponseCode"), reply.get("vnp_Message"),
                reply.get("vnp_TmnCode"), reply.get("vnp_TxnRef"), reply.get("vnp_Amount"),
                reply.get("vnp_BankCode"), reply.get("vnp_PayDate"), reply.get("vnp_TransactionNo"),
                reply.get("vnp_TransactionType"), reply.get("vnp_TransactionStatus"), reply.get("vnp_OrderInfo"),
                reply.get("vnp_PromotionCode"), reply.get("vnp_PromotionAmount"));
        if (!VnPaySigner.sameHash(expectedHash, reply.get("vnp_SecureHash"))) {
            LOGGER.log(Level.WARNING, "querydr {0}: sai checksum phản hồi (mã {1}, {2})",
                    new Object[]{txnRef, reply.get("vnp_ResponseCode"), reply.get("vnp_Message")});
            return;
        }
        if (!"00".equals(reply.get("vnp_ResponseCode"))
                || !config.getTmnCode().equals(reply.get("vnp_TmnCode"))
                || !txnRef.equals(reply.get("vnp_TxnRef"))) {
            LOGGER.log(Level.INFO, "querydr {0}: VNPay trả mã {1} ({2})",
                    new Object[]{txnRef, reply.get("vnp_ResponseCode"), reply.get("vnp_Message")});
            return;
        }
        String status = reply.get("vnp_TransactionStatus");
        String recordedCode;
        if ("00".equals(status)) {
            recordedCode = "00";
        } else if ("02".equals(status)) {
            // Giao dịch lỗi/huỷ: lưu mã lỗi VNPay gửi ở trang return (đã kiểm chữ ký), ví dụ 24 = khách huỷ.
            recordedCode = returnResponseCode == null || "00".equals(returnResponseCode) ? status : returnResponseCode;
        } else {
            // 01 = chưa hoàn tất, 04/05/06/07/09: để PENDING cho người quản lý đối soát.
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(reply.get("vnp_Amount"));
        } catch (NumberFormatException | NullPointerException ex) {
            LOGGER.log(Level.WARNING, "querydr {0}: vnp_Amount không hợp lệ", txnRef);
            return;
        }
        IpnResult result = applyInTransaction(txnRef, amount, recordedCode, status,
                reply.get("vnp_TransactionNo"), reply.get("vnp_BankCode"));
        LOGGER.log(Level.INFO, "querydr {0}: trạng thái {1}, ghi kết quả {2}",
                new Object[]{txnRef, status, result.getCode()});
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
