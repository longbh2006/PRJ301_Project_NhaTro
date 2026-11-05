package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.Invoice;
import service.PaymentService;
import util.AppUrl;

/**
 * GET /tenant/pay?invoiceId=N: xác nhận trước khi trả.
 * POST /tenant/pay (invoiceId): tạo lần thử PENDING rồi redirect sang VNPay.
 */
public class TenantPaymentController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String FLASH_ERROR = "paymentError";
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer tenantId = tenantId(request, response);
        if (tenantId == null) {
            return;
        }
        Integer invoiceId = parseId(request.getParameter("invoiceId"));
        if (invoiceId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            Invoice invoice = paymentService.findInvoiceForTenant(tenantId, invoiceId);
            if (invoice == null) {
                // Không phân biệt "không tồn tại" và "của người khác" để không lộ id hợp lệ.
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            HttpSession session = request.getSession(false);
            request.setAttribute("errorMessage", session.getAttribute(FLASH_ERROR));
            session.removeAttribute(FLASH_ERROR);
            request.setAttribute("invoice", invoice);
            // Chỉ hiện phương thức cổng thanh toán đang cấu hình chạy được.
            request.setAttribute("bankCodes", paymentService.availableBankCodes());
            request.getRequestDispatcher("/WEB-INF/views/tenant/pay.jsp").forward(request, response);
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải hoá đơn. Vui lòng thử lại sau.", ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer tenantId = tenantId(request, response);
        if (tenantId == null) {
            return;
        }
        Integer invoiceId = parseId(request.getParameter("invoiceId"));
        if (invoiceId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // Thiếu tham số = "" (để VNPay cho chọn); service từ chối giá trị lạ hoặc phương thức đang ẩn.
        String bankCode = request.getParameter("bankCode");
        bankCode = bankCode == null ? "" : bankCode.trim();
        try {
            // ReturnUrl theo host/cổng trình duyệt đang dùng nên máy nào chạy cổng nào cũng quay về đúng.
            String paymentUrl = paymentService.createPaymentUrl(tenantId, invoiceId, request.getRemoteAddr(),
                    bankCode, AppUrl.of(request));
            if (paymentUrl == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.sendRedirect(paymentUrl);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            request.getSession(false).setAttribute(FLASH_ERROR, ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/tenant/pay?invoiceId=" + invoiceId);
        } catch (SQLException ex) {
            throw new ServletException("Không thể tạo giao dịch thanh toán. Vui lòng thử lại sau.", ex);
        }
    }

    /** ID người thuê lấy từ session đăng nhập, không nhận từ URL/form. */
    private Integer tenantId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        Object accountId = session.getAttribute("accountId");
        if (!"TENANT".equals(session.getAttribute("role"))
                || !(accountId instanceof Integer) || (Integer) accountId <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return (Integer) accountId;
    }

    private static Integer parseId(String value) {
        try {
            int id = Integer.parseInt(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
