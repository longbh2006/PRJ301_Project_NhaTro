package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Contract;
import model.RentalRequest;
import model.RentalRoom;

/** Giữ connection kế thừa đến khi đóng DAO để có thể gọi nhiều truy vấn liên tiếp. */
public class RentalDAO extends DBContext implements AutoCloseable {

    public boolean lockActiveTenant(int tenantId) throws SQLException {
        String sql = """
                SELECT t.tenantId
                FROM Tenant t WITH (UPDLOCK, HOLDLOCK)
                JOIN Account a ON a.accountId = t.tenantId
                WHERE t.tenantId = ? AND a.role = ? AND a.active = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, "TENANT");
            ps.setBoolean(3, true);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public RentalRoom findRentalRoom(int roomId) throws SQLException {
        String sql = """
                SELECT r.roomId, r.roomCode, rt.name AS roomTypeName, rt.capacity,
                       r.monthlyPrice, r.status, r.description, r.floor, r.area, r.imageUrl,
                       CASE WHEN EXISTS (SELECT 1 FROM Contract c
                                         WHERE c.roomId = r.roomId AND c.status = ?)
                            THEN 1 ELSE 0 END AS occupied
                FROM Room r JOIN RoomType rt ON rt.roomTypeId = r.roomTypeId
                WHERE r.roomId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            ps.setInt(2, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new RentalRoom(rs.getInt("roomId"), rs.getString("roomCode"),
                        rs.getNString("roomTypeName"), rs.getInt("capacity"),
                        rs.getBigDecimal("monthlyPrice"), rs.getString("status"),
                        rs.getNString("description"), rs.getBoolean("occupied"),
                        rs.getInt("floor"), rs.getBigDecimal("area"), rs.getString("imageUrl")) : null;
            }
        }
    }

    public boolean hasActiveContract(int tenantId) throws SQLException {
        String sql = "SELECT 1 FROM Contract WHERE tenantId = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasPendingRequest(int tenantId) throws SQLException {
        String sql = "SELECT 1 FROM RentalRequest WHERE tenantId = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

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
                SELECT rr.requestId, rr.tenantId, rr.roomId, rr.moveInDate, rr.months, rr.numPeople,
                       rr.note, rr.status, rr.createdAt, rr.decidedAt, rr.decidedBy, r.roomCode
                FROM RentalRequest rr JOIN Room r ON r.roomId = rr.roomId
                WHERE rr.tenantId = ?
                ORDER BY rr.createdAt DESC, rr.requestId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RentalRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    RentalRequest rentalRequest = mapRequest(rs);
                    rentalRequest.setRoomCode(rs.getString("roomCode"));
                    requests.add(rentalRequest);
                }
                return requests;
            }
        }
    }

    public boolean cancelPendingRequest(int requestId, int tenantId) throws SQLException {
        String sql = """
                UPDATE RentalRequest SET status = ?, decidedAt = SYSDATETIME(), decidedBy = NULL
                WHERE requestId = ? AND tenantId = ? AND status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "CANCELLED");
            ps.setInt(2, requestId);
            ps.setInt(3, tenantId);
            ps.setString(4, "PENDING");
            return ps.executeUpdate() == 1;
        }
    }

    public Contract findActiveContractByTenant(int tenantId) throws SQLException {
        String sql = """
                SELECT c.contractId, c.requestId, c.tenantId, c.roomId, c.startDate,
                       c.endDate, c.monthlyRent, c.deposit, c.numPeople,
                       c.startElectricIndex, c.startWaterIndex, c.status, c.endedAt, r.roomCode
                FROM Contract c JOIN Room r ON r.roomId = c.roomId
                WHERE c.tenantId = ? AND c.status = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Contract contract = new Contract(rs.getInt("contractId"), rs.getInt("requestId"),
                        rs.getInt("tenantId"), rs.getInt("roomId"),
                        rs.getObject("startDate", LocalDate.class), rs.getObject("endDate", LocalDate.class),
                        rs.getBigDecimal("monthlyRent"), rs.getBigDecimal("deposit"), rs.getInt("numPeople"),
                        rs.getInt("startElectricIndex"), rs.getInt("startWaterIndex"),
                        rs.getString("status"), rs.getObject("endedAt", LocalDateTime.class));
                contract.setRoomCode(rs.getString("roomCode"));
                return contract;
            }
        }
    }

    public List<RentalRequest> findPendingRequests() throws SQLException {
        String sql = """
                SELECT requestId, tenantId, roomId, moveInDate, months, numPeople,
                       note, status, createdAt, decidedAt, decidedBy
                FROM RentalRequest
                WHERE status = ?
                ORDER BY createdAt, requestId
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                List<RentalRequest> requests = new ArrayList<>();
                while (rs.next()) {
                    requests.add(mapRequest(rs));
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
