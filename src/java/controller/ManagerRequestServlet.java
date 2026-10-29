package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import service.RentalService;
import service.RentalService.BusinessException;

@WebServlet(name = "ManagerRequestServlet", urlPatterns = {"/manager/requests"})
public class ManagerRequestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CSRF_KEY = "managerRequestsCsrf";
    private static final String FLASH_KEY = "managerRequestsSuccess";
    private final RentalService service = new RentalService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (managerId(request, response) == null) {
            return;
        }
        try {
            Integer roomId = optionalRoomId(request);
            HttpSession session = request.getSession(false);
            request.setAttribute("successMessage", session.getAttribute(FLASH_KEY));
            session.removeAttribute(FLASH_KEY);
            showRequests(request, response, roomId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", ex.getMessage());
            showRequests(request, response, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Integer managerId = managerId(request, response);
        if (managerId == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        Object token = session.getAttribute(CSRF_KEY);
        if (!(token instanceof String) || !token.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Integer roomId = null;
        try {
            roomId = optionalRoomId(request);
            int requestId = positiveInt(request.getParameter("requestId"), "Mã yêu cầu");
            request.setAttribute("formRequestId", requestId);
            String action = request.getParameter("action");
            String successMessage;
            if ("approve".equals(action)) {
                BigDecimal deposit = deposit(request.getParameter("deposit"));
                int electric = nonnegativeInt(request.getParameter("electricIndex"), "Chỉ số điện");
                int water = nonnegativeInt(request.getParameter("waterIndex"), "Chỉ số nước");
                service.approve(requestId, managerId, deposit, electric, water);
                successMessage = "Đã duyệt yêu cầu, tạo hợp đồng và từ chối các yêu cầu khác cùng phòng.";
            } else if ("reject".equals(action)) {
                service.reject(requestId, managerId);
                successMessage = "Đã từ chối yêu cầu thuê.";
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            session.setAttribute(FLASH_KEY, successMessage);
            String destination = request.getContextPath() + "/manager/requests";
            if (roomId != null) {
                destination += "?roomId=" + roomId;
            }
            response.sendRedirect(destination);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", ex.getMessage());
            showRequests(request, response, roomId);
        } catch (BusinessException ex) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute("errorMessage", ex.getMessage());
            showRequests(request, response, roomId);
        } catch (SQLException ex) {
            throw new ServletException("Không thể xử lý yêu cầu thuê.", ex);
        }
    }

    private void showRequests(HttpServletRequest request, HttpServletResponse response, Integer roomId)
            throws ServletException, IOException {
        try {
            request.setAttribute("pendingRequests", service.findPendingRequests(roomId));
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải yêu cầu thuê.", ex);
        }
        HttpSession session = request.getSession(false);
        synchronized (session) {
            if (session.getAttribute(CSRF_KEY) == null) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                session.setAttribute(CSRF_KEY, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            }
            request.setAttribute("csrfToken", session.getAttribute(CSRF_KEY));
        }
        request.setAttribute("roomId", roomId);
        request.getRequestDispatcher("/WEB-INF/views/manager/requests.jsp").forward(request, response);
    }

    /** Hợp đồng session dùng chung với controller đăng nhập: accountId và role. */
    private Integer managerId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("accountId") instanceof Integer)
                || (Integer) session.getAttribute("accountId") <= 0
                || !"MANAGER".equals(session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return (Integer) session.getAttribute("accountId");
    }

    private Integer optionalRoomId(HttpServletRequest request) {
        String value = request.getParameter("roomId");
        return value == null || value.isBlank() ? null : positiveInt(value, "Mã phòng");
    }

    private int positiveInt(String value, String label) {
        int number = nonnegativeInt(value, label);
        if (number == 0) {
            throw new IllegalArgumentException(label + " phải là số nguyên dương.");
        }
        return number;
    }

    private int nonnegativeInt(String value, String label) {
        try {
            if (value == null || !value.matches("[0-9]{1,10}")) {
                throw new NumberFormatException();
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " phải là số nguyên từ 0 đến 2.147.483.647.");
        }
    }

    private BigDecimal deposit(String value) {
        if (value == null || !value.matches("[0-9]{1,12}")) {
            throw new IllegalArgumentException("Tiền cọc phải là số nguyên từ 0 đến 999.999.999.999 đồng.");
        }
        return new BigDecimal(value);
    }
}
