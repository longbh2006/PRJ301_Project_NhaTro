package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cấu hình VNPay đọc lúc chạy, không viết cứng trong code.
 * Biến môi trường được ưu tiên; thiếu thì đọc WEB-INF/VNPay.properties
 * (file này nằm trong .gitignore, mẫu ở docs/VNPay.properties.example).
 */
public final class VnPayConfig {

    public static final String SANDBOX_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    /** API truy vấn giao dịch (querydr) của sandbox, theo tài liệu "Truy vấn và hoàn tiền" của VNPay. */
    public static final String SANDBOX_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    // Đường dẫn mặc định trong ứng dụng; ghép với địa chỉ gốc của request nên không cố định host/cổng.
    private static final String DEFAULT_RETURN_PATH = "/payment/return";
    private static final String DEFAULT_IPN_PATH = "/payment/ipn";

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String apiUrl;
    private final String returnUrl;
    private final String ipnUrl;
    private final boolean fakeGateway;
    private final String fakeQrBaseUrl;
    private final boolean trustReturnUrl;
    private final String bankCodes;

    private VnPayConfig(Properties file) {
        fakeQrBaseUrl = read(file, "VNPAY_FAKE_QR_BASE_URL", "vnp_FakeQrBaseUrl", null);
        tmnCode = read(file, "VNPAY_TMN_CODE", "vnp_TmnCode", null);
        hashSecret = read(file, "VNPAY_HASH_SECRET", "vnp_HashSecret", null);
        payUrl = read(file, "VNPAY_PAY_URL", "vnp_PayUrl", null);
        apiUrl = read(file, "VNPAY_API_URL", "vnp_ApiUrl", SANDBOX_API_URL);
        returnUrl = read(file, "VNPAY_RETURN_URL", "vnp_ReturnUrl", null);
        ipnUrl = read(file, "VNPAY_IPN_URL", "vnp_IpnUrl", null);
        fakeGateway = Boolean.parseBoolean(read(file, "VNPAY_FAKE_GATEWAY", "vnp_FakeGateway", "false"));
        trustReturnUrl = Boolean.parseBoolean(read(file, "VNPAY_TRUST_RETURN_URL", "vnp_TrustReturnUrl", "false"));
        bankCodes = read(file, "VNPAY_BANK_CODES", "vnp_BankCodes", null);
    }

    /** Đọc lại mỗi lần gọi để sửa file cấu hình không cần khởi động lại (Tomcat cache tài nguyên khoảng 5 giây). */
    public static VnPayConfig load() {
        Properties file = new Properties();
        // Cùng cách DBContext tìm ConnectDB.properties: từ WEB-INF/classes đi lên WEB-INF.
        try (InputStream in = VnPayConfig.class.getClassLoader().getResourceAsStream("../VNPay.properties")) {
            if (in != null) {
                file.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Không đọc được WEB-INF/VNPay.properties.", ex);
        }
        return new VnPayConfig(file);
    }

    private static String read(Properties file, String envName, String key, String fallback) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = file.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    /** Gọi trước khi ký hoặc kiểm chữ ký để thiếu cấu hình thì báo rõ thay vì ký bằng khoá rỗng. */
    public VnPayConfig requireMerchant() {
        if (tmnCode == null || hashSecret == null) {
            throw new IllegalStateException("Thiếu cấu hình VNPay: cần vnp_TmnCode, vnp_HashSecret"
                    + " (biến môi trường VNPAY_* hoặc WEB-INF/VNPay.properties).");
        }
        return this;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    /** baseUrl = scheme://host:cổng/contextPath của request đang xử lý (xem AppUrl). */
    public String getPayUrl(String baseUrl) {
        return resolve(payUrl, SANDBOX_PAY_URL, baseUrl);
    }

    /** URL API querydr; trang return gọi để tự xác nhận giao dịch khi IPN không tới được máy local. */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Bỏ trống hoặc ghi đường dẫn tương đối (vd. /payment/return) thì ghép với địa chỉ máy đang
     * chạy, nên mỗi máy dùng cổng Tomcat nào cũng được. URL tuyệt đối chỉ dùng khi cần cố định.
     */
    public String getReturnUrl(String baseUrl) {
        return resolve(returnUrl, DEFAULT_RETURN_PATH, baseUrl);
    }

    public String getIpnUrl(String baseUrl) {
        return resolve(ipnUrl, DEFAULT_IPN_PATH, baseUrl);
    }

    private static String resolve(String configured, String fallback, String baseUrl) {
        String value = configured == null ? fallback : configured;
        if (value.startsWith("/")) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + value;
        }
        return value;
    }

    /** Địa chỉ gốc (vd. http://192.168.1.5:8080/Project_NhaTro) cho mã QR của cổng giả; null = tự dò. */
    public String getFakeQrBaseUrl() {
        return fakeQrBaseUrl;
    }

    public boolean isFakeGateway() {
        return fakeGateway;
    }

    /**
     * Chỉ bật khi chạy local với VNPay sandbox: IPN không tới được máy local và querydr của
     * sandbox đang trả 99, nên cho ghi kết quả từ tham số return ĐÃ KIỂM CHỮ KÝ. Mặc định tắt.
     */
    public boolean isTrustReturnUrl() {
        return trustReturnUrl;
    }

    /** Danh sách phương thức terminal đã bật, cách nhau dấu phẩy (VNPAYQR, VNBANK, INTCARD, ANY); null = mặc định. */
    public String getBankCodes() {
        return bankCodes;
    }
}
