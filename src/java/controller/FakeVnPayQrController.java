package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import service.FakeVnPayService;
import service.FakeVnPayService.QrTicket;
import util.FlatJson;

/**
 * QR của cổng VNPay giả (chỉ chạy local, tắt cùng vnp_FakeGateway).
 * GET  /payment/fake-vnpay/qr?token=      trang điện thoại mở sau khi quét mã.
 * POST /payment/fake-vnpay/qr             token, action=pay|cancel: ký kết quả, gọi IPN.
 * GET  /payment/fake-vnpay/qr-status?token=  JSON cho trang máy tính hỏi định kỳ.
 * Điện thoại không có session đăng nhập; mã vé ngẫu nhiên 128 bit, dùng một lần, hết hạn 15 phút.
 */
public class FakeVnPayQrController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final FakeVnPayService fakeService = new FakeVnPayService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!fakeService.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        QrTicket ticket = fakeService.findQrTicket(request.getParameter("token"));
        if ("/payment/fake-vnpay/qr-status".equals(request.getServletPath())) {
            writeStatus(response, ticket);
            return;
        }
        if (ticket == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Mã QR không tồn tại hoặc đã hết hạn.");
            return;
        }
        request.setAttribute("ticket", ticket);
        request.setAttribute("token", request.getParameter("token"));
        request.getRequestDispatcher("/WEB-INF/views/payment/fake-vnpay-qr.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!fakeService.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String token = request.getParameter("token");
        QrTicket ticket = fakeService.findQrTicket(token);
        String action = request.getParameter("action");
        if (ticket == null || !("pay".equals(action) || "cancel".equals(action))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            // false = vé đã được xử lý (quét lại / bấm hai lần): không gọi IPN lần nữa, chỉ hiện kết quả cũ.
            fakeService.completeQrTicket(ticket, "pay".equals(action));
        } catch (IllegalStateException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        if ("desktop".equals(request.getParameter("from"))) {
            response.sendRedirect(fakeService.returnUrl(ticket.getResultQuery()));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/payment/fake-vnpay/qr?token=" + token);
    }

    private void writeStatus(HttpServletResponse response, QrTicket ticket) throws IOException {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("found", String.valueOf(ticket != null));
        boolean done = ticket != null && ticket.isDone();
        status.put("done", String.valueOf(done));
        if (done) {
            status.put("paid", String.valueOf(ticket.isPaid()));
            status.put("returnUrl", fakeService.returnUrl(ticket.getResultQuery()));
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(FlatJson.write(status));
    }
}
