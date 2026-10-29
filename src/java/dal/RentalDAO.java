package dal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Contract;
import model.RentalRequest;

/** Giữ connection kế thừa đến khi đóng DAO để có thể gọi nhiều truy vấn liên tiếp. */
public class RentalDAO extends DBContext implements AutoCloseable {

    /** Luôn tạo request PENDING và trả ID để caller có thể tra cứu bản ghi vừa tạo. */
    public int insertRequest(RentalRequest request) throws SQLException {
        String sql = """
                INSERT INTO RentalRequest
                    (tenantId, roomId, moveInDate, months, numPeople, note, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getTenantId());
            ps.setInt(2, request.getRoomId());
            ps.setObject(3, request.getMoveInDate(), java.sql.Types.DATE);
            ps.setInt(4, request.getMonths());
            ps.setInt(5, request.getNumPeople());
            ps.setNString(6, request.getNote());
            ps.setString(7, "PENDING");
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public List<RentalRequest> findRequestsByTenant(int tenantId) throws SQLException {
        String sql = """
                SELECT requestId, tenantId, roomId, moveInDate, months, numPeople,
                       note, status, createdAt, decidedAt, decidedBy
                FROM RentalRequest
                WHERE tenantId = ?
                ORDER BY createdAt DESC, requestId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RentalRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(mapRequest(rs));
                }
                return requests;
            }
        }
    }

    public List<RentalRequest> findPendingRequests() throws SQLException {
        return findPendingRequests(null);
    }

    public List<RentalRequest> findPendingRequests(Integer roomId) throws SQLException {
        String sql = """
                SELECT r.requestId, r.tenantId, r.roomId, r.moveInDate, r.months, r.numPeople,
                       r.note, r.status, r.createdAt, r.decidedAt, r.decidedBy,
                       room.roomCode, room.monthlyPrice, a.fullName AS tenantName
                FROM RentalRequest r
                JOIN Room room ON room.roomId = r.roomId
                JOIN Account a ON a.accountId = r.tenantId
                WHERE r.status = ? AND (? IS NULL OR r.roomId = ?)
                ORDER BY r.createdAt, r.requestId
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            ps.setObject(2, roomId, Types.INTEGER);
            ps.setObject(3, roomId, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) {
                List<RentalRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    RentalRequest request = mapRequest(rs);
                    request.setRoomCode(rs.getString("roomCode"));
                    request.setTenantName(rs.getNString("tenantName"));
                    request.setMonthlyPrice(rs.getBigDecimal("monthlyPrice"));
                    requests.add(request);
                }
                return requests;
            }
        }
    }

    /** Trả null để caller phân biệt request không tồn tại với lỗi truy vấn. */
    public RentalRequest findById(int id) throws SQLException {
        String sql = """
                SELECT requestId, tenantId, roomId, moveInDate, months, numPeople,
                       note, status, createdAt, decidedAt, decidedBy
                FROM RentalRequest
                WHERE requestId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRequest(rs) : null;
            }
        }
    }

    public boolean isActiveManager(int accountId) throws SQLException {
        String sql = "SELECT 1 FROM Account WHERE accountId = ? AND role = ? AND active = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, "MANAGER");
            ps.setBoolean(3, true);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public record RoomTerms(String status, BigDecimal monthlyPrice, int capacity) { }

    /** Khóa phòng trước khi cập nhật request để các lần duyệt cùng phòng có cùng thứ tự khóa. */
    public RoomTerms lockRoomForApproval(int roomId) throws SQLException {
        String sql = """
                SELECT r.status, r.monthlyPrice, t.capacity
                FROM Room r WITH (UPDLOCK, HOLDLOCK)
                JOIN RoomType t ON t.roomTypeId = r.roomTypeId
                WHERE r.roomId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new RoomTerms(rs.getString("status"),
                        rs.getBigDecimal("monthlyPrice"), rs.getInt("capacity")) : null;
            }
        }
    }

    public boolean hasActiveContractForRoom(int roomId) throws SQLException {
        String sql = "SELECT 1 FROM Contract WHERE roomId = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int decidePendingRequest(int requestId, int managerId, String status) throws SQLException {
        String sql = """
                UPDATE RentalRequest SET status = ?, decidedAt = SYSDATETIME(), decidedBy = ?
                WHERE requestId = ? AND status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, managerId);
            ps.setInt(3, requestId);
            ps.setString(4, "PENDING");
            return ps.executeUpdate();
        }
    }

    public int rejectOtherPendingRequests(int roomId, int approvedRequestId, int managerId) throws SQLException {
        String sql = """
                UPDATE RentalRequest SET status = ?, decidedAt = SYSDATETIME(), decidedBy = ?
                WHERE roomId = ? AND requestId <> ? AND status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "REJECTED");
            ps.setInt(2, managerId);
            ps.setInt(3, roomId);
            ps.setInt(4, approvedRequestId);
            ps.setString(5, "PENDING");
            return ps.executeUpdate();
        }
    }

    /**
     * Trả contractId vừa tạo. Service sở hữu con và điều khiển transaction,
     * nên DAO chỉ đóng statement, không đóng, commit hay rollback con.
     */
    public int insertContract(Connection con, Contract c) throws SQLException {
        String sql = """
                INSERT INTO Contract
                    (requestId, tenantId, roomId, startDate, endDate, monthlyRent,
                     deposit, numPeople, startElectricIndex, startWaterIndex, status, endedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getRequestId());
            ps.setInt(2, c.getTenantId());
            ps.setInt(3, c.getRoomId());
            ps.setObject(4, c.getStartDate(), java.sql.Types.DATE);
            ps.setObject(5, c.getEndDate(), java.sql.Types.DATE);
            ps.setBigDecimal(6, c.getMonthlyRent());
            ps.setBigDecimal(7, c.getDeposit());
            ps.setInt(8, c.getNumPeople());
            ps.setInt(9, c.getStartElectricIndex());
            ps.setInt(10, c.getStartWaterIndex());
            ps.setString(11, c.getStatus());
            ps.setTimestamp(12, c.getEndedAt() == null ? null : Timestamp.valueOf(c.getEndedAt()));
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private RentalRequest mapRequest(ResultSet rs) throws SQLException {
        return new RentalRequest(
                rs.getInt("requestId"),
                rs.getInt("tenantId"),
                rs.getInt("roomId"),
                rs.getObject("moveInDate", LocalDate.class),
                rs.getInt("months"),
                rs.getInt("numPeople"),
                rs.getNString("note"),
                rs.getString("status"),
                rs.getObject("createdAt", LocalDateTime.class),
                rs.getObject("decidedAt", LocalDateTime.class),
                rs.getObject("decidedBy", Integer.class));
    }

    private int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("INSERT did not return a generated ID.");
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
