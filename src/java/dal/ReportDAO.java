package dal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import model.OccupancyStats;

public class ReportDAO extends DBContext implements AutoCloseable {

    /** Tổng tiền hóa đơn PAID theo tháng của paidAt; key = tháng (1–12). */
    public Map<Integer, Totals> findPaidByMonth(LocalDate from, LocalDate to) throws SQLException {
        // Khoảng [from, to): paidAt là DATETIME2 nên không dùng BETWEEN để khỏi sót giờ của ngày cuối.
        String sql = """
                SELECT MONTH(paidAt) AS [month], SUM(total) AS amount, COUNT(*) AS invoiceCount
                FROM Invoice
                WHERE status = 'PAID' AND paidAt >= ? AND paidAt < ?
                GROUP BY MONTH(paidAt)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            return readTotals(ps);
        }
    }

    /** Tổng tiền hóa đơn UNPAID đã quá hạn trước {@code today}, gom theo tháng của dueDate. */
    public Map<Integer, Totals> findOverdueByMonth(LocalDate from, LocalDate to, LocalDate today)
            throws SQLException {
        String sql = """
                SELECT MONTH(dueDate) AS [month], SUM(total) AS amount, COUNT(*) AS invoiceCount
                FROM Invoice
                WHERE status = 'UNPAID' AND dueDate < ? AND dueDate >= ? AND dueDate < ?
                GROUP BY MONTH(dueDate)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, today);
            ps.setObject(2, from);
            ps.setObject(3, to);
            return readTotals(ps);
        }
    }

    /** Phòng đang mở cho thuê (Room ACTIVE) và số phòng trong đó có hợp đồng ACTIVE. */
    public OccupancyStats findOccupancy() throws SQLException {
        // UX_Contract_ActiveRoom bảo đảm mỗi phòng có tối đa 1 hợp đồng ACTIVE nên LEFT JOIN không nhân dòng.
        String sql = """
                SELECT COUNT(*) AS openRooms,
                       COUNT(c.contractId) AS rentedRooms
                FROM Room r
                LEFT JOIN Contract c ON c.roomId = r.roomId AND c.status = 'ACTIVE'
                WHERE r.status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new OccupancyStats(rs.getInt("openRooms"), rs.getInt("rentedRooms"));
        }
    }

    private Map<Integer, Totals> readTotals(PreparedStatement ps) throws SQLException {
        Map<Integer, Totals> totals = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                totals.put(rs.getInt("month"),
                        new Totals(rs.getBigDecimal("amount"), rs.getInt("invoiceCount")));
            }
        }
        return totals;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) { connection.close(); }
    }

    public record Totals(BigDecimal amount, int count) { }
}
