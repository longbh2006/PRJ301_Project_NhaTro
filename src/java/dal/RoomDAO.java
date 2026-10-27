package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types; // Đã bổ sung thư viện Types
import java.util.ArrayList;
import java.util.List;
import model.Room;

public class RoomDAO extends DBContext {

    // Bước 1 & Bước 2: Khai báo và viết hàm buildWhere tạo chuỗi điều kiện
    private String buildWhere(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) {
        StringBuilder sql = new StringBuilder();
        sql.append("r.status = 'ACTIVE' ");
        sql.append("AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.roomId = r.roomId AND c.status = 'ACTIVE') ");

        sql.append("AND (? IS NULL OR r.roomTypeId = ?) ");
        sql.append("AND (? IS NULL OR r.monthlyPrice >= ?) ");
        sql.append("AND (? IS NULL OR r.monthlyPrice <= ?) ");
        sql.append("AND (? IS NULL OR r.floor = ?) ");

        return sql.toString();
    }

    // Hàm nhỏ: Xử lý gán giá trị an toàn (khắc phục lỗi ép kiểu NULL của SQL Server JDBC)
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    // Hàm hỗ trợ để gán tham số lặp lại cho các dấu ? trong buildWhere
    private void setWhereParameters(PreparedStatement ps, Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) throws SQLException {
        int index = 1;
        // Sử dụng hàm setNullableInt để truyền tham số
        setNullableInt(ps, index++, roomTypeId);
        setNullableInt(ps, index++, roomTypeId);

        setNullableInt(ps, index++, minPrice);
        setNullableInt(ps, index++, minPrice);

        setNullableInt(ps, index++, maxPrice);
        setNullableInt(ps, index++, maxPrice);

        setNullableInt(ps, index++, floor);
        setNullableInt(ps, index++, floor);
    }

    // Bước 3: Viết hàm countRooms đếm tổng số phòng thỏa mãn
    public int countRooms(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor) {
        String sql = "SELECT COUNT(*) FROM Room r WHERE " + buildWhere(roomTypeId, minPrice, maxPrice, floor);

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            setWhereParameters(ps, roomTypeId, minPrice, maxPrice, floor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Bước 4: Viết hàm findRoomsPage lấy danh sách phòng có phân trang
    public List<Room> findRoomsPage(Integer roomTypeId, Integer minPrice, Integer maxPrice, Integer floor, int page, int pageSize) {
        List<Room> list = new ArrayList<>();

        String sql = "SELECT r.roomCode, rt.name AS roomTypeName, r.monthlyPrice, r.floor, r.imageUrl "
                + "FROM Room r "
                + "JOIN RoomType rt ON r.roomTypeId = rt.roomTypeId "
                + "WHERE " + buildWhere(roomTypeId, minPrice, maxPrice, floor)
                + " ORDER BY r.floor ASC, r.roomCode ASC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1. Gán 8 tham số cho mệnh đề WHERE
            setWhereParameters(ps, roomTypeId, minPrice, maxPrice, floor);

            // 2. Gán 2 tham số cho OFFSET và FETCH
            int offset = (page - 1) * pageSize;
            int index = 9;
            ps.setInt(index++, offset);
            ps.setInt(index, pageSize);

            // 3. Đọc dữ liệu vào Object Room
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room room = new Room();
                    room.setRoomCode(rs.getString("roomCode"));
                    room.setRoomTypeName(rs.getString("roomTypeName"));
                    room.setMonthlyPrice(rs.getInt("monthlyPrice"));
                    room.setFloor(rs.getInt("floor"));
                    room.setImageUrl(rs.getString("imageUrl"));

                    list.add(room);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
