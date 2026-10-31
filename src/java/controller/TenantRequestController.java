package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import model.RentalRequest;
import model.RentalRoom;
import service.RentalService;

public class TenantRequestController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final RentalService rentalService = new RentalService();

    private Integer requireTenant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer tenantId = requireTenant(request, response);
        if (tenantId == null) {
            return;
        }
        if (!"/tenant/requests".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
            String query = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
            List<RentalRequest> rentalRequests = rentalService.findRequestsByTenant(tenantId, status, query);
            request.setAttribute("rentalRequests", rentalRequests);
            request.setAttribute("selectedStatus", status);
            request.setAttribute("searchQuery", query);
            if (request.getParameter("requestId") != null) {
                int requestId = Integer.parseInt(request.getParameter("requestId"));
                RentalRequest selectedRequest = rentalRequests.stream()
                        .filter(item -> item.getRequestId() == requestId).findFirst().orElse(null);
                if (selectedRequest == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("selectedRequest", selectedRequest);
            }
            HttpSession session = request.getSession(false);
            if (session.getAttribute("rentalCsrfToken") == null) {
                session.setAttribute("rentalCsrfToken", UUID.randomUUID().toString());
            }
            request.setAttribute("successMessage", session.getAttribute("rentalSuccess"));
            session.removeAttribute("rentalSuccess");
            request.getRequestDispatcher("/WEB-INF/views/tenant/requests.jsp").forward(request, response);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã yêu cầu không hợp lệ.");
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải yêu cầu thuê.", ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Giải mã trước lần đọc tham số đầu tiên; không thay đổi chuỗi filter của dự án.
        request.setCharacterEncoding("UTF-8");
        Integer tenantId = requireTenant(request, response);
        if (tenantId == null) {
            return;
        }
        if ("/tenant/requests/cancel".equals(request.getServletPath())) {
            cancelRequest(request, response, tenantId);
            return;
        }
        try {
            int roomId = Integer.parseInt(request.getParameter("roomId"));
            int months = Integer.parseInt(request.getParameter("months"));
            int numPeople = Integer.parseInt(request.getParameter("numPeople"));
            String dateValue = request.getParameter("moveInDate");
            if (dateValue == null || dateValue.isBlank()) {
                throw new IllegalArgumentException("Vui lòng chọn ngày dọn vào.");
            }
            LocalDate moveInDate = LocalDate.parse(dateValue);
            rentalService.createRequest(tenantId, roomId, moveInDate,
                    months, numPeople, request.getParameter("note"));
            request.getSession().setAttribute("rentalSuccess", "Đã gửi yêu cầu thuê phòng. Vui lòng chờ duyệt.");
            response.sendRedirect(request.getContextPath() + "/tenant/requests");
            return;
        } catch (NumberFormatException ex) {
            request.setAttribute("errorMessage", "Mã phòng, số tháng và số người phải là số nguyên hợp lệ.");
        } catch (DateTimeParseException ex) {
            request.setAttribute("errorMessage", "Ngày dọn vào không hợp lệ.");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể lưu yêu cầu thuê. Vui lòng thử lại sau.", ex);
        }
        try {
            showRoomDetail(request, response);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phòng không hợp lệ.");
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải lại thông tin phòng.", ex);
        }
    }

    private void cancelRequest(HttpServletRequest request, HttpServletResponse response, int tenantId)
            throws ServletException, IOException {
        Object csrfToken = request.getSession(false).getAttribute("rentalCsrfToken");
        if (!(csrfToken instanceof String) || !csrfToken.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            int requestId = Integer.parseInt(request.getParameter("requestId"));
            rentalService.cancelRequest(tenantId, requestId);
            request.getSession(false).setAttribute("rentalSuccess", "Đã hủy yêu cầu thuê phòng.");
            response.sendRedirect(request.getContextPath() + "/tenant/requests");
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã yêu cầu không hợp lệ.");
        } catch (IllegalStateException ex) {
            response.sendError(HttpServletResponse.SC_CONFLICT, ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể hủy yêu cầu thuê. Vui lòng thử lại sau.", ex);
        }
    }

    private void showRoomDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        int roomId = Integer.parseInt(request.getParameter("roomId"));
        if (roomId <= 0) {
            throw new NumberFormatException();
        }
        RentalRoom room = rentalService.findRoom(roomId);
        if (room == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("room", room);
        request.getRequestDispatcher("/WEB-INF/views/rooms/detail.jsp").forward(request, response);
    }
}
