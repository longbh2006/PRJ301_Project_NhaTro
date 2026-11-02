package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import service.BillingService;
import service.BillingService.BillingException;
import service.BillingService.GenerationResult;
import service.BillingService.ReadingInput;

/**
 *
 * @author Lenovo
 */
public class MeterController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CSRF_KEY = "managerMetersCsrf";
    private static final String SUCCESS_KEY = "managerMetersSuccess";
    private static final String WARNING_KEY = "managerMetersWarning";
    private final BillingService service = new BillingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (managerId(request, response) == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        request.setAttribute("successMessage", session.getAttribute(SUCCESS_KEY));
        request.setAttribute("warningMessage", session.getAttribute(WARNING_KEY));
        session.removeAttribute(SUCCESS_KEY);
        session.removeAttribute(WARNING_KEY);
        YearMonth period;
        Integer roomId;
        try {
            period = readPeriod(request);
            roomId = optionalRoomId(request);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessages", List.of(ex.getMessage()));
            period = defaultPeriod();
            roomId = null;
        }
        showPage(request, response, period, roomId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (managerId(request, response) == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        Object token = session.getAttribute(CSRF_KEY);
        if (!(token instanceof String) || !token.equals(request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        YearMonth period;
        Integer roomId;
        try {
            if (request.getParameter("year") == null || request.getParameter("month") == null) {
                throw new IllegalArgumentException("Thiếu kỳ ghi số.");
            }
            period = readPeriod(request);
            roomId = optionalRoomId(request);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        try {
            String action = request.getParameter("action");
            if ("save".equals(action)) {
                int saved = service.saveReadings(period, readInputs(request));
                session.setAttribute(SUCCESS_KEY, "Đã lưu chỉ số tháng " + label(period) + " cho "
                        + saved + " phòng.");
            } else if ("generate".equals(action)) {
                storeGenerationMessages(session, period, service.generateInvoices(period));
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            String destination = request.getContextPath() + "/manager/meters?year=" + period.getYear()
                    + "&month=" + period.getMonthValue();
            if (roomId != null) {
                destination += "&roomId=" + roomId;
            }
            response.sendRedirect(destination);
            return;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessages", List.of(ex.getMessage()));
        } catch (BillingException ex) {
            response.setStatus(ex.isConflict() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessages", ex.getMessages());
        } catch (SQLException ex) {
            throw new ServletException("Không thể lưu chỉ số hoặc lập hoá đơn.", ex);
        }
        showPage(request, response, period, roomId);
    }

    private void storeGenerationMessages(HttpSession session, YearMonth period, GenerationResult result) {
        String skipped = result.getAlreadyInvoiced() > 0
                ? " " + result.getAlreadyInvoiced() + " hợp đồng đã có hoá đơn kỳ này nên không lập lại." : "";
        if (result.getCreated() > 0) {
            session.setAttribute(SUCCESS_KEY, "Đã lập " + result.getCreated() + " hoá đơn tháng " + label(period)
                    + ", trạng thái Chưa trả, hạn trả ngày 10 tháng sau." + skipped);
        }
        if (!result.getMissingRooms().isEmpty()) {
            session.setAttribute(WARNING_KEY, "Chưa lập hoá đơn cho phòng " + String.join(", ", result.getMissingRooms())
                    + " vì chưa đủ chỉ số tháng " + label(period) + " hoặc tháng trước.");
        } else if (result.getCreated() == 0) {
            session.setAttribute(WARNING_KEY, result.getAlreadyInvoiced() > 0
                    ? "Không lập thêm hoá đơn: mọi hợp đồng đã có hoá đơn tháng " + label(period) + "."
                    : "Không có hợp đồng đang hiệu lực để lập hoá đơn tháng " + label(period) + ".");
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, YearMonth period, Integer roomId)
            throws ServletException, IOException {
        BillingService.MeterSheet sheet;
        try {
            try {
                sheet = service.loadSheet(period);
            } catch (BillingException ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("errorMessages", ex.getMessages());
                roomId = null;
                sheet = service.loadSheet(defaultPeriod());
            }
        } catch (BillingException | SQLException ex) {
            throw new ServletException("Không thể tải chỉ số điện nước.", ex);
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
        request.setAttribute("sheet", sheet);
        request.setAttribute("roomId", roomId);
        request.setAttribute("minYear", BillingService.MIN_YEAR);
        request.setAttribute("maxYear", YearMonth.now().getYear());
        request.getRequestDispatcher("/WEB-INF/views/manager/meters.jsp").forward(request, response);
    }

    /** Mỗi dòng gửi kèm meterRoomId; ô chỉ số đặt tên electric_{roomId}, water_{roomId}. */
    private List<ReadingInput> readInputs(HttpServletRequest request) {
        String[] values = request.getParameterValues("meterRoomId");
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Không có phòng nào để lưu chỉ số.");
        }
        Set<Integer> roomIds = new LinkedHashSet<>();
        for (String value : values) {
            roomIds.add(positiveInt(value, "Mã phòng"));
        }
        List<ReadingInput> inputs = new ArrayList<>();
        for (int roomId : roomIds) {
            inputs.add(new ReadingInput(roomId, parameter(request, "electric_" + roomId),
                    parameter(request, "water_" + roomId)));
        }
        return inputs;
    }

    /** Không truyền year và month thì mở tháng vừa qua, kỳ thường ghi số cuối tháng. */
    private YearMonth readPeriod(HttpServletRequest request) {
        String year = parameter(request, "year");
        String month = parameter(request, "month");
        if (year.isEmpty() && month.isEmpty()) {
            return defaultPeriod();
        }
        if (!year.matches("[0-9]{4}") || !month.matches("[0-9]{1,2}")) {
            throw new IllegalArgumentException("Năm phải gồm 4 chữ số và tháng phải là số từ 1 đến 12.");
        }
        int monthValue = Integer.parseInt(month);
        if (monthValue < 1 || monthValue > 12) {
            throw new IllegalArgumentException("Tháng phải là số từ 1 đến 12.");
        }
        return YearMonth.of(Integer.parseInt(year), monthValue);
    }

    private YearMonth defaultPeriod() {
        return YearMonth.now().minusMonths(1);
    }

    private Integer optionalRoomId(HttpServletRequest request) {
        String value = parameter(request, "roomId");
        return value.isEmpty() ? null : positiveInt(value, "Mã phòng");
    }

    private int positiveInt(String value, String label) {
        if (value == null || !value.matches("[0-9]{1,9}") || Integer.parseInt(value) == 0) {
            throw new IllegalArgumentException(label + " phải là số nguyên dương.");
        }
        return Integer.parseInt(value);
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private String label(YearMonth period) {
        return String.format("%02d/%d", period.getMonthValue(), period.getYear());
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
}
