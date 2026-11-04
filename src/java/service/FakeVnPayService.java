package service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import util.VnPayConfig;
import util.VnPaySigner;

/**
 * Cổng VNPay giả để chạy local: máy dev không có URL công khai nên VNPay
 * sandbox không gọi được IPN. Cổng giả làm đúng phần việc của VNPay: kiểm chữ
 * ký URL thanh toán, ký kết quả bằng cùng HashSecret, gọi IPN máy chủ rồi đưa
 * trình duyệt về ReturnUrl. Chỉ bật khi cấu hình vnp_FakeGateway=true.
 */
public class FakeVnPayService {

    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** Cổng giả tắt thì controller trả 404 như endpoint không tồn tại. */
    public boolean isEnabled() {
        return VnPayConfig.load().isFakeGateway();
    }

    /** VNPay thật báo "Sai chữ ký" khi URL bị sửa (ví dụ sửa vnp_Amount); cổng giả làm giống vậy. */
    public boolean verifyPaymentUrl(Map<String, String[]> params) {
        return VnPaySigner.verify(params, VnPayConfig.load().requireMerchant().getHashSecret());
    }

    /**
     * Dựng tham số kết quả như VNPay gửi về (đã ký).
     * success = true: thẻ NCB trả thành công; false: khách huỷ (mã 24).
     */
    public String buildResultQuery(Map<String, String[]> paymentParams, boolean success) {
        VnPayConfig config = VnPayConfig.load().requireMerchant();
        Map<String, String> result = new HashMap<>();
        result.put("vnp_TmnCode", single(paymentParams, "vnp_TmnCode"));
        result.put("vnp_Amount", single(paymentParams, "vnp_Amount"));
        result.put("vnp_TxnRef", single(paymentParams, "vnp_TxnRef"));
        result.put("vnp_OrderInfo", single(paymentParams, "vnp_OrderInfo"));
        // Người thuê chọn VNPAY-QR thì kết quả ghi đúng phương thức đó, còn lại giả lập thẻ NCB.
        boolean qr = PaymentService.BANK_QR.equals(single(paymentParams, "vnp_BankCode"));
        result.put("vnp_BankCode", qr ? PaymentService.BANK_QR : "NCB");
        result.put("vnp_PayDate", VNP_TIME.format(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        if (success) {
            String transactionNo = String.valueOf(10_000_000 + RANDOM.nextInt(90_000_000));
            result.put("vnp_ResponseCode", "00");
            result.put("vnp_TransactionStatus", "00");
            result.put("vnp_TransactionNo", transactionNo);
            result.put("vnp_BankTranNo", "VNP" + transactionNo);
            result.put("vnp_CardType", qr ? "QRCODE" : "ATM");
        } else {
            result.put("vnp_ResponseCode", "24");
            result.put("vnp_TransactionStatus", "02");
            result.put("vnp_TransactionNo", "0");
        }
        return VnPaySigner.signedQuery(result, config.getHashSecret());
    }

    /** Gọi IPN máy chủ như VNPay (server → server); trả nội dung phản hồi để hiển thị. */
    public String callIpn(String resultQuery) {
        String ipnUrl = VnPayConfig.load().getIpnUrl();
        if (ipnUrl == null) {
            throw new IllegalStateException("Cổng giả cần vnp_IpnUrl (hoặc VNPAY_IPN_URL) để gọi IPN.");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(ipnUrl + "?" + resultQuery))
                .timeout(Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + " " + response.body();
        } catch (IOException ex) {
            return "Không gọi được IPN: " + ex.getMessage();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "Bị ngắt khi gọi IPN.";
        }
    }

    /** VNPay đưa trình duyệt về ReturnUrl kèm đúng bộ tham số đã gửi cho IPN. */
    public String returnUrl(String resultQuery) {
        return VnPayConfig.load().requireMerchant().getReturnUrl() + "?" + resultQuery;
    }

    /** Số tiền VND thật để hiển thị (vnp_Amount là số tiền × 100). */
    public BigDecimal displayAmount(Map<String, String[]> params) {
        try {
            return new BigDecimal(single(params, "vnp_Amount")).movePointLeft(2);
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    private static String single(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        return values == null || values.length != 1 ? null : values[0];
    }
}
