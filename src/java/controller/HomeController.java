package controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Room;
import service.RoomService;

// SỬA: Xóa @WebServlet
public class HomeController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            RoomService roomService = new RoomService();
            List<Room> rooms = roomService.getAvailableRoomsForHome();
            request.setAttribute("rooms", rooms);
        } catch (RuntimeException e) {
            // SỬA: Sử dụng LOGGER thay vì printStackTrace
            LOGGER.log(Level.SEVERE, "Lỗi Database ở Trang Chủ", e);
            request.setAttribute("errorMessage", "Hệ thống đang gặp sự cố gián đoạn kết nối. Vui lòng thử lại sau.");
        }
        
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Trang chủ không hỗ trợ phương thức POST.");
    }

    @Override
    public String getServletInfo() {
        return "Controller xử lý dữ liệu và điều hướng cho Trang chủ (hiển thị danh sách phòng trống tiêu biểu).";
    }
}