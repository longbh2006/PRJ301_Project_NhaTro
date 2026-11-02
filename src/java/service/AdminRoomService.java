package service;

import dal.AdminRoomDAO;
import java.util.List;
import java.util.regex.Pattern;
import model.Room;
import model.RoomType;
import model.UtilityPrice;

public class AdminRoomService {

    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    // ================= 1. NGHIỆP VỤ LOẠI PHÒNG =================

    public List<RoomType> getAllRoomTypes() {
        AdminRoomDAO dao = new AdminRoomDAO();
        return dao.findAllRoomTypes();
    }

    public RoomType getRoomTypeById(int roomTypeId) {
        AdminRoomDAO dao = new AdminRoomDAO();
        return dao.findRoomTypeById(roomTypeId);
    }

    public void saveRoomType(RoomType rt) {
        if (rt.getName() == null || rt.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên loại phòng không được để trống.");
        }
        String cleanName = rt.getName().trim();
        if (cleanName.length() > 50) {
            throw new IllegalArgumentException("Tên loại phòng không được vượt quá 50 ký tự.");
        }
        if (rt.getCapacity() < 1 || rt.getCapacity() > 8) {
            throw new IllegalArgumentException("Sức chứa phải từ 1 đến 8 người.");
        }
        rt.setName(cleanName);

        Integer excludeId = rt.getRoomTypeId() > 0 ? rt.getRoomTypeId() : null;
        
        AdminRoomDAO checkDao = new AdminRoomDAO(); // NEW DAO
        if (checkDao.checkRoomTypeNameExists(cleanName, excludeId)) {
            throw new IllegalArgumentException("Tên loại phòng '" + cleanName + "' đã tồn tại.");
        }

        AdminRoomDAO writeDao = new AdminRoomDAO(); // NEW DAO
        if (rt.getRoomTypeId() == 0) {
            writeDao.insertRoomType(rt);
        } else {
            writeDao.updateRoomType(rt);
        }
    }

    public void deleteRoomType(int roomTypeId) {
        AdminRoomDAO checkDao = new AdminRoomDAO(); // NEW DAO
        if (checkDao.hasRoomsUsingType(roomTypeId)) {
            throw new IllegalArgumentException("Không thể xóa loại phòng đang có phòng sử dụng.");
        }
        
        AdminRoomDAO deleteDao = new AdminRoomDAO(); // NEW DAO
        deleteDao.deleteRoomType(roomTypeId);
    }

    // ================= 2. NGHIỆP VỤ PHÒNG =================

    public List<Room> getAllRooms() {
        AdminRoomDAO dao = new AdminRoomDAO();
        return dao.findAllRooms();
    }

    public Room getRoomById(int roomId) {
        AdminRoomDAO dao = new AdminRoomDAO();
        return dao.findRoomById(roomId);
    }

