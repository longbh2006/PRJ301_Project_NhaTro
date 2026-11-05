package service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import util.VnPayConfig;
import util.VnPaySigner;

/**
 * Cổng VNPay giả để chạy local: máy dev không có URL công khai nên VNPay
 * sandbox không gọi được IPN. Cổng giả làm đúng phần việc của VNPay: kiểm chữ
 * ký URL thanh toán, ký kết quả bằng cùng HashSecret, gọi IPN máy chủ rồi đưa
 * trình duyệt về ReturnUrl. Chỉ bật khi cấu hình vnp_FakeGateway=true.
 *
 * Chọn VNPAY-QR thì cổng giả hiện mã QR trỏ tới trang xác nhận; điện thoại quét
 * mã (hoặc mở link trên máy) để thanh toán, trang máy tính tự chuyển về ReturnUrl.
 */
public class FakeVnPayService {

    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    // Điện thoại quét QR không có session của máy tính nên vé QR giữ ở bộ nhớ ứng dụng, sống bằng hạn thanh toán.
    private static final Duration QR_TTL = Duration.ofMinutes(15);
    private static final Map<String, QrTicket> QR_TICKETS = new ConcurrentHashMap<>();

    /** Một lần quét QR: tham số thanh toán đã kiểm chữ ký và kết quả sau khi xác nhận. */
    public static final class QrTicket {
        private final Map<String, String[]> paymentParams;
        private final Instant expiresAt;
        private String resultQuery;
        private String ipnLog;
        private boolean paid;

        private QrTicket(Map<String, String[]> paymentParams, Instant expiresAt) {
            this.paymentParams = paymentParams;
            this.expiresAt = expiresAt;
        }

        public String getTxnRef() { return single(paymentParams, "vnp_TxnRef"); }
        public String getOrderInfo() { return single(paymentParams, "vnp_OrderInfo"); }
        public BigDecimal getAmount() { return amountOf(paymentParams); }
        public synchronized boolean isDone() { return resultQuery != null; }
        public synchronized boolean isPaid() { return paid; }
        public synchronized String getIpnLog() { return ipnLog; }
        public synchronized String getResultQuery() { return resultQuery; }
    }

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
    public String callIpn(String resultQuery, String baseUrl) {
        String ipnUrl = VnPayConfig.load().getIpnUrl(baseUrl);
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

    /**
     * VNPay đưa trình duyệt về vnp_ReturnUrl của URL thanh toán (đã kiểm chữ ký) kèm đúng
     * bộ tham số đã gửi cho IPN; nhờ vậy về đúng host/cổng máy đã tạo giao dịch.
     */
    public String returnUrl(String vnpReturnUrl, String resultQuery) {
        if (vnpReturnUrl == null || vnpReturnUrl.isBlank()) {
            throw new IllegalStateException("URL thanh toán thiếu vnp_ReturnUrl.");
        }
        return vnpReturnUrl + "?" + resultQuery;
    }

    public String returnUrl(QrTicket ticket) {
        return returnUrl(single(ticket.paymentParams, "vnp_ReturnUrl"), ticket.getResultQuery());
    }

    /** Số tiền VND thật để hiển thị (vnp_Amount là số tiền × 100). */
    public BigDecimal displayAmount(Map<String, String[]> params) {
        return amountOf(params);
    }

    private static BigDecimal amountOf(Map<String, String[]> params) {
        try {
            return new BigDecimal(single(params, "vnp_Amount")).movePointLeft(2);
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    /** Chỉ hiện QR khi người thuê chọn VNPAY-QR ở bước trước. */
    public boolean wantsQr(Map<String, String[]> params) {
        return PaymentService.BANK_QR.equals(single(params, "vnp_BankCode"));
    }

    /** Tạo vé QR cho bộ tham số đã kiểm chữ ký; trả mã vé ngẫu nhiên 128 bit. */
    public String createQrTicket(Map<String, String[]> verifiedParams) {
        Instant now = Instant.now();
        QR_TICKETS.values().removeIf(ticket -> ticket.expiresAt.isBefore(now));
        Map<String, String[]> copy = new HashMap<>();
        verifiedParams.forEach((key, values) -> {
            if (key.startsWith("vnp_")) {
                copy.put(key, values.clone());
            }
        });
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        QR_TICKETS.put(token, new QrTicket(Collections.unmodifiableMap(copy), now.plus(QR_TTL)));
        return token;
    }

    /** null khi mã vé sai định dạng, không tồn tại hoặc đã hết hạn. */
    public QrTicket findQrTicket(String token) {
        if (token == null || !token.matches("[0-9a-f]{32}")) {
            return null;
        }
        QrTicket ticket = QR_TICKETS.get(token);
        if (ticket == null || ticket.expiresAt.isBefore(Instant.now())) {
            return null;
        }
        return ticket;
    }

    /**
     * Xác nhận (success) hoặc huỷ một vé QR: ký kết quả rồi gọi IPN như VNPay.
     * Mỗi vé chỉ dùng một lần; trả false khi vé đã được xử lý trước đó.
     */
    public boolean completeQrTicket(QrTicket ticket, boolean success, String baseUrl) {
        synchronized (ticket) {
            if (ticket.resultQuery != null) {
                return false;
            }
            // Kiểm lại chữ ký bằng HashSecret hiện tại trước khi ký kết quả.
            if (!verifyPaymentUrl(ticket.paymentParams)) {
                throw new IllegalStateException("Sai chữ ký (mã lỗi 70): dữ liệu thanh toán đã bị thay đổi.");
            }
            ticket.resultQuery = buildResultQuery(ticket.paymentParams, success);
            ticket.paid = success;
            ticket.ipnLog = callIpn(ticket.resultQuery, baseUrl);
            return true;
        }
    }

    /**
     * Địa chỉ gốc đưa vào mã QR. vnp_FakeQrBaseUrl được ưu tiên; không có thì giữ host
     * của trình duyệt, riêng 127.0.0.1/localhost đổi sang IP mạng LAN để điện thoại mở được.
     */
    public String qrBaseUrl(String scheme, String host, int port, String contextPath) {
        String configured = VnPayConfig.load().getFakeQrBaseUrl();
        if (configured != null) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        String qrHost = host;
        if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.") || "[::1]".equals(host)) {
            String lan = lanAddress();
            if (lan != null) {
                qrHost = lan;
            }
        }
        return scheme + "://" + qrHost + ":" + port + contextPath;
    }

    /** IPv4 mạng LAN, ưu tiên 192.168.x rồi 10.x rồi 172.16–31.x; null nếu không có. */
    private static String lanAddress() {
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (!(address instanceof Inet4Address) || !address.isSiteLocalAddress()) {
                        continue;
                    }
                    String ip = address.getHostAddress();
                    int rank = ip.startsWith("192.168.") ? 0 : ip.startsWith("10.") ? 1 : 2;
                    if (rank < bestRank) {
                        best = ip;
                        bestRank = rank;
                    }
                }
            }
        } catch (SocketException ex) {
            return null;
        }
        return best;
    }

    private static String single(Map<String, String[]> params, String name) {
        String[] values = params.get(name);
        return values == null || values.length != 1 ? null : values[0];
    }
}
