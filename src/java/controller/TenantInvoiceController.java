package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Account;
import model.Invoice;
import service.InvoiceService;
import service.InvoiceService.InvoicePage;

/**
 * UC06 · NT-38: người thuê xem danh sách và chi tiết hoá đơn của mình.
 */
public class TenantInvoiceController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TenantInvoiceController.class.getName());
    private final InvoiceService invoiceService = new InvoiceService();

    private Account getSessionAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (Account) session.getAttribute("account");
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Account account = getSessionAccount(request);
        if (account == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            if ("/tenant/invoices/detail".equals(request.getServletPath())) {
                showDetail(request, response, account.getAccountId());
            } else {
                showList(request, response, account.getAccountId());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi CSDL khi tải hoá đơn của người thuê", e);
            throw new ServletException("Không thể tải hoá đơn. Vui lòng thử lại sau.", e);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, int tenantId)
            throws ServletException, IOException, SQLException {
        String pageParam = request.getParameter("page");
        Integer page = (pageParam == null || pageParam.isBlank()) ? Integer.valueOf(1) : parsePositiveInt(pageParam);
        if (page == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Số trang không hợp lệ.");
            return;
        }

        InvoicePage invoicePage = invoiceService.findPageForTenant(tenantId, page);
        request.setAttribute("invoices", invoicePage.getInvoices());
        request.setAttribute("totalInvoices", invoicePage.getTotal());
        request.setAttribute("currentPage", invoicePage.getPage());
        request.setAttribute("totalPages", invoicePage.getTotalPages());
        request.getRequestDispatcher("/WEB-INF/views/tenant/invoices.jsp").forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, int tenantId)
            throws ServletException, IOException, SQLException {
        Integer invoiceId = parsePositiveInt(request.getParameter("id"));
        if (invoiceId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã hoá đơn không hợp lệ.");
            return;
        }

        Invoice invoice = invoiceService.findDetailForTenant(tenantId, invoiceId);
        if (invoice == null) {
            // Không phân biệt "không tồn tại" và "của người khác" để không lộ id hoá đơn hợp lệ.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("invoice", invoice);
        request.getRequestDispatcher("/WEB-INF/views/tenant/invoice-detail.jsp").forward(request, response);
    }

    private Integer parsePositiveInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            int number = Integer.parseInt(value.trim());
            return number > 0 ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getServletInfo() {
        return "Controller cho người thuê xem hoá đơn của mình (UC06).";
    }
}
