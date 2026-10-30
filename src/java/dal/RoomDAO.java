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
        // SỬA: Chuỗi SQL nguyên khối, không nối ghép
        String sql = "SELECT COUNT(*) FROM Room r WHERE r.status = 'ACTIVE' AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') AND (? IS NULL OR r.roomTypeId = ?) AND (? IS NULL OR r.monthlyPrice >= ?) AND (? IS NULL OR r.monthlyPrice <= ?) AND (? IS NULL OR r.floor = ?)";
        
        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setWhereParameters(ps, roomTypeId, minPrice, maxPrice, floor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi đếm số phòng.", e);
        }
        return 0;
    }

    public List<Room> findRoomsPage(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor, int page, int pageSize) {
        List<Room> list = new ArrayList<>();
        // SỬA: Chuỗi SQL nguyên khối
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, r.monthlyPrice, r.floor, r.imageUrl FROM Room r JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId WHERE r.status = 'ACTIVE' AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') AND (? IS NULL OR r.roomTypeId = ?) AND (? IS NULL OR r.monthlyPrice >= ?) AND (? IS NULL OR r.monthlyPrice <= ?) AND (? IS NULL OR r.floor = ?) ORDER BY r.floor ASC, r.roomCode ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

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
        // SỬA: Chuỗi SQL nguyên khối
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, rt.capacity, r.monthlyPrice, r.floor, r.area, r.status, r.imageUrl, r.description FROM Room r JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId WHERE r.roomCode = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public List<Room> getAvailableRoomsForHome() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, r.monthlyPrice, r.floor, r.imageUrl FROM Room r JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId WHERE r.status = 'ACTIVE' AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') ORDER BY r.floor ASC, r.roomCode ASC"; 
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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