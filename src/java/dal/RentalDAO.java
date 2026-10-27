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

/** Close the DAO after use to release the connection inherited from DBContext. */
public class RentalDAO extends DBContext implements AutoCloseable {

    /** Inserts a new PENDING request and returns its generated requestId. */
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

    /** Returns null when the request does not exist. */
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
     * Returns the generated contractId. The caller owns con and manages its
     * transaction; this method never closes, commits or rolls back con.
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
