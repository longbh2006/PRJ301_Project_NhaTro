package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Invoice;
import model.UtilityPrice;

/**
 * Truy vấn hoá đơn phía người thuê (UC06 · NT-38, Q15 trong queries-mau.sql).
 * Invoice không lưu tenantId nên mọi câu đều đi Invoice → Contract và lọc
 * Contract.tenantId.
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
                if (!rs.next()) {
                    return null;
                }
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
        }
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
