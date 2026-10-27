/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import model.Room;
import service.RoomService;

/**
 *
 * @author ADMIN
 */
public class RoomController extends HttpServlet {

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
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet RoomController</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet RoomController at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
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
        try {
            // 1. Lấy param từ request (do người dùng truyền qua URL)
            String pageString = request.getParameter("page");
            String minPriceStr = request.getParameter("minPrice");
            String maxPriceStr = request.getParameter("maxPrice");
            String roomTypeStr = request.getParameter("roomTypeId");
            String floorStr = request.getParameter("floor");

            // Ép kiểu chuỗi sang Integer một cách an toàn (trả về null nếu không nhập)
            Integer minPrice = (minPriceStr != null && !minPriceStr.isEmpty()) ? Integer.valueOf(minPriceStr) : null;
            Integer maxPrice = (maxPriceStr != null && !maxPriceStr.isEmpty()) ? Integer.valueOf(maxPriceStr) : null;
            Integer roomTypeId = (roomTypeStr != null && !roomTypeStr.isEmpty()) ? Integer.valueOf(roomTypeStr) : null;
            Integer floor = (floorStr != null && !floorStr.isEmpty()) ? Integer.valueOf(floorStr) : null;

            RoomService rs = new RoomService();

            // 2. Validate khoảng giá
            rs.validatePriceFilter(minPrice, maxPrice);

            // 3. Tính toán phân trang dựa trên bộ lọc
            int totalRecords = rs.countRooms(roomTypeId, minPrice, maxPrice, floor);
            int totalPages = rs.calculateTotalPages(totalRecords);
            // Điền pageString lấy từ tham số URL vào đây
            int currentPage = rs.normalizePage(pageString, totalPages, totalRecords);

            // 4. Lấy danh sách phòng theo đúng tham số lọc và trang hiện tại
            List<Room> rooms = rs.getRoomsPage(roomTypeId, minPrice, maxPrice, floor, currentPage);

            // 5. Set attributes truyền sang file .jsp
            request.setAttribute("rooms", rooms);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);

        } catch (NumberFormatException e) {
            // Lỗi này xảy ra khi người dùng cố tình nhập chữ vào ô bộ lọc giá/tầng trên URL
            request.setAttribute("errorMessage", "Dữ liệu lọc không hợp lệ. Vui lòng chỉ nhập số.");
        } catch (IllegalArgumentException e) {
            // Lỗi logic từ Service (ví dụ: min > max, hoặc giá âm)
            request.setAttribute("errorMessage", e.getMessage());
        }

        // Chuyển tiếp (forward) sang file JSP
        request.getRequestDispatcher("/WEB-INF/views/rooms/index.jsp").forward(request, response);
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
