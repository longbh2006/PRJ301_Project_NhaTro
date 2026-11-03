package util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Ký và kiểm chữ ký VNPay 2.1.0: sắp tham số vnp_* theo tên, nối key=value đã
 * URL-encode bằng '&', rồi HMAC-SHA512 với HashSecret.
 */
public final class VnPaySigner {

    public static final String SECURE_HASH = "vnp_SecureHash";
    public static final String SECURE_HASH_TYPE = "vnp_SecureHashType";

    private VnPaySigner() {
    }

    /** Trả query đã có vnp_SecureHash ở cuối, dùng nối sau dấu '?' của URL cổng. */
    public static String signedQuery(Map<String, String> params, String hashSecret) {
        String query = canonicalQuery(params);
        return query + "&" + SECURE_HASH + "=" + hmacSha512(hashSecret, query);
    }

    /**
     * Kiểm chữ ký của tham số VNPay gửi về (IPN, return). Chỉ dùng các tham số
     * vnp_* trừ hai trường chữ ký; giá trị đã được container giải mã nên phải
     * encode lại đúng như lúc VNPay ký.
     */
    public static boolean verify(Map<String, String[]> requestParams, String hashSecret) {
        String[] received = requestParams.get(SECURE_HASH);
        if (received == null || received.length != 1 || received[0].isBlank()) {
            return false;
        }
        Map<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("vnp_") || SECURE_HASH.equals(key) || SECURE_HASH_TYPE.equals(key)) {
                continue;
            }
            // Một tham số lặp lại là dữ liệu bị chỉnh, không phải dữ liệu VNPay đã ký.
            if (entry.getValue().length != 1) {
                return false;
            }
            fields.put(key, entry.getValue()[0]);
        }
        byte[] expected = hmacSha512(hashSecret, canonicalQuery(fields)).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = received[0].toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    static String canonicalQuery(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            joiner.add(encode(entry.getKey()) + "=" + encode(value));
        }
        return joiner.toString();
    }

    private static String encode(String value) {
        // Mã nguồn mẫu của VNPay encode theo US-ASCII (dấu cách thành '+'); phải giống để chữ ký khớp.
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA512.", ex);
        }
    }
}
