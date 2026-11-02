package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import model.Contract;
import model.Invoice;
import model.MeterReading;
import model.MeterRow;
import model.UtilityPrice;

/**
 *
 * @author Lenovo
 */
public class BillingDAO extends DBContext implements AutoCloseable {

    public MeterReading findLastReading(int roomId) throws SQLException {
        String sql = """
                SELECT TOP (1) * FROM MeterReading
                WHERE roomId = ?
                ORDER BY [year] DESC, [month] DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapReading(rs) : null;
            }
        }
    }

    /**
     * Mọi hợp đồng ACTIVE đã bắt đầu trước hoặc trong kỳ, kèm chỉ số tháng trước,
     * chỉ số của kỳ và hoá đơn của kỳ (NULL khi chưa có).
     */
    public List<MeterRow> findMeterRows(YearMonth period) throws SQLException {
        String sql = """
                SELECT c.contractId, c.requestId, c.tenantId, c.roomId, c.startDate, c.endDate,
                       c.monthlyRent, c.deposit, c.numPeople, c.startElectricIndex, c.startWaterIndex,
                       c.status, c.endedAt, r.roomCode, a.fullName AS tenantName,
                       prev.electricIndex AS prevElectric, prev.waterIndex AS prevWater,
                       cur.electricIndex AS curElectric, cur.waterIndex AS curWater,
                       i.invoiceId
                FROM Contract c
                JOIN Room r    ON r.roomId = c.roomId
                JOIN Account a ON a.accountId = c.tenantId
                LEFT JOIN MeterReading prev ON prev.roomId = c.roomId AND prev.[year] = ? AND prev.[month] = ?
                LEFT JOIN MeterReading cur  ON cur.roomId = c.roomId AND cur.[year] = ? AND cur.[month] = ?
                LEFT JOIN Invoice i         ON i.contractId = c.contractId AND i.[year] = ? AND i.[month] = ?
                WHERE c.status = 'ACTIVE' AND c.startDate <= ?
                ORDER BY r.roomCode
                """;
        YearMonth previousPeriod = period.minusMonths(1);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, previousPeriod.getYear());
            ps.setInt(2, previousPeriod.getMonthValue());
            ps.setInt(3, period.getYear());
            ps.setInt(4, period.getMonthValue());
            ps.setInt(5, period.getYear());
            ps.setInt(6, period.getMonthValue());
            ps.setObject(7, period.atEndOfMonth());
            try (ResultSet rs = ps.executeQuery()) {
                List<MeterRow> rows = new ArrayList<>();
                while (rs.next()) {
                    Contract contract = new Contract(rs.getInt("contractId"), rs.getInt("requestId"),
                            rs.getInt("tenantId"), rs.getInt("roomId"),
                            rs.getObject("startDate", LocalDate.class), rs.getObject("endDate", LocalDate.class),
                            rs.getBigDecimal("monthlyRent"), rs.getBigDecimal("deposit"), rs.getInt("numPeople"),
                            rs.getInt("startElectricIndex"), rs.getInt("startWaterIndex"),
                            rs.getString("status"), rs.getObject("endedAt", LocalDateTime.class));
                    contract.setRoomCode(rs.getString("roomCode"));
                    contract.setTenantName(rs.getNString("tenantName"));
                    MeterReading previous = readingOrNull(rs, "prevElectric", "prevWater",
                            contract.getRoomId(), previousPeriod);
                    MeterReading current = readingOrNull(rs, "curElectric", "curWater",
                            contract.getRoomId(), period);
                    rows.add(new MeterRow(contract, previous, current, rs.getObject("invoiceId", Integer.class)));
                }
                return rows;
            }
        }
    }

    private MeterReading readingOrNull(ResultSet rs, String electricColumn, String waterColumn,
            int roomId, YearMonth period) throws SQLException {
        Integer electric = rs.getObject(electricColumn, Integer.class);
        Integer water = rs.getObject(waterColumn, Integer.class);
        if (electric == null || water == null) {
            return null;
        }
        MeterReading reading = new MeterReading();
        reading.setRoomId(roomId);
        reading.setYear(period.getYear());
        reading.setMonth(period.getMonthValue());
        reading.setElectricIndex(electric);
        reading.setWaterIndex(water);
        return reading;
    }

    public int insertReading(MeterReading reading) throws SQLException {
        String sql = """
                INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reading.getRoomId());
            ps.setInt(2, reading.getYear());
            ps.setInt(3, reading.getMonth());
            ps.setInt(4, reading.getElectricIndex());
            ps.setInt(5, reading.getWaterIndex());
            return ps.executeUpdate();
        }
    }

    public int insertInvoice(Invoice invoice) throws SQLException {
        String sql = """
                INSERT INTO Invoice (contractId, priceId, [year], [month], roomFee,
                                     electricUsed, electricFee, waterUsed, waterFee,
                                     serviceFee, total, status, dueDate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoice.getContractId());
            ps.setInt(2, invoice.getPriceId());
            ps.setInt(3, invoice.getYear());
            ps.setInt(4, invoice.getMonth());
            ps.setBigDecimal(5, invoice.getRoomFee());
            ps.setInt(6, invoice.getElectricUsed());
            ps.setBigDecimal(7, invoice.getElectricFee());
            ps.setInt(8, invoice.getWaterUsed());
            ps.setBigDecimal(9, invoice.getWaterFee());
            ps.setBigDecimal(10, invoice.getServiceFee());
            ps.setBigDecimal(11, invoice.getTotal());
            ps.setString(12, invoice.getStatus());
            ps.setObject(13, invoice.getDueDate());
            return ps.executeUpdate();
        }
    }

    public List<Invoice> findInvoicesByPeriod(YearMonth period) throws SQLException {
        String sql = """
                SELECT i.*, r.roomCode, a.fullName AS tenantName
                FROM Invoice i
                JOIN Contract c ON c.contractId = i.contractId
                JOIN Room r     ON r.roomId = c.roomId
                JOIN Account a  ON a.accountId = c.tenantId
                WHERE i.[year] = ? AND i.[month] = ?
                ORDER BY r.roomCode
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, period.getYear());
            ps.setInt(2, period.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();
                while (rs.next()) {
                    Invoice invoice = mapInvoice(rs);
                    invoice.setRoomCode(rs.getString("roomCode"));
                    invoice.setTenantName(rs.getNString("tenantName"));
                    invoices.add(invoice);
                }
                return invoices;
            }
        }
    }

    public List<Invoice> findInvoicesByTenant(int tenantId) {
        String sql = """
                SELECT i.* FROM Invoice i
                JOIN Contract c ON i.contractId = c.contractId
                WHERE c.tenantId = ?
                ORDER BY i.invoiceId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapInvoice(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn hoá đơn theo tenant", e);
        }
    }

    public Invoice findInvoiceById(int invoiceId) {
        String sql = "SELECT * FROM Invoice WHERE invoiceId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapInvoice(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn hoá đơn theo id", e);
        }
    }

    public UtilityPrice currentPrice() {
        try {
            return findPriceOn(LocalDate.now());
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn giá hiện tại", e);
        }
    }

    /** Bảng giá có hiệu lực tại một ngày: effectiveFrom ≤ ngày đó, mốc mới nhất. */
    public UtilityPrice findPriceOn(LocalDate date) throws SQLException {
        String sql = """
                SELECT TOP (1) * FROM UtilityPrice
                WHERE effectiveFrom <= ?
                ORDER BY effectiveFrom DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUtilityPrice(rs) : null;
            }
        }
    }

    private MeterReading mapReading(ResultSet rs) throws SQLException {
        MeterReading m = new MeterReading();
        m.setReadingId(rs.getInt("readingId"));
        m.setRoomId(rs.getInt("roomId"));
        m.setYear(rs.getInt("year"));
        m.setMonth(rs.getInt("month"));
        m.setElectricIndex(rs.getInt("electricIndex"));
        m.setWaterIndex(rs.getInt("waterIndex"));
        m.setRecordedAt(rs.getObject("recordedAt", LocalDateTime.class));
        return m;
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
        return inv;
    }

    private UtilityPrice mapUtilityPrice(ResultSet rs) throws SQLException {
        UtilityPrice p = new UtilityPrice();
        p.setPriceId(rs.getInt("priceId"));
        p.setElectricPrice(rs.getBigDecimal("electricPrice"));
        p.setWaterPrice(rs.getBigDecimal("waterPrice"));
        p.setServiceFeePerPerson(rs.getBigDecimal("serviceFeePerPerson"));
        p.setEffectiveFrom(rs.getObject("effectiveFrom", LocalDate.class));
        p.setCreatedAt(rs.getObject("createdAt", LocalDateTime.class));
        return p;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
