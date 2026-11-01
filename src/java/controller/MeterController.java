/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import model.Contract;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import model.MeterReading;
import java.sql.SQLException;
import service.MeterService;
import service.BillingService;

/**
 *
 * @author Lenovo
 */
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
        int year = today.getYear();
        int month = today.getMonthValue();
        Object objLoi = request.getAttribute("loi");
        List<String> loi;
        if (objLoi instanceof List) {
            loi = (List<String>) objLoi;
        } else {
            loi = new ArrayList<>();
        }
        request.setAttribute("loi", loi);
        try {
            if (yearStr != null && !yearStr.isEmpty()) {
                year = Integer.parseInt(yearStr);
            }
            if (monthStr != null && !monthStr.isEmpty()) {
                month = Integer.parseInt(monthStr);
            }
        } catch (NumberFormatException e) {
            loi.add("Năm hoặc tháng không hợp lệ.");
            year = today.getYear();
            month = today.getMonthValue();
        }
        if (month < 1 || month > 12 || year < 2020 || year > 2100) {
            loi.add("Năm hoặc tháng không hợp lệ.");
            year = today.getYear();
            month = today.getMonthValue();
        }
        List<Contract> contracts;
        try {
            contracts = new MeterService().layHopDongActive();
        } catch (SQLException e) {
            throw new ServletException("Lỗi đọc danh sách hợp đồng", e);
        }

        Map<Integer, Integer> oldElectric = new HashMap<>();
        Map<Integer, Integer> oldWater = new HashMap<>();
        Map<Integer, MeterReading> prevMap = docChiSoKyTruoc(contracts, year, month);

        for (Contract ct : contracts) {
            MeterReading prev = prevMap.get(ct.getContractId());

            if (prev != null) {
                oldElectric.put(ct.getContractId(), BillingService.chiSoCu(prev, ct.getStartElectricIndex(), true));
            } else {
                oldElectric.put(ct.getContractId(), ct.getStartElectricIndex());
            }

            if (prev != null) {
                oldWater.put(ct.getContractId(), BillingService.chiSoCu(prev, ct.getStartWaterIndex(), false));
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

    private Map<Integer, MeterReading> docChiSoKyTruoc(
            List<Contract> contracts, int year, int month) throws ServletException {

        Map<Integer, MeterReading> result;
        try {
            result = new MeterService().chiSoKyTruoc(contracts, year, month);
        } catch (SQLException e) {
            throw new ServletException("Lỗi đọc chỉ số kỳ trước", e);
        }
        return result;

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
        int year, month;
        try {
            year = Integer.parseInt(request.getParameter("year"));
            month = Integer.parseInt(request.getParameter("month"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (month < 1 || month > 12) {

            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        List<Contract> contracts;
        try {
            contracts = new MeterService().layHopDongActive();
        } catch (SQLException e) {
            throw new ServletException("Lỗi đọc danh sách hợp đồng", e);
        }
        List<String> loi = new ArrayList<>();

        // 1. Lấy giá trị chỉ số cũ (nếu có) – không cần thay đổi chữ ký do hàm đã throws ServletException
        Map<Integer, MeterReading> prevMap = docChiSoKyTruoc(contracts, year, month);

        for (Contract ct : contracts) {

            // 3. Đọc đọc chỉ số cũ (nếu có)
            MeterReading prev = prevMap.get(ct.getContractId());

            // 4. Tính giá trị “cũ” – điện
            int oldE;
            if (prev != null) {
                oldE = BillingService.chiSoCu(prev, ct.getStartElectricIndex(), true);
            } else {
                oldE = ct.getStartElectricIndex();       // giá trị bắt đầu hợp đồng
            }

            // 5. Tính giá trị “cũ” – nước
            int oldW;
            if (prev != null) {
                oldW = BillingService.chiSoCu(prev, ct.getStartWaterIndex(), false);
            } else {
                oldW = ct.getStartWaterIndex();         // giá trị bắt đầu hợp đồng
            }

            // ---------- Điện ----------
            String keyElec = "electric_" + ct.getContractId();
            String valElec = request.getParameter(keyElec);
            if (valElec == null || valElec.trim().isEmpty()) {
                loi.add("Phòng " + ct.getRoomId() + ": thiếu chỉ số điện");
            } else {
                try {
                    int newE = Integer.parseInt(valElec.trim());
                    if (newE < oldE) {
                        loi.add("Phòng " + ct.getRoomId() + ": chỉ số điện mới nhỏ hơn cũ");
                    }
                } catch (NumberFormatException e) {
                    loi.add("Phòng " + ct.getRoomId() + ": chỉ số điện không phải số");
                }
            }

            // ---------- Nước ----------
            String keyWater = "water_" + ct.getContractId();
            String valWater = request.getParameter(keyWater);
            if (valWater == null || valWater.trim().isEmpty()) {
                loi.add("Phòng " + ct.getRoomId() + ": thiếu chỉ số nước");
            } else {
                try {
                    int newW = Integer.parseInt(valWater.trim());
                    if (newW < oldW) {
                        loi.add("Phòng " + ct.getRoomId() + ": chỉ số nước mới nhỏ hơn cũ");
                    }
                } catch (NumberFormatException e) {
                    loi.add("Phòng " + ct.getRoomId() + ": chỉ số nước không phải số");
                }
            }

        }

        if (!loi.isEmpty()) {
            request.setAttribute("loi", loi);
            processRequest(request, response);
            return;
        }
        List<MeterReading> ds = new ArrayList<>();

        for (Contract ct : contracts) {
            // 2. Đọc giá trị nhập từ form
            String valElec = request.getParameter("electric_" + ct.getContractId());
            String valWater = request.getParameter("water_" + ct.getContractId());

            // 3. Parse thành số nguyên (đã được kiểm tra lỗi ở vòng lặp trước)
            int eIndex = Integer.parseInt(valElec.trim());
            int wIndex = Integer.parseInt(valWater.trim());

            // 4. Tạo MeterReading, điền dữ liệu
            MeterReading mr = new MeterReading();
            mr.setRoomId(ct.getRoomId());
            mr.setYear(year);          // năm đang xử lý
            mr.setMonth(month);        // tháng đang xử lý
            mr.setElectricIndex(eIndex);
            mr.setWaterIndex(wIndex);

            // 5. Thêm vào danh sách
            ds.add(mr);
        }

// 6. Gọi service lưu, xử lý lỗi
        try {
            new MeterService().luuChiSo(ds);
        } catch (SQLException e) {

            if (e.getErrorCode() == 2627) {
                // 2. Thêm thông báo lỗi, trả về trang nhập lại
                loi.add("Tháng này đã có chỉ số, không lưu lại");
                request.setAttribute("loi", loi);
                processRequest(request, response);   // hiển thị lại form + lỗi
                return;                              // thoát khỏi doPost/doGet
            }
            throw new ServletException("Lỗi lưu chỉ số", e);
        }

        String redirect = request.getContextPath()
                + "/manager/meters?year=" + year + "&month=" + month;
        response.sendRedirect(redirect);
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