    public void saveRoom(Room room) {
        if (room.getRoomCode() == null || room.getRoomCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phòng không được để trống.");
        }
        String cleanCode = room.getRoomCode().trim();
        if (cleanCode.length() > 10) {
            throw new IllegalArgumentException("Mã phòng không được vượt quá 10 ký tự.");
        }
        if (room.getMonthlyPrice() <= 0) {
            throw new IllegalArgumentException("Giá phòng hàng tháng phải lớn hơn 0.");
        }
        if (room.getFloor() <= 0) {
            throw new IllegalArgumentException("Số tầng phải lớn hơn 0.");
        }
        
        // Cập nhật kiểm tra diện tích
        if (room.getArea() < 0 || room.getArea() > 9999.9) {
            throw new IllegalArgumentException("Diện tích phòng không hợp lệ (0 <= x <= 9999.9).");
        }
        
        // Kiểm tra độ dài description (500 ký tự)
        if (room.getDescription() != null && room.getDescription().length() > 500) {
             throw new IllegalArgumentException("Mô tả phòng không được vượt quá 500 ký tự.");
        }
        
        // Kiểm tra độ dài và định dạng imageUrl (255 ký tự)
        if (room.getImageUrl() != null && !room.getImageUrl().trim().isEmpty()) {
             if (room.getImageUrl().length() > 255) {
                  throw new IllegalArgumentException("Link URL ảnh không được vượt quá 255 ký tự.");
             }
             if (!Pattern.compile(URL_REGEX).matcher(room.getImageUrl()).matches()) {
                  throw new IllegalArgumentException("URL ảnh không đúng định dạng (VD: http://...).");
             }
        }
        
        // Kiểm tra Status
        String status = room.getStatus();
        if (status == null || (!status.equals("ACTIVE") && !status.equals("MAINTENANCE") && !status.equals("HIDDEN"))) {
             throw new IllegalArgumentException("Trạng thái phòng không hợp lệ.");
        }
        
        room.setRoomCode(cleanCode);

        AdminRoomDAO checkTypeDao = new AdminRoomDAO(); // NEW DAO
        if (checkTypeDao.findRoomTypeById(room.getRoomTypeId()) == null) {
            throw new IllegalArgumentException("Loại phòng đã chọn không tồn tại trên hệ thống.");
        }

        Integer excludeId = room.getRoomId() > 0 ? room.getRoomId() : null;
        
        AdminRoomDAO checkCodeDao = new AdminRoomDAO(); // NEW DAO
        if (checkCodeDao.checkRoomCodeExists(cleanCode, excludeId)) {
            throw new IllegalArgumentException("Mã phòng '" + cleanCode + "' đã tồn tại.");
        }
        
        if ("HIDDEN".equals(status) && room.getRoomId() > 0) {
             AdminRoomDAO checkContractDao = new AdminRoomDAO(); // NEW DAO
             if (checkContractDao.hasActiveContracts(room.getRoomId())) {
                 throw new IllegalArgumentException("Phòng đang có người thuê. Không thể đổi trạng thái thành Ẩn (HIDDEN).");
             }
        }

        AdminRoomDAO writeDao = new AdminRoomDAO(); // NEW DAO
        if (room.getRoomId() == 0) {
            writeDao.insertRoom(room);
        } else {
            writeDao.updateRoom(room);
        }
    }

    public void deleteOrHideRoom(int roomId) {
        AdminRoomDAO checkDao = new AdminRoomDAO(); // NEW DAO
        
        // 1. Có hợp đồng ACTIVE -> Chặn đứng
        if (checkDao.hasActiveContracts(roomId)) {
             throw new IllegalArgumentException("Không thể xóa hay ẩn phòng đang có người thuê (Hợp đồng ACTIVE).");
        }
        
        AdminRoomDAO historyDao = new AdminRoomDAO(); // NEW DAO
        // 2. Có hợp đồng bất kỳ (ENDED...) -> Ẩn (HIDDEN)
        if (historyDao.hasAnyContracts(roomId)) {
            AdminRoomDAO actionDao = new AdminRoomDAO(); // NEW DAO
            actionDao.hideRoom(roomId);
        } else {
            // 3. Xóa cứng (DAO bắt mã 547 nếu vướng chỉ số điện nước/yêu cầu thuê)
            AdminRoomDAO actionDao = new AdminRoomDAO(); // NEW DAO
            actionDao.deleteRoomHard(roomId);
        }
    }

    // ================= 3. NGHIỆP VỤ ĐƠN GIÁ =================

    public List<UtilityPrice> getAllUtilityPrices() {
        AdminRoomDAO dao = new AdminRoomDAO();
        return dao.findAllUtilityPrices();
    }

    public void addUtilityPrice(UtilityPrice up) {
        if (up.getElectricPrice() == null || up.getElectricPrice().doubleValue() <= 0 ||
            up.getWaterPrice() == null || up.getWaterPrice().doubleValue() <= 0 ||
            up.getServiceFeePerPerson() == null || up.getServiceFeePerPerson().doubleValue() < 0) {
            throw new IllegalArgumentException("Đơn giá điện, nước phải lớn hơn 0 và phí dịch vụ không được âm.");
        }
        if (up.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực không được để trống.");
        }

        AdminRoomDAO checkDao = new AdminRoomDAO(); // NEW DAO
        if (checkDao.checkEffectiveDateExists(up.getEffectiveFrom())) {
            throw new IllegalArgumentException("Đã tồn tại bảng giá có cùng ngày hiệu lực " + up.getEffectiveFrom() + ".");
        }

        AdminRoomDAO writeDao = new AdminRoomDAO(); // NEW DAO
        writeDao.insertUtilityPrice(up);
    }
}