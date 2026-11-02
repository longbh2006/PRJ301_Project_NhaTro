package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.Account;
import model.Invoice;
import service.InvoiceService;

/**
 * Chi tiết hoá đơn và trang xác nhận thanh toán của người thuê. OwnershipFilter
 * đã chặn id không thuộc người thuê; servlet vẫn đọc qua câu SELECT có
 * c.tenantId = ? nên bỏ filter cũng không lộ hoá đơn người khác.
 */
public class TenantInvoiceController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Account account = session == null ? null : (Account) session.getAttribute("account");
        if (account == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int invoiceId;
        try {
            invoiceId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Invoice invoice;
        try {
            invoice = invoiceService.getInvoiceForTenant(invoiceId, account.getAccountId());
        } catch (SQLException e) {
            throw new ServletException("Không thể tải hoá đơn. Vui lòng thử lại sau.", e);
        }
        if (invoice == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("invoice", invoice);
        String view = "/tenant/pay".equals(request.getServletPath())
                ? "/WEB-INF/views/tenant/pay.jsp"
                : "/WEB-INF/views/tenant/invoice-detail.jsp";
        request.getRequestDispatcher(view).forward(request, response);
    }
}
