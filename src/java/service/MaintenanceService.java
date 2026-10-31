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
}