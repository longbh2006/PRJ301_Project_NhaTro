package dal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Contract;
import model.RentalRequest;

/** Chạy trong môi trường web để DBContext gốc phân giải đúng resource cấu hình. */
public class RentalDAOIntegrationTest {

    public static void main(String[] args) throws SQLException {
        Connection ownedConnection;
        try (RentalDAO dao = new RentalDAO(); RentalTestDAO testDAO = new RentalTestDAO()) {
            ownedConnection = dao.getConnection();
            runChecks(dao, testDAO);
        }
        require(ownedConnection.isClosed(), "Closing the DAO must close its inherited connection");
        System.out.println("PASS: DAO closes its inherited connection");
    }

    private static void runChecks(RentalDAO dao, RentalTestDAO testDAO) throws SQLException {
        RentalRequest request = testDAO.findSampleRequest();
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
                    require(testDAO.approveRequest(con, requestId) == 1, "Approval must update exactly one request");
                    contractId = dao.insertContract(con, contract);
                    require(!con.isClosed() && !con.getAutoCommit(), "DAO must leave caller's transaction open");
                    Contract savedContract = testDAO.findContract(con, contractId);
                    require(savedContract != null, "Contract must exist in the caller's transaction");
                    require(contract.getMonthlyRent().compareTo(savedContract.getMonthlyRent()) == 0,
                            "Monthly rent must preserve DECIMAL(12,0) precision");
                    require(contract.getDeposit().compareTo(savedContract.getDeposit()) == 0,
                            "Deposit must preserve DECIMAL(12,0) precision");
                    require(savedContract.getStartElectricIndex() == 125 && savedContract.getStartWaterIndex() == 40,
                            "Initial meter readings must round-trip");
                    require(contract.getEndedAt().equals(savedContract.getEndedAt()), "endedAt must round-trip");
                } finally {
                    con.rollback();
                }
                require(testDAO.countContracts(con, requestId) == 0, "Rollback must remove the contract");
                con.rollback();
            }
            require("PENDING".equals(dao.findById(requestId).getStatus()),
                    "Rollback must also undo request approval");
            System.out.println("PASS: contract insert, BigDecimal precision, connection ownership and atomic rollback");

            try (Connection con = new DBContext().getConnection()) {
                con.setAutoCommit(false);
                try {
                    require(testDAO.approveRequest(con, requestId) == 1, "Approval must update exactly one request");
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
                require(testDAO.countContracts(con, requestId) == 0, "Failure rollback must remove the first insert");
                con.rollback();
            }
            require("PENDING".equals(dao.findById(requestId).getStatus()),
                    "Failure rollback must undo request approval");
            System.out.println("PASS: UNIQUE violation propagates and caller can roll back both writes");
        } finally {
            testDAO.deleteRequest(requestId);
        }
        require(dao.findById(requestId) == null, "Test request must be removed");
        System.out.println("PASS: test data cleaned up");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
