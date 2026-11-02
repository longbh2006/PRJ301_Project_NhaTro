package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import service.RentalService;

public class TenantContractController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final RentalService rentalService = new RentalService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        Object accountId = session.getAttribute("accountId");
        if (!"TENANT".equals(session.getAttribute("role"))
                || !(accountId instanceof Integer) || (Integer) accountId <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            request.setAttribute("contract", rentalService.findActiveContractByTenant((Integer) accountId));
            request.getRequestDispatcher("/WEB-INF/views/tenant/contract.jsp").forward(request, response);
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải hợp đồng thuê. Vui lòng thử lại sau.", ex);
        }
    }
}
