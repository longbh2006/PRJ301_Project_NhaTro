package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import service.FakeVnPayService;
import util.AppUrl;

/**
 * /payment/fake-vnpay: cổng VNPay giả cho máy local (xem FakeVnPayService).
 * GET có tham số vnp_*: trang chọn kết quả. POST action=pay|cancel: ký kết quả,
 * gọi IPN. POST action=resend: gọi lại IPN với đúng bộ tham số cũ.
 * GET ?view=result: xem phản hồi IPN và nút quay về website.
 */
public class FakeVnPayController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String RESULT_QUERY = "fakeVnpResultQuery";
    private static final String IPN_LOG = "fakeVnpIpnLog";
    private static final String RETURN_URL = "fakeVnpReturnUrl";
    private final FakeVnPayService fakeService = new FakeVnPayService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!fakeService.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if ("result".equals(request.getParameter("view"))) {
            HttpSession session = request.getSession(false);
            Object resultQuery = session == null ? null : session.getAttribute(RESULT_QUERY);
            if (!(resultQuery instanceof String)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("ipnLog", session.getAttribute(IPN_LOG));
            request.setAttribute("returnUrl", fakeService.returnUrl((String) session.getAttribute(RETURN_URL),
                    (String) resultQuery));
            request.getRequestDispatcher("/WEB-INF/views/payment/fake-vnpay.jsp").forward(request, response);
            return;
        }
        Map<String, String[]> params = request.getParameterMap();
        if (!fakeService.verifyPaymentUrl(params)) {
            request.setAttribute("signatureError", true);
        } else {
            request.setAttribute("paymentParams", vnpParams(params));
            request.setAttribute("amount", fakeService.displayAmount(params));
            if (fakeService.wantsQr(params)) {
                String token = fakeService.createQrTicket(params);
                String base = fakeService.qrBaseUrl(request.getScheme(), request.getServerName(),
                        request.getServerPort(), request.getContextPath());
                request.setAttribute("qrToken", token);
                request.setAttribute("qrUrl", base + "/payment/fake-vnpay/qr?token=" + token);
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/payment/fake-vnpay.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!fakeService.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String action = request.getParameter("action");
        HttpSession session = request.getSession();
        String resultQuery;
        if ("pay".equals(action) || "cancel".equals(action)) {
            // Các trường ẩn có thể bị sửa trên trình duyệt ⇒ kiểm lại chữ ký trước khi ký kết quả.
            if (!fakeService.verifyPaymentUrl(request.getParameterMap())) {
                request.setAttribute("signatureError", true);
                request.getRequestDispatcher("/WEB-INF/views/payment/fake-vnpay.jsp").forward(request, response);
                return;
            }
            resultQuery = fakeService.buildResultQuery(request.getParameterMap(), "pay".equals(action));
            session.setAttribute(RESULT_QUERY, resultQuery);
            // vnp_ReturnUrl đã nằm trong chữ ký vừa kiểm nên không bị sửa thành địa chỉ lạ.
            session.setAttribute(RETURN_URL, request.getParameter("vnp_ReturnUrl"));
            session.setAttribute(IPN_LOG, new ArrayList<String>());
        } else if ("resend".equals(action) && session.getAttribute(RESULT_QUERY) instanceof String) {
            resultQuery = (String) session.getAttribute(RESULT_QUERY);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> ipnLog = (List<String>) session.getAttribute(IPN_LOG);
        ipnLog.add(fakeService.callIpn(resultQuery, AppUrl.loopback(request)));
        response.sendRedirect(request.getContextPath() + "/payment/fake-vnpay?view=result");
    }

    private static Map<String, String> vnpParams(Map<String, String[]> params) {
        Map<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && entry.getValue().length == 1) {
                fields.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return fields;
    }
}
