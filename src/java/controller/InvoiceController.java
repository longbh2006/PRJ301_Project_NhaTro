package controller;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BillingService;

@WebServlet(name = "InvoiceController", urlPatterns = {"/manager/invoices"})
public class InvoiceController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int year = Integer.parseInt(request.getParameter("year"));
        int month = Integer.parseInt(request.getParameter("month"));
               try {
               new BillingService().lapHoaDonThang(year, month);
           } catch (SQLException e) {
               if (e.getErrorCode() == 2627) {
                   response.sendRedirect(request.getContextPath()
                           + "/manager/meters?year=" + year + "&month=" + month + "&trung=1");
                   return;
               }
               throw new ServletException("Lỗi lập hoá đơn", e);
           }
               response.sendRedirect(request.getContextPath()
                   + "/manager/meters?year=" + year + "&month=" + month);
    }
}
