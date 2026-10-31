package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.MaintenanceRequest;

/**
 * Giữ connection kế thừa đến khi đóng DAO để có thể gọi nhiều truy vấn liên tiếp.
 */
public class MaintenanceDAO extends DBContext implements AutoCloseable {

    /**
     * 1. Lấy id hợp đồng ACTIVE của một tenant.
     * Dùng SQL từ Q06a (queries-mau.sql:159).
     */
    public Optional<Integer> getActiveContractId(int tenantId) throws SQLException {
        String sql = "SELECT contractId FROM Contract WHERE tenantId = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("contractId"));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * 2. Ghi sự cố. Dùng INSERT...SELECT (Q06b). 
     * - Bỏ cột status để CSDL tự dùng DEFAULT là 'OPEN' (tránh lỗi CHECK constraint).
     * - SỬA LỖI 7: Bổ sung lọc tenantId = ? để chặn lỗ hổng IDOR (ghi hộ sự cố vào HỢP ĐỒNG người khác).
     */
    public boolean insertMaintenanceRequest(int tenantId, int contractId, String title, String description) throws SQLException {
        String sql = "INSERT INTO MaintenanceRequest (contractId, title, description) SELECT contractId, ?, ? FROM Contract WHERE contractId = ? AND tenantId = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);

            if (description == null || description.trim().isEmpty()) {
                ps.setNull(2, Types.NVARCHAR); 
            } else {
                ps.setString(2, description.trim());
            }

            ps.setInt(3, contractId);
            ps.setInt(4, tenantId); 
            
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * 3. Liệt kê sự cố của người thuê. (Q06c)
     * Trả về danh sách sắp xếp mới nhất trước, kèm tên mã phòng.
     */
    public List<MaintenanceRequest> findRequestsByTenant(int tenantId) throws SQLException {
        List<MaintenanceRequest> list = new ArrayList<>();
        String sql = "SELECT m.ticketId, m.contractId, m.title, m.description, m.status, m.managerNote, m.createdAt, m.resolvedAt, r.roomCode FROM MaintenanceRequest m JOIN Contract c ON m.contractId = c.contractId JOIN Room r ON c.roomId = r.roomId WHERE c.tenantId = ? ORDER BY m.createdAt DESC, m.ticketId DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MaintenanceRequest mr = new MaintenanceRequest();
                    mr.setTicketId(rs.getInt("ticketId"));
                    mr.setContractId(rs.getInt("contractId"));
                    mr.setTitle(rs.getString("title"));
                    mr.setDescription(rs.getString("description"));
                    mr.setStatus(rs.getString("status"));
                    mr.setManagerNote(rs.getString("managerNote"));
                    
                    mr.setCreatedAt(rs.getObject("createdAt", LocalDateTime.class));
                    mr.setResolvedAt(rs.getObject("resolvedAt", LocalDateTime.class));
                    
                    mr.setRoomCode(rs.getString("roomCode"));
                    
                    list.add(mr);
                }
            }
        }
        return list;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}