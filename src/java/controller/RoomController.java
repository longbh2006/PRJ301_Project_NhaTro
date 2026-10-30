package controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Room;
import service.RoomService;

/**
 * Controller xử lý danh sách phòng (/rooms) và chi tiết phòng (/rooms/detail).
 *
 * @author ADMIN
 */
@WebServlet(name = "RoomController", urlPatterns = {"/rooms", "/rooms/detail"})
public class RoomController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RoomController.class.getName());

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

   private void doGetList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String pageString = request.getParameter("page");
            String minPriceStr = request.getParameter("minPrice");
            String maxPriceStr = request.getParameter("maxPrice");
            // SỬA: Hứng tham số type thay vì roomTypeId
            String typeStr = request.getParameter("type");
            String floorStr = request.getParameter("floor");

            Integer minPrice = (minPriceStr != null && !minPriceStr.isEmpty()) ? Integer.valueOf(minPriceStr) : null;
            Integer maxPrice = (maxPriceStr != null && !maxPriceStr.isEmpty()) ? Integer.valueOf(maxPriceStr) : null;
            Integer roomTypeId = (typeStr != null && !typeStr.isEmpty()) ? Integer.valueOf(typeStr) : null;
            Integer floor = (floorStr != null && !floorStr.isEmpty()) ? Integer.valueOf(floorStr) : null;

            RoomService rs = new RoomService();
            rs.validatePriceFilter(minPrice, maxPrice);

            int totalRecords = rs.countRooms(roomTypeId, minPrice, maxPrice, floor);
            int totalPages = rs.calculateTotalPages(totalRecords);
            int currentPage = rs.normalizePage(pageString, totalPages, totalRecords);

            List<Room> rooms = rs.getRoomsPage(roomTypeId, minPrice, maxPrice, floor, currentPage);

            request.setAttribute("rooms", rooms);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);

            // SỬA: Đổi tên attribute cho khớp với view
            request.setAttribute("filterType", roomTypeId);
            request.setAttribute("filterMinPrice", minPrice);
            request.setAttribute("filterMaxPrice", maxPrice);
            request.setAttribute("filterFloor", floor);
            
            // TODO: (Tùy chọn) Gọi DAO lấy danh sách tất cả RoomType truyền sang với tên "roomTypes" để bộ lọc không bị gõ cứng.

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Dữ liệu lọc không hợp lệ. Vui lòng chỉ nhập số.");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Lỗi Database khi truy vấn danh sách phòng", e);
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi truy vấn dữ liệu. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/rooms/index.jsp").forward(request, response);
    }
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
            // ĐÃ SỬA: Ghi log chuẩn bằng java.util.logging.Logger
            LOGGER.log(Level.SEVERE, "Lỗi Database khi truy vấn chi tiết phòng: " + roomCode, e);
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi truy vấn dữ liệu. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/rooms/detail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public String getServletInfo() {
        return "Controller quản lý danh sách và chi tiết phòng trọ.";
    }
}