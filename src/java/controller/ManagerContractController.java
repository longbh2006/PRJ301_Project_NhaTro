package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import service.ContractService;

public class ManagerContractController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ContractService service = new ContractService();

    private Integer requireManager(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !"MANAGER".equals(session.getAttribute("role"))
                || !(session.getAttribute("accountId") instanceof Integer)
                || (Integer) session.getAttribute("accountId") <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return (Integer) session.getAttribute("accountId");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (requireManager(request, response) == null) { return; }
        try {
            Filters filters = readFilters(request);
            HttpSession session = request.getSession(false);
            request.setAttribute("successMessage", session.getAttribute("contractSuccess"));
            session.removeAttribute("contractSuccess");
            showPage(request, response, filters);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải danh sách hợp đồng.", ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Integer managerId = requireManager(request, response);
        if (managerId == null) { return; }
        HttpSession session = request.getSession(false);
        Object token = session.getAttribute("contractCsrfToken");
        if (!(token instanceof String) || !token.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Filters filters;
        try {
            filters = readFilters(request);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        try {
            if (!"end".equals(request.getParameter("action"))) {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            int contractId = positiveInt(request.getParameter("contractId"), "Mã hợp đồng");
            service.endContract(contractId, managerId);
            session.setAttribute("contractSuccess", "Đã kết thúc hợp đồng. Phòng không còn hợp đồng thuê đang hiệu lực.");
            String target = String.format("%s/manager/contracts?status=%s&roomId=%s&tenantName=%s&page=%d",
                    request.getContextPath(), filters.status(), filters.roomId() == null ? "" : filters.roomId(),
                    URLEncoder.encode(filters.tenantName(), StandardCharsets.UTF_8), filters.page());
            response.sendRedirect(target);
            return;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể kết thúc hợp đồng. Vui lòng thử lại.", ex);
        }
        try {
            showPage(request, response, filters);
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải lại danh sách hợp đồng.", ex);
        }
    }

    private Filters readFilters(HttpServletRequest request) {
        String status = parameter(request, "status");
        String tenantName = parameter(request, "tenantName");
        String roomValue = parameter(request, "roomId");
        String pageValue = parameter(request, "page");
        if (!Set.of("", "ACTIVE", "ENDED").contains(status)) {
            throw new IllegalArgumentException("Trạng thái hợp đồng không hợp lệ.");
        }
        if (tenantName.length() > 100) {
            throw new IllegalArgumentException("Tên người thuê không được vượt quá 100 ký tự.");
        }
        return new Filters(status, roomValue.isEmpty() ? null : positiveInt(roomValue, "Mã phòng"),
                tenantName, pageValue.isEmpty() ? 1 : positiveInt(pageValue, "Số trang"));
    }

    private int positiveInt(String value, String label) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new IllegalArgumentException(label + " phải là số nguyên dương.");
        }
        try {
            int number = Integer.parseInt(value);
            if (number > 0) { return number; }
        } catch (NumberFormatException ex) {
            // Giá trị vượt giới hạn int cũng là dữ liệu nhập không hợp lệ.
        }
        throw new IllegalArgumentException(label + " phải là số nguyên dương hợp lệ.");
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, Filters filters)
            throws SQLException, ServletException, IOException {
        request.setAttribute("contractPage", service.findPage(filters.status(), filters.roomId(),
                filters.tenantName(), filters.page()));
        request.setAttribute("selectedStatus", filters.status());
        request.setAttribute("selectedRoomId", filters.roomId());
        request.setAttribute("tenantName", filters.tenantName());
        HttpSession session = request.getSession(false);
        synchronized (session) {
            if (session.getAttribute("contractCsrfToken") == null) {
                session.setAttribute("contractCsrfToken", UUID.randomUUID().toString());
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/manager/contracts.jsp").forward(request, response);
    }

    private record Filters(String status, Integer roomId, String tenantName, int page) { }
}
