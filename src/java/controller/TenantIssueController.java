package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Account;
import service.MaintenanceService;

public class TenantIssueController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TenantIssueController.class.getName());

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

        MaintenanceService service = new MaintenanceService();
        try {
            request.setAttribute("hasActiveContract", service.hasActiveContract(account.getAccountId()));
            request.setAttribute("issues", service.getIssuesByTenant(account.getAccountId()));
            request.getRequestDispatcher("/WEB-INF/views/tenant/issues.jsp").forward(request, response);
        } catch (SQLException e) {
            // SỬA LỖI 4: Ném lỗi lên Container để web.xml xử lý thay vì tự forward trang 500
            LOGGER.log(Level.SEVERE, "Lỗi CSDL khi tải trang sự cố cho tenant", e);
            throw new ServletException("Sự cố hệ thống khi tải dữ liệu.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // SỬA LỖI 1: Báo cho server đọc dữ liệu Form theo UTF-8 TRƯỚC KHI lấy tham số
        request.setCharacterEncoding("UTF-8");

        Account account = getSessionAccount(request);
        if (account == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập.");
            return;
        }

        String action = request.getParameter("action");
        MaintenanceService service = new MaintenanceService();

        if ("create".equals(action)) {
            String title = request.getParameter("title");
            String description = request.getParameter("description");

            try {
                service.createIssue(account.getAccountId(), title, description);
                response.sendRedirect(request.getContextPath() + "/tenant/issues?msg=created");
            } catch (IllegalArgumentException e) {
                // Validation hỏng -> Trả lại Form
                request.setAttribute("errorMessage", e.getMessage());
                request.setAttribute("inputTitle", title);
                request.setAttribute("inputDescription", description);
                
                try {
                    request.setAttribute("hasActiveContract", service.hasActiveContract(account.getAccountId()));
                    request.setAttribute("issues", service.getIssuesByTenant(account.getAccountId()));
                } catch (SQLException ex) {
                     // SỬA LỖI 6: Không nuốt lỗi âm thầm khi nạp lại danh sách thất bại
                     LOGGER.log(Level.SEVERE, "Lỗi tải lại danh sách sự cố", ex);
                     throw new ServletException("Lỗi nạp lại danh sách sau khi validation hỏng.", ex);
                }
                request.getRequestDispatcher("/WEB-INF/views/tenant/issues.jsp").forward(request, response);
            } catch (SQLException e) {
                // SỬA LỖI 4: Để lỗi lên container
                LOGGER.log(Level.SEVERE, "Lỗi CSDL khi tenant tạo sự cố", e);
                throw new ServletException("Sự cố hệ thống khi ghi nhận sự cố.", e);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
        }
    }
    
    @Override
    public String getServletInfo() {
        return "Controller dành cho người thuê quản lý và báo cáo sự cố (Maintenance).";
    }
}