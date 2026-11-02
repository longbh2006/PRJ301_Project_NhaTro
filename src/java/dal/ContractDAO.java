package dal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Contract;

public class ContractDAO extends DBContext implements AutoCloseable {
    public long count(String status, Integer roomId, String tenantName) throws SQLException {
        String sql = """
                SELECT COUNT_BIG(*)
                FROM Contract c JOIN Room r ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                WHERE (? = '' OR c.status = ?)
                  AND (? IS NULL OR c.roomId = ?)
                  AND a.fullName LIKE ? ESCAPE '\\'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindFilters(ps, status, roomId, tenantName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<Contract> findPage(String status, Integer roomId, String tenantName,
            long offset, int pageSize) throws SQLException {
        String sql = """
                SELECT c.*, r.roomCode, a.fullName AS tenantName
                FROM Contract c JOIN Room r ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                WHERE (? = '' OR c.status = ?)
                  AND (? IS NULL OR c.roomId = ?)
                  AND a.fullName LIKE ? ESCAPE '\\'
                ORDER BY c.contractId DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindFilters(ps, status, roomId, tenantName);
            ps.setLong(6, offset);
            ps.setInt(7, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                List<Contract> contracts = new ArrayList<>();
                while (rs.next()) {
                    Contract contract = new Contract(rs.getInt("contractId"), rs.getInt("requestId"),
                            rs.getInt("tenantId"), rs.getInt("roomId"),
                            rs.getObject("startDate", LocalDate.class), rs.getObject("endDate", LocalDate.class),
                            rs.getBigDecimal("monthlyRent"), rs.getBigDecimal("deposit"), rs.getInt("numPeople"),
                            rs.getInt("startElectricIndex"), rs.getInt("startWaterIndex"),
                            rs.getString("status"), rs.getObject("endedAt", LocalDateTime.class));
                    contract.setRoomCode(rs.getString("roomCode"));
                    contract.setTenantName(rs.getNString("tenantName"));
                    contracts.add(contract);
                }
                return contracts;
            }
        }
    }

    private void bindFilters(PreparedStatement ps, String status, Integer roomId,
            String tenantName) throws SQLException {
        ps.setString(1, status);
        ps.setString(2, status);
        ps.setObject(3, roomId, java.sql.Types.INTEGER);
        ps.setObject(4, roomId, java.sql.Types.INTEGER);
        // Ký tự LIKE do người dùng nhập phải được tìm như văn bản thông thường.
        String escaped = tenantName.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[");
        ps.setNString(5, "%".concat(escaped).concat("%"));
    }

    public boolean isActiveManager(int managerId) throws SQLException {
        String sql = "SELECT accountId FROM Account WITH (HOLDLOCK) WHERE accountId = ? AND role = ? AND active = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            ps.setString(2, "MANAGER");
            ps.setBoolean(3, true);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Integer findRoomId(int contractId) throws SQLException {
        String sql = "SELECT roomId FROM Contract WHERE contractId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    public void lockRoom(int roomId) throws SQLException {
        String sql = "SELECT roomId FROM Room WITH (UPDLOCK, HOLDLOCK) WHERE roomId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { throw new SQLException("Contract room is missing."); }
            }
        }
    }

    public String lockStatus(int contractId) throws SQLException {
        String sql = "SELECT status FROM Contract WITH (UPDLOCK, HOLDLOCK) WHERE contractId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public Map<String, BigDecimal> findInvoiceTotals(int contractId) throws SQLException {
        String sql = """
                SELECT status, SUM(total) AS total FROM Invoice WITH (UPDLOCK, HOLDLOCK)
                WHERE contractId = ?
                GROUP BY status
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, BigDecimal> totals = new LinkedHashMap<>();
                while (rs.next()) {
                    totals.put(rs.getString("status"), rs.getBigDecimal("total"));
                }
                return totals;
            }
        }
    }

    public boolean endContract(int contractId) throws SQLException {
        String sql = "UPDATE Contract SET status = ?, endedAt = SYSDATETIME() WHERE contractId = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "ENDED");
            ps.setInt(2, contractId);
            ps.setString(3, "ACTIVE");
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) { connection.close(); }
    }
}
