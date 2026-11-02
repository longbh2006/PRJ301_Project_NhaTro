package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.MaintenanceRequest;
import service.MaintenanceService;
import service.RentalService;


public class ManagerIssueController extends HttpServlet {

    private final MaintenanceService maintenanceService = new MaintenanceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String status = request.getParameter("status");
        String roomIdStr = request.getParameter("roomId");
        Integer roomId = null;

        try {
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                roomId = Integer.parseInt(roomIdStr.trim());
            }

            List<MaintenanceRequest> issues = maintenanceService.getIssuesForManager(status, roomId);
            request.setAttribute("issues", issues);
            request.getRequestDispatcher("/WEB-INF/views/manager/issues.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Mã phòng không hợp lệ (phải là số).");
            request.getRequestDispatcher("/WEB-INF/views/manager/issues.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/manager/issues.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Database error loading manager issues", e);
            throw new ServletException("Lỗi hệ thống khi tải danh sách sự cố.", e);
        }
    }

    // ...
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

    

        String ticketIdStr = request.getParameter("ticketId");
        String currentStatus = request.getParameter("currentStatus");
        String newStatus = request.getParameter("newStatus");
        String managerNote = request.getParameter("managerNote");

        try {
            int ticketId = Integer.parseInt(ticketIdStr);
            maintenanceService.updateIssueStatus(ticketId, currentStatus, newStatus, managerNote);

            request.getSession().setAttribute("successMessage", "Cập nhật trạng thái sự cố thành công.");
            response.sendRedirect(request.getContextPath() + "/manager/issues");

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "ID sự cố không hợp lệ.");
            doGet(request, response);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        } catch (RentalService.BusinessException e) {
            response.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Database error updating issue status", e);
            throw new ServletException("Lỗi hệ thống khi cập nhật sự cố.", e);
        }
    }
}
