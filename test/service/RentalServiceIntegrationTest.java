package service;

import dal.RentalDAO;
import dal.RentalRequestTestDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import model.RentalRequest;

/** Chạy bằng class loader của webapp để giữ nguyên DBContext của dự án. */
public class RentalServiceIntegrationTest {
    public static void main(String[] args) throws Exception {
        RentalService service = new RentalService();
        try (RentalRequestTestDAO fixtures = new RentalRequestTestDAO()) {
            int tenantId = fixtures.createTenant();
            int otherTenantId = fixtures.createTenant();
            try {
                int roomId = fixtures.findRoom(false);
                int occupiedRoomId = fixtures.findRoom(true);
                LocalDate today = LocalDate.now();
                expectRejected(() -> service.createRequest(tenantId, occupiedRoomId, today, 1, 1, null), "Phòng đã có người thuê");
                expectRejected(() -> service.createRequest(fixtures.findActiveTenant(), roomId, today, 1, 1, null), "hợp đồng");
                expectRejected(() -> service.createRequest(tenantId, roomId, today.minusDays(1), 1, 1, null), "Ngày");
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 0, 1, null), "Số tháng");
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 25, 1, null), "Số tháng");
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 1, 0, null), "Số người");
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 1,
                        service.findRoom(roomId).getCapacity() + 1, null), "sức chứa");
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 1, 1, "a".repeat(501)), "500");
                expectRejected(() -> service.createRequest(tenantId, Integer.MAX_VALUE, today, 1, 1, null), "không tồn tại");
                System.out.println("PASS: occupied room, active contract, date, duration, capacity and note limits");

                String note = "Tiếng Việt <script>alert(1)</script> '); DROP TABLE RentalRequest; --";
                int requestId = service.createRequest(tenantId, roomId, today, 24, 1, note);
                try (RentalDAO dao = new RentalDAO()) {
                    RentalRequest saved = dao.findById(requestId);
                    require(saved != null && "PENDING".equals(saved.getStatus()), "Request must be PENDING");
                    require(note.equals(saved.getNote()), "Unicode and SQL-like note must round-trip");
                    require(service.findRequestsByTenant(otherTenantId).isEmpty(), "No cross-tenant requests");
                }
                expectRejected(() -> service.createRequest(tenantId, roomId, today, 1, 1, null), "chờ duyệt");
                require(service.findRequestsByTenant(tenantId).size() == 1, "Second submission must not insert");
                System.out.println("PASS: success, PENDING, Unicode binding, ownership and sequential duplicate rejection");

                fixtures.deleteRequests(tenantId);
                ExecutorService pool = Executors.newFixedThreadPool(2);
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                try {
                    Future<Boolean> first = pool.submit(() -> submitTogether(service, tenantId, roomId, ready, start));
                    Future<Boolean> second = pool.submit(() -> submitTogether(service, tenantId, roomId, ready, start));
                    require(ready.await(10, java.util.concurrent.TimeUnit.SECONDS), "Workers must become ready");
                    start.countDown();
                    boolean firstCreated = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
                    boolean secondCreated = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
                    require(firstCreated != secondCreated, "Exactly one concurrent request must succeed");
                    require(service.findRequestsByTenant(tenantId).size() == 1, "Only one PENDING row may exist");
                } finally {
                    start.countDown();
                    pool.shutdownNow();
                    pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
                }
                System.out.println("PASS: concurrent duplicate submission is rejected without a second row");
            } finally {
                fixtures.deleteTenant(tenantId);
                fixtures.deleteTenant(otherTenantId);
            }
        }
        System.out.println("PASS: test-owned accounts and requests cleaned up");
    }

    private static boolean submitTogether(RentalService service, int tenantId, int roomId,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            service.createRequest(tenantId, roomId, LocalDate.now(), 1, 1, null);
            return true;
        } catch (IllegalArgumentException ex) {
            require(ex.getMessage().contains("chờ duyệt"), "Only a pending-request rejection is expected");
            return false;
        }
    }

    private static void expectRejected(CheckedAction action, String reason) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Expected rejection: " + reason);
        } catch (IllegalArgumentException ex) {
            require(ex.getMessage().contains(reason), "Wrong rejection: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws SQLException;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
