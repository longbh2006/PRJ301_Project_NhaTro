package controller;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Room;
import service.RoomService;

public class HomeController extends HttpServlet {

    /** 
     * Handles the HTTP GET method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            // 1. Khởi tạo Service
            RoomService roomService = new RoomService();
            
            // 2. Gọi hàm lấy danh sách phòng trống cho trang chủ
            List<Room> rooms = roomService.getAvailableRoomsForHome();
            
            // 3. Đặt danh sách vào request attribute (tên là "rooms" để đồng bộ với /rooms)
            request.setAttribute("rooms", rooms);
            
        } catch (RuntimeException e) {
            // Bắt lỗi CSDL từ DAO ném lên để tránh hiển thị trang trắng/lỗi stack trace
            request.setAttribute("errorMessage", "Hệ thống đang gặp sự cố gián đoạn kết nối. Vui lòng thử lại sau.");
            // Ghi log lỗi hệ thống (có thể thay bằng logger thực tế của dự án)
            e.printStackTrace();
        }
        
        // 4. Chuyển tiếp (forward) sang file JSP
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    } 

    /** 
     * Handles the HTTP POST method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Trang chủ không có form gửi lên, trả về lỗi 405 Method Not Allowed
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Trang chủ không hỗ trợ phương thức POST.");
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Controller xử lý dữ liệu và điều hướng cho Trang chủ (hiển thị danh sách phòng trống tiêu biểu).";
    }
}