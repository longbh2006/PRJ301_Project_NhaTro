package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import service.ReportService;

public class ManagerReportController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ReportService service = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        String yearValue = request.getParameter("year");
        yearValue = yearValue == null ? "" : yearValue.trim();
        request.setAttribute("selectedYear", yearValue.isEmpty() ? String.valueOf(today.getYear()) : yearValue);
        request.setAttribute("minYear", ReportService.MIN_YEAR);
        request.setAttribute("maxYear", ReportService.MAX_YEAR);
        try {
            int year = yearValue.isEmpty() ? today.getYear() : parseYear(yearValue);
            request.setAttribute("report", service.buildReport(year, today));
            request.setAttribute("today", today);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải báo cáo doanh thu.", ex);
        }
        request.getRequestDispatcher("/WEB-INF/views/manager/report.jsp").forward(request, response);
    }

    private int parseYear(String value) {
        if (!value.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("Năm báo cáo phải là số gồm 4 chữ số.");
        }
        return Integer.parseInt(value);
    }
}
