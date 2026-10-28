package service;

import dal.RentalDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import model.RentalRequest;
import model.RentalRoom;

public class RentalService {
    public RentalRoom findRoom(int roomId) throws SQLException {
        try (RentalDAO dao = new RentalDAO()) {
            RentalRoom room = dao.findRentalRoom(roomId);
            return room == null || "HIDDEN".equals(room.getStatus()) ? null : room;
        }
    }

    public List<RentalRequest> findRequestsByTenant(int tenantId) throws SQLException {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tài khoản người thuê không hợp lệ.");
        }
        try (RentalDAO dao = new RentalDAO()) {
            return dao.findRequestsByTenant(tenantId);
        }
    }

    public List<RentalRequest> findRequestsByTenant(int tenantId, String status, String query)
            throws SQLException {
        if (!Set.of("", "PENDING", "APPROVED", "REJECTED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Trạng thái lọc không hợp lệ.");
        }
        if (query.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được vượt quá 100 ký tự.");
        }
        String keyword = query.toLowerCase(Locale.ROOT);
        return findRequestsByTenant(tenantId).stream()
                .filter(item -> status.isEmpty() || status.equals(item.getStatus()))
                .filter(item -> keyword.isEmpty()
                        || item.getRequestCode().toLowerCase(Locale.ROOT).contains(keyword)
                        || item.getRoomCode().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
    }

    public int createRequest(int tenantId, int roomId, LocalDate moveInDate,
            int months, int numPeople, String note) throws SQLException {
        if (tenantId <= 0 || roomId <= 0) {
            throw new IllegalArgumentException("Người thuê hoặc phòng không hợp lệ.");
        }
        if (moveInDate == null || moveInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày dọn vào phải từ hôm nay trở đi.");
        }
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException("Số tháng thuê phải từ 1 đến 24.");
        }
        if (numPeople < 1) {
            throw new IllegalArgumentException("Số người phải ít nhất là 1.");
        }
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
        try (RentalDAO dao = new RentalDAO()) {
            Connection connection = dao.getConnection();
            if (connection == null) {
                throw new SQLException("Database connection is unavailable.");
            }
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                // Khóa người thuê trước khi kiểm tra để hai lần bấm không cùng tạo PENDING.
                if (!dao.lockActiveTenant(tenantId)) {
                    throw new IllegalArgumentException("Tài khoản người thuê không còn hoạt động.");
                }
                RentalRoom room = dao.findRentalRoom(roomId);
                if (room == null) {
                    throw new IllegalArgumentException("Phòng không tồn tại.");
                }
                if (room.isOccupied()) {
                    throw new IllegalArgumentException("Phòng đã có người thuê.");
                }
                if (!"ACTIVE".equals(room.getStatus())) {
                    throw new IllegalArgumentException("Phòng hiện không mở cho thuê.");
                }
                if (numPeople > room.getCapacity()) {
                    throw new IllegalArgumentException("Số người vượt quá sức chứa của phòng ("
                            + room.getCapacity() + " người).");
                }
                if (dao.hasActiveContract(tenantId)) {
                    throw new IllegalArgumentException("Bạn đang có hợp đồng thuê còn hiệu lực.");
                }
                if (dao.hasPendingRequest(tenantId)) {
                    throw new IllegalArgumentException("Bạn đã có một yêu cầu đang chờ duyệt.");
                }
                RentalRequest rentalRequest = new RentalRequest(0, tenantId, roomId,
                        moveInDate, months, numPeople, note == null ? null : note.trim(),
                        "PENDING", null, null, null);
                int requestId = dao.insertRequest(rentalRequest);
                connection.commit();
                return requestId;
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                throw ex;
            }
        }
    }
}
