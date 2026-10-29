package service;

import dal.RentalDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Contract;
import model.RentalRequest;

public class RentalService {

    public static class BusinessException extends Exception {
        private static final long serialVersionUID = 1L;

        public BusinessException(String message) {
            super(message);
        }

        public BusinessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public List<RentalRequest> findPendingRequests(Integer roomId) throws SQLException {
        if (roomId != null && roomId <= 0) {
            throw new IllegalArgumentException("Mã phòng phải là số nguyên dương.");
        }
        try (RentalDAO dao = openDAO()) {
            return dao.findPendingRequests(roomId);
        }
    }

    public int approve(int requestId, int managerId, BigDecimal deposit,
            int electricIndex, int waterIndex) throws SQLException, BusinessException {
        validateIds(requestId, managerId);
        if (deposit == null || deposit.signum() < 0
                || deposit.stripTrailingZeros().scale() > 0
                || deposit.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new IllegalArgumentException("Tiền cọc phải là số nguyên từ 0 đến 999.999.999.999 đồng.");
        }
        if (electricIndex < 0 || waterIndex < 0) {
            throw new IllegalArgumentException("Chỉ số điện và nước phải là số nguyên không âm.");
        }
        try (RentalDAO dao = openDAO()) {
            Connection con = dao.getConnection();
            con.setAutoCommit(false);
            try {
                requireManager(dao, managerId);
                RentalRequest request = requireRequest(dao, requestId);
                RentalDAO.RoomTerms room = dao.lockRoomForApproval(request.getRoomId());
                // Đọc lại sau khi lấy khóa: một transaction khác có thể vừa xử lý request.
                request = requireRequest(dao, requestId);
                if (dao.hasActiveContractForRoom(request.getRoomId())) {
                    throw new BusinessException("Phòng vừa được người khác thuê.");
                }
                requirePending(request);
                if (room == null || !"ACTIVE".equals(room.status())) {
                    throw new BusinessException("Phòng hiện không mở cho thuê.");
                }
                if (request.getNumPeople() < 1 || request.getNumPeople() > room.capacity()) {
                    throw new BusinessException("Số người vượt sức chứa của phòng.");
                }
                if (request.getMoveInDate() == null || request.getMonths() < 1 || request.getMonths() > 24) {
                    throw new BusinessException("Ngày nhận phòng hoặc thời hạn thuê không hợp lệ.");
                }
                if (dao.decidePendingRequest(requestId, managerId, "APPROVED") != 1) {
                    throw new BusinessException("Yêu cầu đã được xử lý. Vui lòng tải lại danh sách.");
                }
                Contract contract = new Contract(0, requestId, request.getTenantId(), request.getRoomId(),
                        request.getMoveInDate(), request.getMoveInDate().plusMonths(request.getMonths()),
                        room.monthlyPrice(), deposit, request.getNumPeople(),
                        electricIndex, waterIndex, "ACTIVE", null);
                int contractId = dao.insertContract(con, contract);
                dao.rejectOtherPendingRequests(request.getRoomId(), requestId, managerId);
                con.commit();
                return contractId;
            } catch (SQLException ex) {
                rollback(con, ex);
                throwTranslated(ex);
                throw ex;
            } catch (BusinessException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
            // DAO đóng connection của giao dịch; không đổi autoCommit khi rollback thất bại.
        }
    }

    public void reject(int requestId, int managerId) throws SQLException, BusinessException {
        validateIds(requestId, managerId);
        try (RentalDAO dao = openDAO()) {
            Connection con = dao.getConnection();
            con.setAutoCommit(false);
            try {
                requireManager(dao, managerId);
                requirePending(requireRequest(dao, requestId));
                if (dao.decidePendingRequest(requestId, managerId, "REJECTED") != 1) {
                    throw new BusinessException("Yêu cầu đã được xử lý. Vui lòng tải lại danh sách.");
                }
                con.commit();
            } catch (SQLException | BusinessException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    protected RentalDAO openDAO() throws SQLException {
        RentalDAO dao = new RentalDAO();
        if (dao.getConnection() == null || dao.getConnection().isClosed()) {
            dao.close();
            throw new SQLException("Không thể kết nối CSDL.");
        }
        return dao;
    }

    private void requireManager(RentalDAO dao, int managerId) throws SQLException, BusinessException {
        if (!dao.isActiveManager(managerId)) {
            throw new BusinessException("Tài khoản quản lý không còn hoạt động.");
        }
    }

    private RentalRequest requireRequest(RentalDAO dao, int requestId) throws SQLException, BusinessException {
        RentalRequest request = dao.findById(requestId);
        if (request == null) {
            throw new BusinessException("Không tìm thấy yêu cầu thuê.");
        }
        return request;
    }

    private void requirePending(RentalRequest request) throws BusinessException {
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("Yêu cầu đã được xử lý. Vui lòng tải lại danh sách.");
        }
    }

    private void validateIds(int requestId, int managerId) {
        if (requestId <= 0 || managerId <= 0) {
            throw new IllegalArgumentException("Mã yêu cầu hoặc tài khoản quản lý không hợp lệ.");
        }
    }

    private void rollback(Connection con, Exception failure) throws SQLException {
        try {
            con.rollback();
        } catch (SQLException rollbackFailure) {
            rollbackFailure.addSuppressed(failure);
            throw rollbackFailure;
        }
    }

    private void throwTranslated(SQLException failure) throws BusinessException {
        for (SQLException ex = failure; ex != null; ex = ex.getNextException()) {
            if (ex.getErrorCode() == 2601 || ex.getErrorCode() == 2627) {
                if (ex.getMessage() != null && ex.getMessage().contains("UX_Contract_ActiveTenant")) {
                    throw new BusinessException("Người thuê đã có hợp đồng đang hiệu lực.", failure);
                }
                throw new BusinessException("Phòng vừa được người khác thuê.", failure);
            }
            if (ex.getErrorCode() == 1205) {
                throw new BusinessException("Yêu cầu đang được xử lý đồng thời. Vui lòng tải lại và thử lại.", failure);
            }
        }
    }
}
