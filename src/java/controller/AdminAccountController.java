package controller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Account;
import service.AccountService;
import service.AccountService.AccountPageData;

public class AdminAccountController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminAccountController.class.getName());

    private int parseIntRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là một số nguyên hợp lệ.");
        }
    }

    private void loadAccountListAttributes(HttpServletRequest request, AccountService service) {
        String keyword = request.getParameter("keyword");
        String role = request.getParameter("role");
        String pageStr = request.getParameter("page");

        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String cleanRole = service.normalizeRoleFilter(role);

        AccountPageData pageData = service.getAccountPageData(cleanKeyword, cleanRole, pageStr);

        request.setAttribute("accounts", pageData.getAccounts());
        request.setAttribute("currentPage", pageData.getCurrentPage());
        request.setAttribute("totalPages", pageData.getTotalPages());
        request.setAttribute("filterKeyword", cleanKeyword);
        request.setAttribute("filterRole", cleanRole);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        AccountService service = new AccountService();

        try {
            if ("create".equals(action)) {
                request.getRequestDispatcher("/WEB-INF/views/admin/accounts/form.jsp").forward(request, response);
            } else {
                loadAccountListAttributes(request, service);
                request.getRequestDispatcher("/WEB-INF/views/admin/accounts/list.jsp").forward(request, response);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải dữ liệu trang Quản lý tài khoản", e);
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        AccountService service = new AccountService();

        if ("toggleStatus".equals(action)) {
            handleToggleStatus(request, response, service);
        } else if ("create".equals(action)) {
            handleCreateManager(request, response, service);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
        }
    }

    private void handleToggleStatus(HttpServletRequest request, HttpServletResponse response, AccountService service)
            throws ServletException, IOException {
        try {
            int targetAccountId = parseIntRequired(request.getParameter("accountId"), "ID tài khoản");
            String activeParam = request.getParameter("active");
            boolean newActiveStatus = "1".equals(activeParam) || "true".equalsIgnoreCase(activeParam);

            HttpSession session = request.getSession(false);
            Account currentAdmin = (session != null) ? (Account) session.getAttribute("account") : null;

            service.updateAccountStatus(targetAccountId, newActiveStatus, currentAdmin);

            String msg = newActiveStatus ? "unlocked" : "locked";
            response.sendRedirect(request.getContextPath() + "/admin/accounts?msg=" + msg);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            try {
                loadAccountListAttributes(request, service);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Lỗi tải lại danh sách tài khoản sau khi khóa thất bại", ex);
            }
            request.getRequestDispatcher("/WEB-INF/views/admin/accounts/list.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Lỗi CSDL khi khóa/mở khóa tài khoản", e);
            request.setAttribute("errorMessage", "Sự cố hệ thống khi cập nhật trạng thái tài khoản.");
            try {
                loadAccountListAttributes(request, service);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Lỗi tải lại danh sách tài khoản", ex);
            }
            request.getRequestDispatcher("/WEB-INF/views/admin/accounts/list.jsp").forward(request, response);
        }
    }

    private void handleCreateManager(HttpServletRequest request, HttpServletResponse response, AccountService service)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            service.createManager(username, password, fullName, phone, email);
            response.sendRedirect(request.getContextPath() + "/admin/accounts?msg=created");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputFullName", fullName);
            request.setAttribute("inputPhone", phone);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/WEB-INF/views/admin/accounts/form.jsp").forward(request, response);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Lỗi CSDL khi tạo tài khoản MANAGER", e);
            request.setAttribute("errorMessage", "Sự cố hệ thống. Vui lòng thử lại sau.");
            request.setAttribute("inputUsername", username);
            request.setAttribute("inputFullName", fullName);
            request.setAttribute("inputPhone", phone);
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/WEB-INF/views/admin/accounts/form.jsp").forward(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Controller quản lý tài khoản người dùng và tạo tài khoản MANAGER.";
    }
}