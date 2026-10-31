package service;

import dal.MaintenanceDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import model.MaintenanceRequest;

public class MaintenanceService {

    // SỬA LỖI 3: Khai báo throws SQLException để nhường xử lý cho Controller
    public void createIssue(int tenantId, String title, String description) throws SQLException {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề sự cố không được để trống.");
        }
        String cleanTitle = title.trim();
        if (cleanTitle.length() > 150) {
            throw new IllegalArgumentException("Tiêu đề sự cố không được vượt quá 150 ký tự.");
        }

        String cleanDesc = (description != null) ? description.trim() : null;
        if (cleanDesc != null && cleanDesc.length() > 1000) {
            throw new IllegalArgumentException("Mô tả sự cố không được vượt quá 1000 ký tự.");
        }

        try (MaintenanceDAO dao = new MaintenanceDAO()) {
            // SỬA LỖI 5: Kiểm tra Connection có được mở thành công không
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }

            Optional<Integer> contractIdOpt = dao.getActiveContractId(tenantId);
            if (contractIdOpt.isEmpty()) {
                throw new IllegalArgumentException("Bạn không có hợp đồng đang thuê hợp lệ để báo sự cố.");
            }

            int contractId = contractIdOpt.get();
            boolean success = dao.insertMaintenanceRequest(tenantId, contractId, cleanTitle, cleanDesc);
            if (!success) {
                throw new IllegalArgumentException("Không thể ghi nhận sự cố. Hợp đồng có thể vừa hết hạn.");
            }
        }
    }

    public List<MaintenanceRequest> getIssuesByTenant(int tenantId) throws SQLException {
        try (MaintenanceDAO dao = new MaintenanceDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            return dao.findRequestsByTenant(tenantId);
        }
    }

    public boolean hasActiveContract(int tenantId) throws SQLException {
        try (MaintenanceDAO dao = new MaintenanceDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            return dao.getActiveContractId(tenantId).isPresent();
        }
    }

    // ================= UC12: QUẢN LÝ XỬ LÝ SỰ CỐ =================
    private boolean isValidStatus(String status) {
        return "OPEN".equals(status) || "IN_PROGRESS".equals(status) || "DONE".equals(status);
    }

    public List<MaintenanceRequest> getIssuesForManager(String status, Integer roomId) throws SQLException {
        if (status != null && !status.trim().isEmpty() && !isValidStatus(status)) {
            throw new IllegalArgumentException("Trạng thái sự cố không hợp lệ.");
        }
        if (roomId != null && roomId <= 0) {
            throw new IllegalArgumentException("Mã phòng không hợp lệ.");
        }

        try (MaintenanceDAO dao = new MaintenanceDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            return dao.findRequestsByManager(status, roomId);
        }
    }

    public void updateIssueStatus(int ticketId, String currentStatus, String newStatus, String managerNote) throws SQLException, service.RentalService.BusinessException {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("ID sự cố không hợp lệ.");
        }

        if (!isValidStatus(currentStatus) || !isValidStatus(newStatus)) {
            throw new IllegalArgumentException("Trạng thái sự cố không hợp lệ.");
        }

        if ("OPEN".equals(currentStatus) && !"IN_PROGRESS".equals(newStatus)) {
            throw new IllegalArgumentException("Sự cố đang ở trạng thái 'Mới tiếp nhận' chỉ có thể chuyển sang 'Đang xử lý'.");
        }
        if ("IN_PROGRESS".equals(currentStatus) && !"DONE".equals(newStatus)) {
            throw new IllegalArgumentException("Sự cố đang 'Đang xử lý' chỉ có thể chuyển sang trạng thái 'Đã hoàn thành'.");
        }
        if ("DONE".equals(currentStatus)) {
            throw new IllegalArgumentException("Sự cố đã hoàn tất ('Đã hoàn thành') không thể thay đổi trạng thái nữa.");
        }

        String cleanNote = (managerNote != null) ? managerNote.trim() : null;

        // Bắt buộc phải nhập ghi chú quản lý khi hoàn tất sự cố.
        if ("DONE".equals(newStatus) && (cleanNote == null || cleanNote.isEmpty())) {
            throw new IllegalArgumentException("Bắt buộc phải nhập ghi chú quản lý khi hoàn tất sự cố.");
        }

        if (cleanNote != null && cleanNote.length() > 500) {
            throw new IllegalArgumentException("Ghi chú của quản lý không được vượt quá 500 ký tự.");
        }

        try (MaintenanceDAO dao = new MaintenanceDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Không thể kết nối đến cơ sở dữ liệu.");
            }
            boolean updated = dao.updateIssueStatus(ticketId, currentStatus, newStatus, cleanNote);
            if (!updated) {
                throw new service.RentalService.BusinessException("Cập nhật thất bại. Dữ liệu sự cố có thể đã được thay đổi bởi người quản lý khác. Vui lòng tải lại trang.");
            }
        }
    }
}
