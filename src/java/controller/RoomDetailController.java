package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import model.RentalRoom;
import service.RentalService;

public class RoomDetailController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int roomId = Integer.parseInt(request.getParameter("roomId"));
            if (roomId <= 0) {
                throw new NumberFormatException();
            }
            RentalRoom room = new RentalService().findRoom(roomId);
            if (room == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("room", room);
            request.getRequestDispatcher("/WEB-INF/views/rooms/detail.jsp").forward(request, response);
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phòng không hợp lệ.");
        } catch (SQLException ex) {
            throw new ServletException("Không thể tải thông tin phòng.", ex);
        }
    }
}
