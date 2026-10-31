package controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Room;
import model.RoomType;
import model.UtilityPrice;
import service.AdminRoomService;

public class AdminRoomController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminRoomController.class.getName());

    private int parseIntRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là một số nguyên hợp lệ.");
        }
    }

    // SỬA: Chỉ chấp nhận số nguyên để khớp với DECIMAL(12,0)
    private BigDecimal parseBigDecimalIntegerRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        try {
            BigDecimal bd = new BigDecimal(value.trim());
            if (bd.scale() > 0 && bd.stripTrailingZeros().scale() > 0) {
                 throw new IllegalArgumentException(fieldName + " không được là số thập phân.");
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là một số hợp lệ (không chứa ký tự).");
        }
    }

    private double parseDoubleOptional(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            double d = Double.parseDouble(value.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                 throw new IllegalArgumentException("Diện tích không phải là con số hợp lệ.");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Diện tích phải là một con số hợp lệ.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        AdminRoomService service = new AdminRoomService();

        try {
            if ("/admin/room-types".equals(path)) {
                handleGetRoomTypes(request, response, service);
            } else if ("/admin/prices".equals(path)) {
                handleGetPrices(request, response, service);
            } else {
                handleGetRooms(request, response, service);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải dữ liệu trang Admin: " + path, e);
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        AdminRoomService service = new AdminRoomService();

        // SỬA: Bọc toàn bộ xử lý POST vào khối try/catch
        try {
            if ("/admin/room-types".equals(path)) {
                handlePostRoomTypes(request, response, service);
            } else if ("/admin/prices".equals(path)) {
                handlePostPrices(request, response, service);
            } else {
                handlePostRooms(request, response, service);
            }
        } catch (RuntimeException e) {
             LOGGER.log(Level.SEVERE, "Lỗi khi lưu dữ liệu Admin: " + path, e);
             request.setAttribute("errorMessage", "Sự cố hệ thống. Vui lòng thử lại sau.");
             // Forward về trang form tùy theo đường dẫn
             if ("/admin/room-types".equals(path)) {
                  request.getRequestDispatcher("/WEB-INF/views/admin/room-types/form.jsp").forward(request, response);
             } else if ("/admin/prices".equals(path)) {
                  request.getRequestDispatcher("/WEB-INF/views/admin/prices/form.jsp").forward(request, response);
             } else {
                  request.getRequestDispatcher("/WEB-INF/views/admin/rooms/form.jsp").forward(request, response);
             }
        }
    }

    // ================= QUẢN LÝ LOẠI PHÒNG =================
    private void handleGetRoomTypes(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/admin/room-types/form.jsp").forward(request, response);
        } else if ("edit".equals(action)) {
            try {
                int id = parseIntRequired(request.getParameter("id"), "ID loại phòng");
                RoomType rt = service.getRoomTypeById(id);
                if (rt == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("roomType", rt);
                request.getRequestDispatcher("/WEB-INF/views/admin/room-types/form.jsp").forward(request, response);
            } catch (IllegalArgumentException ex) {
                 // SỬA: Nếu truyền id=abc thì trả về 404, không ném ra 500
                 response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            request.setAttribute("roomTypes", service.getAllRoomTypes());
            request.getRequestDispatcher("/WEB-INF/views/admin/room-types/list.jsp").forward(request, response);
        }
    }

    private void handlePostRoomTypes(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            try {
                int id = parseIntRequired(request.getParameter("roomTypeId"), "ID loại phòng");
                service.deleteRoomType(id);
                response.sendRedirect(request.getContextPath() + "/admin/room-types?msg=deleted");
            } catch (IllegalArgumentException e) {
                request.setAttribute("errorMessage", e.getMessage());
                request.setAttribute("roomTypes", service.getAllRoomTypes());
                request.getRequestDispatcher("/WEB-INF/views/admin/room-types/list.jsp").forward(request, response);
            }
            return;
        }

        RoomType rt = new RoomType();
        String idStr = request.getParameter("roomTypeId");
        String name = request.getParameter("name");
        String capacityStr = request.getParameter("capacity");

        try {
            if (idStr != null && !idStr.trim().isEmpty()) {
                rt.setRoomTypeId(parseIntRequired(idStr, "ID loại phòng"));
            }
            rt.setName(name);
            rt.setCapacity(parseIntRequired(capacityStr, "Sức chứa"));
            service.saveRoomType(rt);
            response.sendRedirect(request.getContextPath() + "/admin/room-types?msg=saved");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("inputName", name);
            request.setAttribute("inputCapacity", capacityStr);
            request.setAttribute("roomType", rt);
            request.getRequestDispatcher("/WEB-INF/views/admin/room-types/form.jsp").forward(request, response);
        }
    }

    // ================= QUẢN LÝ PHÒNG =================
    private void handleGetRooms(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            request.setAttribute("roomTypes", service.getAllRoomTypes());
            request.getRequestDispatcher("/WEB-INF/views/admin/rooms/form.jsp").forward(request, response);
        } else if ("edit".equals(action)) {
            try {
                int id = parseIntRequired(request.getParameter("id"), "ID phòng");
                Room room = service.getRoomById(id);
                if (room == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("room", room);
                request.setAttribute("roomTypes", service.getAllRoomTypes());
                request.getRequestDispatcher("/WEB-INF/views/admin/rooms/form.jsp").forward(request, response);
            } catch (IllegalArgumentException ex) {
                 response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            request.setAttribute("rooms", service.getAllRooms());
            request.getRequestDispatcher("/WEB-INF/views/admin/rooms/list.jsp").forward(request, response);
        }
    }

    private void handlePostRooms(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            try {
                int roomId = parseIntRequired(request.getParameter("roomId"), "ID phòng");
                service.deleteOrHideRoom(roomId);
                response.sendRedirect(request.getContextPath() + "/admin/rooms?msg=deleted");
            } catch (IllegalArgumentException e) {
                request.setAttribute("errorMessage", e.getMessage());
                request.setAttribute("rooms", service.getAllRooms());
                request.getRequestDispatcher("/WEB-INF/views/admin/rooms/list.jsp").forward(request, response);
            }
            return;
        }

        Room room = new Room();
        String idStr = request.getParameter("roomId");
        String roomCode = request.getParameter("roomCode");
        String typeStr = request.getParameter("roomTypeId");
        String priceStr = request.getParameter("monthlyPrice");
        String floorStr = request.getParameter("floor");
        String areaStr = request.getParameter("area");
        String status = request.getParameter("status");

        try {
            if (idStr != null && !idStr.trim().isEmpty()) {
                room.setRoomId(parseIntRequired(idStr, "ID phòng"));
            }
            room.setRoomCode(roomCode);
            room.setRoomTypeId(parseIntRequired(typeStr, "Loại phòng"));
            room.setMonthlyPrice(parseIntRequired(priceStr, "Giá thuê"));
            room.setFloor(parseIntRequired(floorStr, "Tầng"));
            room.setArea(parseDoubleOptional(areaStr));
            room.setStatus(status);
            room.setDescription(request.getParameter("description"));
            room.setImageUrl(request.getParameter("imageUrl"));

            service.saveRoom(room);
            response.sendRedirect(request.getContextPath() + "/admin/rooms?msg=saved");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("room", room);
            request.setAttribute("inputArea", areaStr); 
            request.setAttribute("roomTypes", service.getAllRoomTypes());
            request.getRequestDispatcher("/WEB-INF/views/admin/rooms/form.jsp").forward(request, response);
        }
    }

    // ================= QUẢN LÝ BẢNG GIÁ =================
    private void handleGetPrices(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/admin/prices/form.jsp").forward(request, response);
        } else {
            request.setAttribute("prices", service.getAllUtilityPrices());
            request.setAttribute("today", LocalDate.now().toString()); // Để jsp xử lý logic nhãn "Đang áp dụng"
            request.getRequestDispatcher("/WEB-INF/views/admin/prices/list.jsp").forward(request, response);
        }
    }

    private void handlePostPrices(HttpServletRequest request, HttpServletResponse response, AdminRoomService service) throws ServletException, IOException {
        UtilityPrice up = new UtilityPrice();
        String elecStr = request.getParameter("electricPrice");
        String waterStr = request.getParameter("waterPrice");
        String serviceStr = request.getParameter("serviceFeePerPerson");
        String dateStr = request.getParameter("effectiveFrom");

        try {
            up.setElectricPrice(parseBigDecimalIntegerRequired(elecStr, "Giá điện"));
            up.setWaterPrice(parseBigDecimalIntegerRequired(waterStr, "Giá nước"));
            up.setServiceFeePerPerson(parseBigDecimalIntegerRequired(serviceStr, "Phí dịch vụ"));
            
            if (dateStr == null || dateStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Ngày hiệu lực không được để trống.");
            }
            try {
                up.setEffectiveFrom(LocalDate.parse(dateStr.trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Ngày hiệu lực sai định dạng (YYYY-MM-DD).");
            }

            service.addUtilityPrice(up);
            response.sendRedirect(request.getContextPath() + "/admin/prices?msg=saved");
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("inputPrice", up);
            request.setAttribute("inputDateStr", dateStr);
            request.getRequestDispatcher("/WEB-INF/views/admin/prices/form.jsp").forward(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Controller quản trị Phòng, Loại phòng và Đơn giá điện nước.";
    }
}