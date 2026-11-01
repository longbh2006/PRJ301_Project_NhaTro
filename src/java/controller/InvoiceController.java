package controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BillingService;

public class InvoiceController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int year;
        int month;
        try {
            year = Integer.parseInt(request.getParameter("year"));
            month = Integer.parseInt(request.getParameter("month"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/manager/meters");
            return;
        }

        try {
            new BillingService().lapHoaDonThang(year, month);
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627) {
                response.sendRedirect(request.getContextPath()
                        + "/manager/meters?year=" + year + "&month=" + month + "&trung=1");
                return;
            }
            if (e.getErrorCode() == 50001) {
                response.sendRedirect(request.getContextPath()
                        + "/manager/meters?year=" + year + "&month=" + month + "&thieu=1");
                return;
            }
            throw new ServletException("Lỗi lập hoá đơn", e);
        }

        response.sendRedirect(request.getContextPath()
                + "/manager/meters?year=" + year + "&month=" + month);
    }
}
