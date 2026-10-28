package service;

import dal.RoomDAO;
import model.Room;
import java.util.ArrayList;
import java.util.List;

public class RoomService {

    // Kích thước trang cố định (theo đặc tả UC-01)
    public static final int PAGE_SIZE = 6; 
    
    private RoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    // 1. Hàm kiểm tra tính hợp lệ của bộ lọc
    public void validatePriceFilter(Integer minPrice, Integer maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("Giá tối thiểu không được là số âm.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("Giá tối đa không được là số âm.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }
    }

    // 2. Hàm đếm tổng số bản ghi
    public int countRooms(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) {
        return roomDAO.countRooms(roomTypeId, minPrice, maxPrice, floor);
    }

    // 3. Tính tổng số trang bằng toán học số nguyên
    public int calculateTotalPages(int totalRecords) {
        if (totalRecords == 0) {
            return 0;
        }
        return (totalRecords + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    // 4. Chuẩn hóa số trang (xử lý chuỗi từ URL và kẹp biên)
    public int normalizePage(String pageString, int totalPages, int totalRecords) {
        if (totalRecords == 0 || totalPages == 0) {
            return 0; // Trả về 0 và bỏ qua phân trang nếu rỗng
        }

        int page = 1;
        if (pageString != null && !pageString.trim().isEmpty()) {
            try {
                page = Integer.parseInt(pageString.trim());
            } catch (NumberFormatException e) {
                page = 1; // Nếu nhập chữ "abc", mặc định về trang 1
            }
        }

        if (page < 1) {
            page = 1; 
        } else if (page > totalPages) {
            page = totalPages; 
        }

        return page;
    }

    // 5. Hàm lấy danh sách phòng (chỉ lấy khi trang hợp lệ)
    public List<Room> getRoomsPage(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor, int currentPage) {
        if (currentPage == 0) {
            return new ArrayList<>(); // Nếu không có dữ liệu thì trả về list rỗng luôn, không gọi CSDL
        }
        return roomDAO.findRoomsPage(roomTypeId, minPrice, maxPrice, floor, currentPage, PAGE_SIZE);
    }

    // 6. Tìm phòng theo roomCode (cho trang chi tiết)
    public Room getRoomByCode(String roomCode) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return null;
        }
        return roomDAO.findByRoomCode(roomCode.trim());
    }
}