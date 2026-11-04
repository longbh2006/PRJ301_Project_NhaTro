package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Invoice;
import model.InvoiceSummary;
import model.UtilityPrice;

/**
 * Truy vấn hoá đơn phía người thuê (UC06 · NT-38, Q15 trong queries-mau.sql)
 * và phía quản lý (NT-39). Invoice không lưu tenantId nên mọi câu đều đi
 * Invoice → Contract; câu phía người thuê luôn lọc Contract.tenantId.
 */
public class InvoiceDAO extends DBContext implements AutoCloseable {

    public long countByTenant(int tenantId) throws SQLException {
        String sql = """
                SELECT COUNT_BIG(*)
                FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
                WHERE c.tenantId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Q15a: hoá đơn của tôi, kỳ mới nhất trước.
     */
    public List<Invoice> findPageByTenant(int tenantId, long offset, int pageSize) throws SQLException {
        String sql = """
                SELECT i.*, r.roomCode,
                       CASE WHEN i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)
                            THEN 1 ELSE 0 END AS isOverdue
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r ON r.roomId = c.roomId
                WHERE c.tenantId = ?
                ORDER BY i.[year] DESC, i.[month] DESC, i.invoiceId DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setLong(2, offset);
            ps.setInt(3, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();
                while (rs.next()) {
                    invoices.add(mapInvoice(rs));
                }
                return invoices;
            }
        }
    }

    /**
     * Q15b: chi tiết một hoá đơn kèm đơn giá đã dùng để tính.
     * Điều kiện c.tenantId = ? giữ ở tầng DAO để hoá đơn người khác trả về null.
     * LEFT JOIN MeterReading lấy chỉ số cuối kỳ của phòng; thiếu dòng thì trang
     * chi tiết bỏ phần chỉ số cũ → mới.
     */
    public Invoice findByIdForTenant(int invoiceId, int tenantId) throws SQLException {
        String sql = """
                SELECT i.*, r.roomCode, c.numPeople,
                       CASE WHEN i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)
                            THEN 1 ELSE 0 END AS isOverdue,
                       p.electricPrice, p.waterPrice, p.serviceFeePerPerson, p.effectiveFrom,
                       m.electricIndex, m.waterIndex
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r ON r.roomId = c.roomId
                JOIN UtilityPrice p ON p.priceId = i.priceId
                LEFT JOIN MeterReading m ON m.roomId = c.roomId
                      AND m.[year] = i.[year] AND m.[month] = i.[month]
                WHERE i.invoiceId = ? AND c.tenantId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDetail(rs) : null;
            }
        }
    }

    /**
     * NT-39: tổng hợp theo bộ lọc của quản lý. Chuỗi rỗng / null nghĩa là không lọc;
     * overdueOnly chỉ giữ hoá đơn UNPAID đã qua hạn trả.
     */
    public InvoiceSummary summarizeForManager(Integer year, Integer month, String status,
            boolean overdueOnly, String roomCode, String tenantName) throws SQLException {
        String sql = """
                SELECT COUNT_BIG(*) AS invoiceCount,
                       COALESCE(SUM(i.total), 0) AS totalAmount,
                       COALESCE(SUM(CASE WHEN i.status = 'UNPAID' THEN i.total END), 0) AS unpaidAmount
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                WHERE (? IS NULL OR i.[year] = ?)
                  AND (? IS NULL OR i.[month] = ?)
                  AND (? = '' OR i.status = ?)
                  AND (? = 0 OR (i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)))
                  AND r.roomCode LIKE ? ESCAPE '\\'
                  AND a.fullName LIKE ? ESCAPE '\\'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindManagerFilters(ps, year, month, status, overdueOnly, roomCode, tenantName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new InvoiceSummary(rs.getLong("invoiceCount"),
                        rs.getBigDecimal("totalAmount"), rs.getBigDecimal("unpaidAmount"));
            }
        }
    }

    /**
     * NT-39: một trang hoá đơn mọi phòng, kỳ mới nhất trước rồi theo mã phòng.
     */
    public List<Invoice> findPageForManager(Integer year, Integer month, String status,
            boolean overdueOnly, String roomCode, String tenantName, long offset, int pageSize)
            throws SQLException {
        String sql = """
                SELECT i.*, r.roomCode, a.fullName AS tenantName,
                       CASE WHEN i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)
                            THEN 1 ELSE 0 END AS isOverdue
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                WHERE (? IS NULL OR i.[year] = ?)
                  AND (? IS NULL OR i.[month] = ?)
                  AND (? = '' OR i.status = ?)
                  AND (? = 0 OR (i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)))
                  AND r.roomCode LIKE ? ESCAPE '\\'
                  AND a.fullName LIKE ? ESCAPE '\\'
                ORDER BY i.[year] DESC, i.[month] DESC, r.roomCode, i.invoiceId DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindManagerFilters(ps, year, month, status, overdueOnly, roomCode, tenantName);
            ps.setLong(10, offset);
            ps.setInt(11, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();
                while (rs.next()) {
                    Invoice invoice = mapInvoice(rs);
                    invoice.setTenantName(rs.getNString("tenantName"));
                    invoices.add(invoice);
                }
                return invoices;
            }
        }
    }

    /**
     * NT-39: chi tiết hoá đơn cho quản lý — cùng dữ liệu như Q15b, thêm tên người thuê,
     * không giới hạn theo người thuê vì quản lý xem được mọi phòng.
     */
    public Invoice findByIdForManager(int invoiceId) throws SQLException {
        String sql = """
                SELECT i.*, r.roomCode, c.numPeople, a.fullName AS tenantName,
                       CASE WHEN i.status = 'UNPAID' AND i.dueDate < CAST(SYSDATETIME() AS DATE)
                            THEN 1 ELSE 0 END AS isOverdue,
                       p.electricPrice, p.waterPrice, p.serviceFeePerPerson, p.effectiveFrom,
                       m.electricIndex, m.waterIndex
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                JOIN UtilityPrice p ON p.priceId = i.priceId
                LEFT JOIN MeterReading m ON m.roomId = c.roomId
                      AND m.[year] = i.[year] AND m.[month] = i.[month]
                WHERE i.invoiceId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Invoice invoice = mapDetail(rs);
                invoice.setTenantName(rs.getNString("tenantName"));
                return invoice;
            }
        }
    }

    private void bindManagerFilters(PreparedStatement ps, Integer year, Integer month, String status,
            boolean overdueOnly, String roomCode, String tenantName) throws SQLException {
        ps.setObject(1, year, java.sql.Types.INTEGER);
        ps.setObject(2, year, java.sql.Types.INTEGER);
        ps.setObject(3, month, java.sql.Types.INTEGER);
        ps.setObject(4, month, java.sql.Types.INTEGER);
        ps.setString(5, status);
        ps.setString(6, status);
        ps.setInt(7, overdueOnly ? 1 : 0);
        ps.setString(8, containsPattern(roomCode));
        ps.setNString(9, containsPattern(tenantName));
    }

    // Ký tự LIKE do người dùng nhập phải được tìm như văn bản thông thường.
    private String containsPattern(String value) {
        String escaped = value.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[");
        return "%".concat(escaped).concat("%");
    }

    private Invoice mapDetail(ResultSet rs) throws SQLException {
        Invoice invoice = mapInvoice(rs);
        invoice.setNumPeople(rs.getInt("numPeople"));
        invoice.setElectricIndex(rs.getObject("electricIndex", Integer.class));
        invoice.setWaterIndex(rs.getObject("waterIndex", Integer.class));

        UtilityPrice price = new UtilityPrice();
        price.setPriceId(invoice.getPriceId());
        price.setElectricPrice(rs.getBigDecimal("electricPrice"));
        price.setWaterPrice(rs.getBigDecimal("waterPrice"));
        price.setServiceFeePerPerson(rs.getBigDecimal("serviceFeePerPerson"));
        price.setEffectiveFrom(rs.getObject("effectiveFrom", LocalDate.class));
        invoice.setUtilityPrice(price);
        return invoice;
    }

    /**
     * NT-42 · OwnershipFilter: hoá đơn có thuộc hợp đồng của người thuê này không.
     */
    public boolean isOwnedByTenant(int invoiceId, int tenantId) throws SQLException {
        String sql = """
                SELECT 1
                FROM Invoice i JOIN Contract c ON c.contractId = i.contractId
                WHERE i.invoiceId = ? AND c.tenantId = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getInt("invoiceId"));
        inv.setContractId(rs.getInt("contractId"));
        inv.setPriceId(rs.getInt("priceId"));
        inv.setYear(rs.getInt("year"));
        inv.setMonth(rs.getInt("month"));
        inv.setRoomFee(rs.getBigDecimal("roomFee"));
        inv.setElectricUsed(rs.getInt("electricUsed"));
        inv.setElectricFee(rs.getBigDecimal("electricFee"));
        inv.setWaterUsed(rs.getInt("waterUsed"));
        inv.setWaterFee(rs.getBigDecimal("waterFee"));
        inv.setServiceFee(rs.getBigDecimal("serviceFee"));
        inv.setTotal(rs.getBigDecimal("total"));
        inv.setStatus(rs.getString("status"));
        inv.setDueDate(rs.getObject("dueDate", LocalDate.class));
        inv.setCreatedAt(rs.getObject("createdAt", LocalDateTime.class));
        inv.setPaidAt(rs.getObject("paidAt", LocalDateTime.class));
        inv.setRoomCode(rs.getString("roomCode"));
        inv.setOverdue(rs.getInt("isOverdue") == 1);
        return inv;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
