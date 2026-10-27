package dal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import model.Contract;
import model.RentalRequest;

/** Standalone integration check against setup.sql; removes its own test request. */
public class RentalDAOIntegrationTest {

    public static void main(String[] args) throws SQLException {
        Connection ownedConnection;
        try (RentalDAO dao = new RentalDAO()) {
            ownedConnection = dao.getConnection();
            runChecks(dao);
        }
        require(ownedConnection.isClosed(), "Closing the DAO must close its inherited connection");
        System.out.println("PASS: DAO closes its inherited connection");
    }

    private static void runChecks(RentalDAO dao) throws SQLException {
        RentalRequest request = sampleRequest();
        request.setStatus("APPROVED");
        request.setNote("NT-26: Tiếng Việt '); DROP TABLE RentalRequest; --");
        int requestId = dao.insertRequest(request);
        try {
            RentalRequest saved = dao.findById(requestId);
            require(saved != null && "PENDING".equals(saved.getStatus()), "INSERT must force PENDING");
            require(saved.getCreatedAt() != null, "createdAt must come from the database default");
            require(saved.getDecidedAt() == null && saved.getDecidedBy() == null,
                    "An undecided request must preserve null decision fields");
            require(request.getNote().equals(saved.getNote()), "Unicode and SQL-like text must round-trip");
            require(request.getMoveInDate().equals(saved.getMoveInDate()), "moveInDate must round-trip");
            List<RentalRequest> tenantRequests = dao.findRequestsByTenant(request.getTenantId());
            require(tenantRequests.stream().allMatch(r -> r.getTenantId() == request.getTenantId()),
                    "Tenant query must not return another tenant's requests");
            require(tenantRequests.stream().anyMatch(r -> r.getRequestId() == requestId),
                    "Tenant query must include the new request");
            List<RentalRequest> pending = dao.findPendingRequests();
            require(pending.stream().allMatch(r -> "PENDING".equals(r.getStatus())),
                    "Pending query must only return PENDING requests");
            require(pending.stream().anyMatch(r -> r.getRequestId() == requestId),
                    "Pending query must include the new request");
            require(dao.findById(-1) == null, "Missing ID must return null");
            require(dao.findRequestsByTenant(-1).isEmpty(), "Missing tenant must return an empty list");
            System.out.println("PASS: request insert, generated ID, filtering, dates, nulls and Unicode binding");

            Contract contract = new Contract(0, requestId, saved.getTenantId(), saved.getRoomId(),
                    saved.getMoveInDate(), saved.getMoveInDate().plusMonths(saved.getMonths()),
                    new BigDecimal("999999999999"), new BigDecimal("123456789012"),
                    saved.getNumPeople(), 125, 40, "ENDED",
                    saved.getMoveInDate().plusMonths(saved.getMonths()).atStartOfDay());
            int contractId;
            try (Connection con = new DBContext().getConnection()) {
                con.setAutoCommit(false);
                try {
                    approve(con, requestId);
                    contractId = dao.insertContract(con, contract);
                    require(!con.isClosed() && !con.getAutoCommit(), "DAO must leave caller's transaction open");
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT monthlyRent, deposit, startElectricIndex, startWaterIndex, endedAt FROM Contract WHERE contractId = ?")) {
                        ps.setInt(1, contractId);
                        try (ResultSet rs = ps.executeQuery()) {
                            require(rs.next(), "Contract must exist in the caller's transaction");
                            require(contract.getMonthlyRent().compareTo(rs.getBigDecimal("monthlyRent")) == 0,
                                    "Monthly rent must preserve DECIMAL(12,0) precision");
                            require(contract.getDeposit().compareTo(rs.getBigDecimal("deposit")) == 0,
                                    "Deposit must preserve DECIMAL(12,0) precision");
                            require(rs.getInt("startElectricIndex") == 125 && rs.getInt("startWaterIndex") == 40,
                                    "Initial meter readings must round-trip");
                            require(contract.getEndedAt().equals(rs.getTimestamp("endedAt").toLocalDateTime()),
                                    "endedAt must round-trip");
                        }
                    }
                } finally {
                    con.rollback();
                }
                require(contractCount(con, requestId) == 0, "Rollback must remove the contract");
                con.rollback();
            }
            require("PENDING".equals(dao.findById(requestId).getStatus()),
                    "Rollback must also undo request approval");
            System.out.println("PASS: contract insert, BigDecimal precision, connection ownership and atomic rollback");

            try (Connection con = new DBContext().getConnection()) {
                con.setAutoCommit(false);
                try {
                    approve(con, requestId);
                    dao.insertContract(con, contract);
                    boolean duplicateFailed = false;
                    try {
                        dao.insertContract(con, contract);
                    } catch (SQLException expected) {
                        duplicateFailed = true;
                    }
                    require(duplicateFailed, "A duplicate contract must propagate SQLException");
                    require(!con.isClosed(), "DAO must leave the connection open after failure");
                } finally {
                    con.rollback();
                }
                require(contractCount(con, requestId) == 0, "Failure rollback must remove the first insert");
                con.rollback();
            }
            require("PENDING".equals(dao.findById(requestId).getStatus()),
                    "Failure rollback must undo request approval");
            System.out.println("PASS: UNIQUE violation propagates and caller can roll back both writes");
        } finally {
            try (Connection con = new DBContext().getConnection();
                    PreparedStatement ps = con.prepareStatement("DELETE FROM RentalRequest WHERE requestId = ?")) {
                ps.setInt(1, requestId);
                ps.executeUpdate();
            }
        }
        require(dao.findById(requestId) == null, "Test request must be removed");
        System.out.println("PASS: test data cleaned up");
    }

    private static RentalRequest sampleRequest() throws SQLException {
        String sql = """
                SELECT TOP (1) t.tenantId, r.roomId, r.moveInDate, r.months, r.numPeople
                FROM RentalRequest r
                CROSS JOIN Tenant t
                WHERE r.status = ?
                  AND NOT EXISTS (SELECT 1 FROM RentalRequest pending WHERE pending.tenantId = t.tenantId AND pending.status = ?)
                ORDER BY t.tenantId, r.requestId
                """;
        try (Connection con = new DBContext().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            ps.setString(2, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                require(rs.next(), "Seed a PENDING request and a separate tenant without a PENDING request first");
                return new RentalRequest(0, rs.getInt("tenantId"), rs.getInt("roomId"),
                        rs.getDate("moveInDate").toLocalDate(), rs.getInt("months"),
                        rs.getInt("numPeople"), null, null, null, null, null);
            }
        }
    }

    private static void approve(Connection con, int requestId) throws SQLException {
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
            require(ps.executeUpdate() == 1, "Approval must update exactly one request");
        }
    }

    private static int contractCount(Connection con, int requestId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Contract WHERE requestId = ?")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
