package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import model.Contract;
import model.RentalRequest;

/** Tách SQL kiểm thử khỏi lớp chạy test để SQL chỉ nằm trong DAO. */
public class RentalTestDAO extends DBContext implements AutoCloseable {

    public RentalRequest findSampleRequest() throws SQLException {
        String sql = """
                SELECT TOP (1) t.tenantId, r.roomId, r.moveInDate, r.months, r.numPeople
                FROM RentalRequest r
                CROSS JOIN Tenant t
                WHERE r.status = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM RentalRequest pending
                      WHERE pending.tenantId = t.tenantId AND pending.status = ?
                  )
                ORDER BY t.tenantId, r.requestId
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            ps.setString(2, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Cần có request PENDING và một tenant khác chưa có request PENDING để chạy test.");
                }
                return new RentalRequest(0, rs.getInt("tenantId"), rs.getInt("roomId"),
                        rs.getObject("moveInDate", LocalDate.class), rs.getInt("months"),
                        rs.getInt("numPeople"), null, null, null, null, null);
            }
        }
    }

    public Contract findContract(Connection con, int contractId) throws SQLException {
        String sql = """
                SELECT contractId, requestId, tenantId, roomId, startDate, endDate,
                       monthlyRent, deposit, numPeople, startElectricIndex,
                       startWaterIndex, status, endedAt
                FROM Contract
                WHERE contractId = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Contract(rs.getInt("contractId"), rs.getInt("requestId"),
                        rs.getInt("tenantId"), rs.getInt("roomId"),
                        rs.getObject("startDate", LocalDate.class), rs.getObject("endDate", LocalDate.class),
                        rs.getBigDecimal("monthlyRent"), rs.getBigDecimal("deposit"),
                        rs.getInt("numPeople"), rs.getInt("startElectricIndex"),
                        rs.getInt("startWaterIndex"), rs.getString("status"),
                        rs.getObject("endedAt", LocalDateTime.class));
            }
        }
    }

    public int approveRequest(Connection con, int requestId) throws SQLException {
        String sql = """
                UPDATE RentalRequest
                SET status = ?, decidedAt = SYSDATETIME(),
                    decidedBy = (SELECT MIN(accountId) FROM Account WHERE role = ?)
                WHERE requestId = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "APPROVED");
            ps.setString(2, "MANAGER");
            ps.setInt(3, requestId);
            return ps.executeUpdate();
        }
    }

    public int countContracts(Connection con, int requestId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Contract WHERE requestId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void deleteRequest(int requestId) throws SQLException {
        String sql = "DELETE FROM RentalRequest WHERE requestId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
