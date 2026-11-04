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

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String apiUrl;
    private final String returnUrl;
    private final String ipnUrl;
    private final boolean fakeGateway;

    private VnPayConfig(Properties file) {
        tmnCode = read(file, "VNPAY_TMN_CODE", "vnp_TmnCode", null);
        hashSecret = read(file, "VNPAY_HASH_SECRET", "vnp_HashSecret", null);
        payUrl = read(file, "VNPAY_PAY_URL", "vnp_PayUrl", SANDBOX_PAY_URL);
        apiUrl = read(file, "VNPAY_API_URL", "vnp_ApiUrl", SANDBOX_API_URL);
        returnUrl = read(file, "VNPAY_RETURN_URL", "vnp_ReturnUrl", null);
        ipnUrl = read(file, "VNPAY_IPN_URL", "vnp_IpnUrl", null);
        fakeGateway = Boolean.parseBoolean(read(file, "VNPAY_FAKE_GATEWAY", "vnp_FakeGateway", "false"));
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
        if (tmnCode == null || hashSecret == null || returnUrl == null) {
            throw new IllegalStateException("Thiếu cấu hình VNPay: cần vnp_TmnCode, vnp_HashSecret, vnp_ReturnUrl"
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

    public String getPayUrl() {
        return payUrl;
    }

    /** URL API querydr; trang return gọi để tự xác nhận giao dịch khi IPN không tới được máy local. */
    public String getApiUrl() {
        return apiUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public boolean isFakeGateway() {
        return fakeGateway;
    }
}
