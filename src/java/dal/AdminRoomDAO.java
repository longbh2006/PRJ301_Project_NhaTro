package dal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Room;
import model.RoomType;
import model.UtilityPrice;

public class AdminRoomDAO extends DBContext {

    // ================= 1. QUẢN LÝ LOẠI PHÒNG =================
    public List<RoomType> findAllRoomTypes() {
        List<RoomType> list = new ArrayList<>();
        String sql = "SELECT roomTypeId, name, capacity FROM RoomType ORDER BY roomTypeId ASC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RoomType rt = new RoomType();
                rt.setRoomTypeId(rs.getInt("roomTypeId"));
                rt.setName(rs.getString("name"));
                rt.setCapacity(rs.getInt("capacity"));
                list.add(rt);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách loại phòng.", e);
        }
        return list;
    }

    public RoomType findRoomTypeById(int roomTypeId) {
        String sql = "SELECT roomTypeId, name, capacity FROM RoomType WHERE roomTypeId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RoomType rt = new RoomType();
                    rt.setRoomTypeId(rs.getInt("roomTypeId"));
                    rt.setName(rs.getString("name"));
                    rt.setCapacity(rs.getInt("capacity"));
                    return rt;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm loại phòng theo ID.", e);
        }
        return null;
    }

    public boolean checkRoomTypeNameExists(String name, Integer excludeRoomTypeId) {
        String sql = "SELECT 1 FROM RoomType WHERE name = ? AND (? IS NULL OR roomTypeId != ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            if (excludeRoomTypeId == null) {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(2, excludeRoomTypeId);
                ps.setInt(3, excludeRoomTypeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trùng tên loại phòng.", e);
        }
    }

    public boolean hasRoomsUsingType(int roomTypeId) {
        String sql = "SELECT 1 FROM Room WHERE roomTypeId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra phòng đang dùng loại phòng.", e);
        }
    }

    public void insertRoomType(RoomType rt) {
        String sql = "INSERT INTO RoomType (name, capacity) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rt.getName());
            ps.setInt(2, rt.getCapacity());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm loại phòng.", e);
        }
    }

    public void updateRoomType(RoomType rt) {
        String sql = "UPDATE RoomType SET name = ?, capacity = ? WHERE roomTypeId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rt.getName());
            ps.setInt(2, rt.getCapacity());
            ps.setInt(3, rt.getRoomTypeId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật loại phòng.", e);
        }
    }

    public void deleteRoomType(int roomTypeId) {
        String sql = "DELETE FROM RoomType WHERE roomTypeId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomTypeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa loại phòng.", e);
        }
    }

    // ================= 2. QUẢN LÝ PHÒNG =================
    public List<Room> findAllRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT r.roomId, r.roomCode, r.roomTypeId, rt.name AS roomTypeName, rt.capacity, r.monthlyPrice, r.floor, r.area, r.status, r.imageUrl, r.description FROM Room r JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId ORDER BY r.floor ASC, r.roomCode ASC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room room = new Room();
                room.setRoomId(rs.getInt("roomId"));
                room.setRoomCode(rs.getString("roomCode"));
                room.setRoomTypeId(rs.getInt("roomTypeId"));
                room.setRoomTypeName(rs.getString("roomTypeName"));
                room.setCapacity(rs.getInt("capacity"));
                room.setMonthlyPrice(rs.getInt("monthlyPrice"));
                room.setFloor(rs.getInt("floor"));
                room.setArea(rs.getDouble("area"));
                room.setStatus(rs.getString("status"));
                room.setImageUrl(rs.getString("imageUrl"));
                room.setDescription(rs.getString("description"));
                list.add(room);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách tất cả phòng.", e);
        }
        return list;
    }

    public Room findRoomById(int roomId) {
        String sql = "SELECT r.roomId, r.roomCode, r.roomTypeId, rt.name AS roomTypeName, rt.capacity, r.monthlyPrice, r.floor, r.area, r.status, r.imageUrl, r.description FROM Room r JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId WHERE r.roomId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getInt("roomId"));
                    room.setRoomCode(rs.getString("roomCode"));
                    room.setRoomTypeId(rs.getInt("roomTypeId"));
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
            throw new RuntimeException("Lỗi tìm phòng theo ID.", e);
        }
        return null;
    }

    public boolean checkRoomCodeExists(String roomCode, Integer excludeRoomId) {
        String sql = "SELECT 1 FROM Room WHERE roomCode = ? AND (? IS NULL OR roomId != ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomCode);
            if (excludeRoomId == null) {
                ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(2, excludeRoomId);
                ps.setInt(3, excludeRoomId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra mã phòng trùng.", e);
        }
    }

    public void insertRoom(Room room) {
        String sql = "INSERT INTO Room (roomCode, roomTypeId, monthlyPrice, floor, area, status, description, imageUrl) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomCode());
            ps.setInt(2, room.getRoomTypeId());
            ps.setInt(3, room.getMonthlyPrice());
            ps.setInt(4, room.getFloor());
            if (room.getArea() > 0) {
                ps.setDouble(5, room.getArea());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }
            ps.setString(6, room.getStatus() != null ? room.getStatus() : "ACTIVE");
            ps.setString(7, room.getDescription());
            ps.setString(8, room.getImageUrl());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm phòng mới.", e);
        }
    }

    public void updateRoom(Room room) {
        String sql = "UPDATE Room SET roomCode = ?, roomTypeId = ?, monthlyPrice = ?, floor = ?, area = ?, status = ?, description = ?, imageUrl = ? WHERE roomId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomCode());
            ps.setInt(2, room.getRoomTypeId());
            ps.setInt(3, room.getMonthlyPrice());
            ps.setInt(4, room.getFloor());
            if (room.getArea() > 0) {
                ps.setDouble(5, room.getArea());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }
            ps.setString(6, room.getStatus());
            ps.setString(7, room.getDescription());
            ps.setString(8, room.getImageUrl());
            ps.setInt(9, room.getRoomId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật thông tin phòng.", e);
        }
    }

    // Kiểm tra có ĐANG có hợp đồng ACTIVE hay không
    public boolean hasActiveContracts(int roomId) {
        String sql = "SELECT 1 FROM Contract WHERE roomId = ? AND status = 'ACTIVE'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra hợp đồng ACTIVE.", e);
        }
    }

    // Kiểm tra có BẤT KỲ hợp đồng nào hay không (Lịch sử)
    public boolean hasAnyContracts(int roomId) {
        String sql = "SELECT 1 FROM Contract WHERE roomId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra lịch sử hợp đồng.", e);
        }
    }

    public void hideRoom(int roomId) {
        String sql = "UPDATE Room SET status = 'HIDDEN' WHERE roomId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi chuyển trạng thái ẩn phòng.", e);
        }
    }

    public void deleteRoomHard(int roomId) {
        String sql = "DELETE FROM Room WHERE roomId = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) {
                throw new IllegalArgumentException("Không thể xóa phòng do còn vướng khóa ngoại (Yêu cầu thuê, chỉ số điện/nước).");
            }
            throw new RuntimeException("Lỗi xóa cứng phòng.", e);
        }
    }

    // ================= 3. ĐƠN GIÁ ĐIỆN NƯỚC =================
    public List<UtilityPrice> findAllUtilityPrices() {
        List<UtilityPrice> list = new ArrayList<>();
        String sql = "SELECT priceId, electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom FROM UtilityPrice ORDER BY effectiveFrom DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UtilityPrice up = new UtilityPrice();
                up.setPriceId(rs.getInt("priceId"));
                up.setElectricPrice(rs.getBigDecimal("electricPrice"));
                up.setWaterPrice(rs.getBigDecimal("waterPrice"));
                up.setServiceFeePerPerson(rs.getBigDecimal("serviceFeePerPerson"));
                Date dbDate = rs.getDate("effectiveFrom");
                if (dbDate != null) {
                    up.setEffectiveFrom(dbDate.toLocalDate());
                }
                list.add(up);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy lịch sử đơn giá điện nước.", e);
        }
        return list;
    }

    public boolean checkEffectiveDateExists(LocalDate effectiveFrom) {
        String sql = "SELECT 1 FROM UtilityPrice WHERE effectiveFrom = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(effectiveFrom));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra ngày hiệu lực.", e);
        }
    }

    public void insertUtilityPrice(UtilityPrice up) {
        String sql = "INSERT INTO UtilityPrice (electricPrice, waterPrice, serviceFeePerPerson, effectiveFrom) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, up.getElectricPrice());
            ps.setBigDecimal(2, up.getWaterPrice());
            ps.setBigDecimal(3, up.getServiceFeePerPerson());
            ps.setDate(4, Date.valueOf(up.getEffectiveFrom()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm bảng giá điện nước mới.", e);
        }
    }
}
