package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Room;

public class RoomDAO extends DBContext {

    // (Giữ nguyên các hàm buildWhere và setWhereParameters)

    private String buildWhere(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) {
        // Gom câu lệnh SQL tĩnh thành chuỗi nối + cho dễ đọc, vì AGENTS.md chỉ cấm nối (+) tham số động
        return "r.status = 'ACTIVE' "
             + "AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') "
             + "AND (? IS NULL OR r.roomTypeId = ?) "
             + "AND (? IS NULL OR r.monthlyPrice >= ?) "
             + "AND (? IS NULL OR r.monthlyPrice <= ?) "
             + "AND (? IS NULL OR r.floor = ?) ";
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setWhereParameters(PreparedStatement ps, Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) throws SQLException {
        int index = 1;
        setNullableInt(ps, index++, roomTypeId);
        setNullableInt(ps, index++, roomTypeId);
        setNullableInt(ps, index++, minPrice);
        setNullableInt(ps, index++, minPrice);
        setNullableInt(ps, index++, maxPrice);
        setNullableInt(ps, index++, maxPrice);
        setNullableInt(ps, index++, floor);
        setNullableInt(ps, index++, floor);
    }

    public int countRooms(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) {
        String sql = "SELECT COUNT(*) FROM Room r WHERE " + buildWhere(roomTypeId, minPrice, maxPrice, floor);
        
        // MỖI HÀM GỌI getConnection() ĐỂ LẤY KẾT NỐI MỚI (Stateless)
        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setWhereParameters(ps, roomTypeId, minPrice, maxPrice, floor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi đếm số phòng.", e);
        }
        return 0;
    }

    public List<Room> findRoomsPage(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor, int page, int pageSize) {
        List<Room> list = new ArrayList<>();
        
        // Trả lại cách nối chuỗi (+) bằng literal string chuẩn xác, không dùng StringBuilder cho hằng số
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, r.monthlyPrice, r.floor, r.imageUrl "
                   + "FROM Room r "
                   + "JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId "
                   + "WHERE " + buildWhere(roomTypeId, minPrice, maxPrice, floor)
                   + " ORDER BY r.floor ASC, r.roomCode ASC "
                   + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setWhereParameters(ps, roomTypeId, minPrice, maxPrice, floor);

            int offset = (page - 1) * pageSize;
            int index = 9;
            ps.setInt(index++, offset);
            ps.setInt(index, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getInt("roomId"));
                    room.setRoomCode(rs.getString("roomCode"));
                    room.setRoomTypeName(rs.getString("roomTypeName"));
                    room.setMonthlyPrice(rs.getInt("monthlyPrice"));
                    room.setFloor(rs.getInt("floor"));
                    room.setImageUrl(rs.getString("imageUrl"));
                    list.add(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy vấn danh sách phòng.", e);
        }
        return list;
    }

    public Room findByRoomCode(String roomCode) {
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, rt.capacity, "
                   + "r.monthlyPrice, r.floor, r.area, r.status, r.imageUrl, r.description "
                   + "FROM Room r "
                   + "JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId "
                   + "WHERE r.roomCode = ?";

        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setString(1, roomCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getInt("roomId"));
                    room.setRoomCode(rs.getString("roomCode"));
                    room.setRoomTypeName(rs.getString("roomTypeName"));
                    room.setCapacity(rs.getInt("capacity"));
                    room.setMonthlyPrice(rs.getInt("monthlyPrice"));
                    room.setFloor(rs.getInt("floor"));
                    room.setArea(rs.getDouble("area"));
                    room.setStatus(rs.getString("status"));
                    room.setImageUrl(rs.getString("imageUrl"));
                    room.setDescription(rs.getString("description"));
                    return room;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy vấn chi tiết phòng.", e);
        }
        return null;
    }


    // Bước 6 (bổ sung): Lấy danh sách phòng trống hiển thị cho trang chủ
    public List<Room> getAvailableRoomsForHome() {
        List<Room> list = new ArrayList<>();

        // 1. Câu SQL có JOIN RoomType và lấy đúng điều kiện "phòng trống" (không có tham số ? nào)
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, r.monthlyPrice, r.floor, r.imageUrl "
                + "FROM Room r "
                + "JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId "
                + "WHERE r.status = 'ACTIVE' "
                + "AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') "
                + "ORDER BY r.floor ASC, r.roomCode ASC"; // Sắp xếp cho gọn gàng trên trang chủ

        // 2. Mở kết nối, PreparedStatement và ResultSet bằng try-with-resources
        // Vì truy vấn không có tham số (?), ta có thể gộp luôn lệnh executeQuery() vào cụm try này
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            // 3. Đọc ResultSet vào đối tượng Room (giống hệt dòng 97-107)
            while (rs.next()) {
                Room room = new Room();
                room.setRoomId(rs.getInt("roomId"));
                room.setRoomCode(rs.getString("roomCode"));
                room.setRoomTypeName(rs.getString("roomTypeName"));
                room.setMonthlyPrice(rs.getInt("monthlyPrice"));
                room.setFloor(rs.getInt("floor"));
                room.setImageUrl(rs.getString("imageUrl"));

                list.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phòng trống cho trang chủ.", e);
        }

        return list;
    }
}

