package service;

import dal.ContractDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import model.Contract;

public class ContractService {
    public static final int PAGE_SIZE = 10;

    public ContractPage findPage(String status, Integer roomId, String tenantName, int page)
            throws SQLException {
        if (status == null || !Set.of("", "ACTIVE", "ENDED").contains(status)) {
            throw new IllegalArgumentException("Trạng thái hợp đồng không hợp lệ.");
        }
        if (page <= 0 || (roomId != null && roomId <= 0)) {
            throw new IllegalArgumentException("Số trang và mã phòng phải là số nguyên dương.");
        }
        if (tenantName == null || tenantName.length() > 100) {
            throw new IllegalArgumentException("Tên người thuê không được vượt quá 100 ký tự.");
        }
        try (ContractDAO dao = new ContractDAO()) {
            requireConnection(dao);
            long total = dao.count(status, roomId, tenantName);
            long totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            int currentPage = (int) Math.min(page, totalPages);
            return new ContractPage(dao.findPage(status, roomId, tenantName,
                    (long) (currentPage - 1) * PAGE_SIZE, PAGE_SIZE), total, currentPage, totalPages);
        }
    }

    public void endContract(int contractId, int managerId) throws SQLException {
        if (contractId <= 0 || managerId <= 0) {
            throw new IllegalArgumentException("Mã hợp đồng hoặc tài khoản quản lý không hợp lệ.");
        }
        try (ContractDAO dao = new ContractDAO()) {
            Connection connection = requireConnection(dao);
            connection.setAutoCommit(false);
            try {
                if (!dao.isActiveManager(managerId)) {
                    throw new IllegalArgumentException("Tài khoản quản lý không còn hoạt động.");
                }
                Integer roomId = dao.findRoomId(contractId);
                if (roomId == null) {
                    throw new IllegalArgumentException("Hợp đồng không tồn tại.");
                }
                // Khóa phòng trước hợp đồng, cùng thứ tự với luồng duyệt thuê NT-36.
                dao.lockRoom(roomId);
                if (!"ACTIVE".equals(dao.lockStatus(contractId))) {
                    throw new IllegalStateException("Chỉ có thể kết thúc hợp đồng đang hiệu lực.");
                }
                BigDecimal unpaid = dao.sumUnpaid(contractId);
                if (unpaid.signum() > 0) {
                    throw new IllegalStateException("Không thể kết thúc hợp đồng: còn "
                            + NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(unpaid)
                            + " đồng hóa đơn chưa thanh toán.");
                }
                if (!dao.endContract(contractId)) {
                    throw new IllegalStateException("Hợp đồng vừa được xử lý. Vui lòng tải lại trang.");
                }
                // Schema suy ra phòng trống từ việc không còn Contract ACTIVE.
                // Không đổi Room.status vì có thể phòng đang bảo trì hoặc bị ẩn.
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); }
                catch (SQLException rollbackError) { ex.addSuppressed(rollbackError); }
                throw ex;
            }
        }
    }

    private Connection requireConnection(ContractDAO dao) throws SQLException {
        if (dao.getConnection() == null) {
            throw new SQLException("Database connection is unavailable.");
        }
        return dao.getConnection();
    }

    public static class ContractPage {
        private final List<Contract> contracts;
        private final long total;
        private final int page;
        private final long totalPages;

        public ContractPage(List<Contract> contracts, long total, int page, long totalPages) {
            this.contracts = contracts;
            this.total = total;
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<Contract> getContracts() { return contracts; }
        public long getTotal() { return total; }
        public int getPage() { return page; }
        public long getTotalPages() { return totalPages; }
    }
}
