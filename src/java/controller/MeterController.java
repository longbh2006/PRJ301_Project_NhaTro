/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import dal.BillingDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import model.Contract;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import model.MeterReading;
import java.sql.SQLException;

/**
 *
 * @author Lenovo
 */
@WebServlet(name = "MeterController", urlPatterns = {"/manager/meters"})
public class MeterController extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String yearStr = request.getParameter("year");
        String monthStr = request.getParameter("month");
        LocalDate today = LocalDate.now();
        int year = (yearStr == null || yearStr.isEmpty())
                ? today.getYear()
                : Integer.parseInt(yearStr);
        int month = (monthStr == null || monthStr.isEmpty())
                ? today.getMonthValue()
                : Integer.parseInt(monthStr);
        List<Contract> contracts = new BillingDAO().findActiveContracts();
        Map<Integer, Integer> oldElectric = new HashMap<>();
        Map<Integer, Integer> oldWater = new HashMap<>();
        BillingDAO dao = new BillingDAO();   // 1. Khởi tạo một lần duy nhất

        for (Contract ct : contracts) {
            // 2. Lấy chỉ số điện kỳ trước
            MeterReading prev = null;

            try {
                prev = dao.findReadingBefore(ct.getRoomId(), year, month);
            } catch (SQLException e) {
                throw new ServletException("Lỗi đọc chỉ số", e);
            }

            // 3. Ghi vào map
            if (prev != null) {
                oldElectric.put(ct.getContractId(), prev.getElectricIndex());
            } else {
                oldElectric.put(ct.getContractId(), ct.getStartElectricIndex());
            }

            

            // 2. Lưu chỉ số nước (đừng gọi findReadingBefore lại!)
            if (prev != null) {
                oldWater.put(ct.getContractId(), prev.getWaterIndex());   // tránh NPE
            } else {
                oldWater.put(ct.getContractId(), ct.getStartWaterIndex());
            }
        }
        request.setAttribute("contracts", contracts);
        request.setAttribute("year", year);
        request.setAttribute("month", month);
        request.setAttribute("oldElectric", oldElectric);
        request.setAttribute("oldWater", oldWater);
        request.getRequestDispatcher("/WEB-INF/views/manager/meters.jsp")
                .forward(request, response);



    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
