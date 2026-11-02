package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.Account;
import service.InvoiceService;
import service.MaintenanceService;

/**
 * Lớp chặn IDOR thứ 2, chạy sau AuthFilter (đã chắc chắn là TENANT): id trên
 * URL phải thuộc hợp đồng của người đang đăng nhập.
 */
public class OwnershipFilter implements Filter {

    private final InvoiceService invoiceService = new InvoiceService();
    private final MaintenanceService maintenanceService = new MaintenanceService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Id người thuê chỉ lấy từ session, không bao giờ lấy từ URL hoặc form.
        HttpSession session = httpReq.getSession(false);
        Account account = session == null ? null : (Account) session.getAttribute("account");
        int resourceId = parseId(httpReq.getParameter("id"));

        boolean owned;
        try {
            owned = account != null && resourceId > 0 && isOwned(httpReq.getServletPath(), resourceId, account.getAccountId());
        } catch (SQLException e) {
            throw new ServletException("Không thể kiểm tra quyền sở hữu dữ liệu.", e);
        }

        if (!owned) {
            // Trả 404 thay vì 403 để kẻ dò id không biết id đó có tồn tại hay không.
            httpResp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isOwned(String path, int resourceId, int tenantId) throws SQLException {
        switch (path) {
            case "/tenant/invoices/detail":
            case "/tenant/pay":
                return invoiceService.isInvoiceOwnedByTenant(resourceId, tenantId);
            case "/tenant/issues/detail":
                return maintenanceService.isIssueOwnedByTenant(resourceId, tenantId);
            default:
                // Đường dẫn được map vào filter nhưng chưa khai báo cách kiểm thì chặn luôn.
                return false;
        }
    }

    private int parseId(String raw) {
        if (raw == null) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
