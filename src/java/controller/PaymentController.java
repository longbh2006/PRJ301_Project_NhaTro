package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import service.PaymentService;

/**
 * GET /payment/ipn: VNPay gọi máy chủ, trả JSON RspCode; nơi DUY NHẤT ghi PAID.
 * GET /payment/return: trình duyệt quay về, chỉ hiển thị kết quả.
 * Không yêu cầu đăng nhập vì VNPay gọi IPN không có session; dữ liệu được tin nhờ chữ ký.
 */
public class PaymentController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/payment/ipn".equals(request.getServletPath())) {
            PaymentService.IpnResult result = paymentService.handleIpn(request.getParameterMap());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result.toJson());
            return;
        }
        try {
            request.setAttribute("result", paymentService.readReturn(request.getParameterMap()));
            request.getRequestDispatcher("/WEB-INF/views/payment/return.jsp").forward(request, response);
        } catch (SQLException ex) {
            throw new ServletException("Không thể đọc kết quả thanh toán. Vui lòng thử lại sau.", ex);
        }
    }
}
