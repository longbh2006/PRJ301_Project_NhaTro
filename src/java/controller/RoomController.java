package controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import model.Room;
import service.RoomService;

/**
 * Controller xử lý danh sách phòng (/rooms) và chi tiết phòng (/rooms/detail).
 *
 * @author ADMIN
 */
public class RoomController extends HttpServlet {

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

        String servletPath = request.getServletPath();

        if ("/rooms/detail".equals(servletPath)) {
            doGetDetail(request, response);
        } else {
            doGetList(request, response);
        }
    }

    /**
     * GET /rooms — Danh sách phòng trống có phân trang và bộ lọc.
     */
    private void doGetList(HttpServletRequest request, HttpServletResponse response)
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

            // 6. Giữ giá trị bộ lọc để hiển thị lại trên form và dùng trong phân trang
            request.setAttribute("filterRoomTypeId", roomTypeId);
            request.setAttribute("filterMinPrice", minPrice);
            request.setAttribute("filterMaxPrice", maxPrice);
            request.setAttribute("filterFloor", floor);

        } catch (NumberFormatException e) {
            // Lỗi này xảy ra khi người dùng cố tình nhập chữ vào ô bộ lọc giá/tầng trên URL
            request.setAttribute("errorMessage", "Dữ liệu lọc không hợp lệ. Vui lòng chỉ nhập số.");
        } catch (IllegalArgumentException e) {
            // Lỗi logic từ Service (ví dụ: min > max, hoặc giá âm)
            request.setAttribute("errorMessage", e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi CSDL bọc trong RuntimeException từ DAO
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi truy vấn dữ liệu. Vui lòng thử lại sau.");
        }

        // Chuyển tiếp (forward) sang file JSP
        request.getRequestDispatcher("/WEB-INF/views/rooms/index.jsp").forward(request, response);
    }

    /**
     * GET /rooms/detail?code=P101 — Chi tiết một phòng.
     */
    private void doGetDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String roomCode = request.getParameter("code");

        try {
            RoomService rs = new RoomService();
            Room room = rs.getRoomByCode(roomCode);

            if (room == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            request.setAttribute("room", room);
        } catch (RuntimeException e) {
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi truy vấn dữ liệu. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/rooms/detail.jsp").forward(request, response);
    }

    /**
     * POST không dùng ở trang phòng công khai — trả 405 Method Not Allowed.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Controller quản lý danh sách và chi tiết phòng trọ.";
    }// </editor-fold>

}
