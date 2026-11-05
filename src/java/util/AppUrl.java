package util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Địa chỉ gốc của ứng dụng lấy từ request đang xử lý, để URL gửi cho VNPay
 * (ReturnUrl, IPN của cổng giả) theo đúng host/cổng máy đang chạy thay vì viết cứng 8080.
 */
public final class AppUrl {

    private AppUrl() {
    }

    /** scheme://host:cổng/contextPath mà trình duyệt đang dùng. */
    public static String of(HttpServletRequest request) {
        return request.getScheme() + "://" + host(request.getServerName()) + ":" + request.getServerPort()
                + request.getContextPath();
    }

    /** Máy chủ tự gọi chính nó (IPN của cổng giả): đi qua loopback và cổng connector đã nhận request. */
    public static String loopback(HttpServletRequest request) {
        return request.getScheme() + "://127.0.0.1:" + request.getLocalPort() + request.getContextPath();
    }

    // Host IPv6 phải nằm trong ngoặc vuông khi ghép với cổng.
    private static String host(String name) {
        return name.indexOf(':') >= 0 && !name.startsWith("[") ? "[" + name + "]" : name;
    }
}
