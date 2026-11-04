package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Invoice;
import service.InvoiceService;

/**
 * NT-39: quản lý xem hoá đơn mọi phòng (danh sách có lọc + chi tiết).
 * Chỉ đọc; lập hoá đơn vẫn ở /manager/meters. AuthFilter đã chặn vai trò khác MANAGER.
 */
public class ManagerInvoiceController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ManagerInvoiceController.class.getName());
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/manager/invoices/detail".equals(request.getServletPath())) {
                showDetail(request, response);
            } else {
                showList(request, response);
            }
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Lỗi CSDL khi tải hoá đơn phía quản lý", ex);
            throw new ServletException("Không thể tải hoá đơn. Vui lòng thử lại sau.", ex);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        String yearValue = parameter(request, "year");
        String monthValue = parameter(request, "month");
        String status = parameter(request, "status");
        String roomCode = parameter(request, "roomCode");
        String tenantName = parameter(request, "tenantName");
        String pageValue = parameter(request, "page");

        Integer year = yearValue.isEmpty() ? null : positiveInt(yearValue, "Năm");
        Integer month = monthValue.isEmpty() ? null : positiveInt(monthValue, "Tháng");
        int page = pageValue.isEmpty() ? 1 : positiveInt(pageValue, "Số trang");

        request.setAttribute("invoicePage",
                invoiceService.findPageForManager(year, month, status, roomCode, tenantName, page));
        request.setAttribute("selectedYear", year);
        request.setAttribute("selectedMonth", month);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("roomCode", roomCode);
        request.setAttribute("tenantName", tenantName);
        request.getRequestDispatcher("/WEB-INF/views/manager/invoices.jsp").forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        int invoiceId = positiveInt(parameter(request, "id"), "Mã hoá đơn");
        Invoice invoice = invoiceService.findDetailForManager(invoiceId);
        if (invoice == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("invoice", invoice);
        request.getRequestDispatcher("/WEB-INF/views/manager/invoice-detail.jsp").forward(request, response);
    }

    private int positiveInt(String value, String label) {
        if (value.matches("[0-9]{1,9}")) {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        }
        throw new IllegalArgumentException(label + " phải là số nguyên dương hợp lệ.");
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    @Override
    public String getServletInfo() {
        return "Controller cho quản lý xem hoá đơn mọi phòng (NT-39).";
    }
}
