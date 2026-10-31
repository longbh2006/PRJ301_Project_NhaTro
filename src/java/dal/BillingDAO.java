/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dal;

import model.MeterReading;
import model.Invoice;
import model.UtilityPrice;
import model.Contract;
import java.util.List;
import java.util.ArrayList;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author Lenovo
 */
public class BillingDAO extends DBContext {

    public MeterReading findLastReading(int roomId) throws SQLException {
        String sql = "SELECT TOP (1) *"
                + " FROM MeterReading"
                + " WHERE roomId = ?"
                + " ORDER BY [year] DESC, [month] DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
            }
        }
        return null;
    }

    public MeterReading findReadingBefore(int roomId, int year, int month) throws SQLException {
        // 3. SQL: lấy ghi có kỳ trước (year, month) đang chọn
        String sql
                = "SELECT TOP (1) * "
                + "FROM MeterReading "
                + "WHERE roomId = ? "
                + "  AND ( [year] < ? "
                + "       OR ( [year] = ? AND [month] < ? ) ) "
                + "ORDER BY [year] DESC, [month] DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            // 4. Đặt tham số đúng thứ tự: roomId, year, year, month
            ps.setInt(1, roomId);
            ps.setInt(2, year);
            ps.setInt(3, year);
            ps.setInt(4, month);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
            }
        }
        return null;   // không tìm thấy ghi trước kỳ
    }

    public int insertReading(MeterReading reading) throws SQLException {
        String sql = "INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reading.getRoomId());
            ps.setInt(2, reading.getYear());
            ps.setInt(3, reading.getMonth());
            ps.setInt(4, reading.getElectricIndex());
            ps.setInt(5, reading.getWaterIndex());
            return ps.executeUpdate();
        }
    }
    
    public int insertReading(Connection con, MeterReading reading) throws SQLException {
    String sql = "INSERT INTO MeterReading (roomId, [year], [month], electricIndex, waterIndex) " +
                 "VALUES (?, ?, ?, ?, ?)";
    // Sử dụng con được truyền vào, không đóng con ở đây
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, reading.getRoomId());
        ps.setInt(2, reading.getYear());
        ps.setInt(3, reading.getMonth());
        ps.setInt(4, reading.getElectricIndex());
        ps.setInt(5, reading.getWaterIndex());
        return ps.executeUpdate();
    }
}

    public int insertInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO Invoice "
                + "(contractId, priceId, [year], [month], roomFee, "
                + " electricUsed, electricFee, waterUsed, waterFee, "
                + " serviceFee, total, dueDate) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            ps.setObject(12, invoice.getDueDate());
            return ps.executeUpdate();
        }
    }

    public List<Invoice> findInvoicesByTenant(int tenantId) {
        List<Invoice> list = new ArrayList<>();

        String sql
                = "SELECT i.* "
                + "FROM Invoice i "
                + "JOIN Contract c ON i.contractId = c.contractId "
                + "WHERE c.tenantId = ? "
                + "ORDER BY i.invoiceId DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, tenantId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapInvoice(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn hoá đơn theo tenant", e);
        }

        return list;
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

    public Invoice findInvoiceById(int invoiceId) {
        String sql = "SELECT * FROM Invoice WHERE invoiceId = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapInvoice(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn hoá đơn theo id", e);
        }

        return null;
    }

    public UtilityPrice currentPrice() {

        String sql = "SELECT TOP (1) * FROM UtilityPrice WHERE effectiveFrom <= CAST(GETDATE() AS DATE) ORDER BY effectiveFrom DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return mapUtilityPrice(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn giá hiện tại", e);
        }

        return null;
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

    public List<Contract> findActiveContracts() {

        List<Contract> list = new ArrayList<>();

        String sql
                = "SELECT contractId, roomId, tenantId, numPeople, startElectricIndex, startWaterIndex, status "
                + "FROM Contract "
                + "WHERE status = 'ACTIVE'";

        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Contract c = new Contract();

                c.setContractId(rs.getInt("contractId"));
                c.setRoomId(rs.getInt("roomId"));
                c.setTenantId(rs.getInt("tenantId"));
                c.setNumPeople(rs.getInt("numPeople"));
                c.setStartElectricIndex(rs.getInt("startElectricIndex"));
                c.setStartWaterIndex(rs.getInt("startWaterIndex"));
                c.setStatus(rs.getString("status"));

                list.add(c);
            }
        } catch (SQLException e) {

            throw new RuntimeException("Lỗi truy vấn hợp đồng ACTIVE", e);
        }

        return list;
    }
}
