package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Dữ liệu kiểm thử riêng, không thay đổi tài khoản hoặc yêu cầu có sẵn. */
public class RentalRequestTestDAO extends DBContext implements AutoCloseable {
    public int createTenant() throws SQLException {
        String sql = """
                INSERT INTO Account (username, passwordHash, fullName, role)
                VALUES (?, ?, ?, ?)
                """;
        int tenantId;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "nt34_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            ps.setString(2, "test-only-disabled-login");
            ps.setNString(3, "Kiểm thử NT-34");
            ps.setString(4, "TENANT");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Missing tenant ID");
                }
                tenantId = rs.getInt(1);
            }
        }
        String tenantSql = "INSERT INTO Tenant (tenantId, idCardNo) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(tenantSql)) {
            ps.setInt(1, tenantId);
            ps.setString(2, String.format("%012d", tenantId));
            ps.executeUpdate();
        } catch (SQLException ex) {
            deleteAccount(tenantId);
            throw ex;
        }
        return tenantId;
    }

    public int findRoom(boolean occupied) throws SQLException {
        String sql = """
                SELECT TOP (1) r.roomId
                FROM Room r
                WHERE r.status = ?
                  AND (CASE WHEN EXISTS (SELECT 1 FROM Contract c
                                         WHERE c.roomId = r.roomId AND c.status = ?)
                            THEN 1 ELSE 0 END) = ?
                ORDER BY r.roomId
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            ps.setString(2, "ACTIVE");
            ps.setBoolean(3, occupied);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Cần phòng trống và phòng đã có hợp đồng ACTIVE để kiểm thử.");
                }
                return rs.getInt(1);
            }
        }
    }

    public int findActiveTenant() throws SQLException {
        String sql = "SELECT TOP (1) tenantId FROM Contract WHERE status = ? ORDER BY tenantId";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Cần hợp đồng ACTIVE để kiểm thử.");
                }
                return rs.getInt(1);
            }
        }
    }

    public void deleteRequests(int tenantId) throws SQLException {
        String sql = "DELETE FROM RentalRequest WHERE tenantId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        }
    }

    public void deleteTenant(int tenantId) throws SQLException {
        deleteRequests(tenantId);
        String sql = "DELETE FROM Tenant WHERE tenantId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        }
        deleteAccount(tenantId);
    }

    private void deleteAccount(int accountId) throws SQLException {
        String sql = "DELETE FROM Account WHERE accountId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, accountId);
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
