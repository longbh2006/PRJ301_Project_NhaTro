/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import model.Contract;
import model.Invoice;
import model.MeterReading;
import model.UtilityPrice;
import dal.BillingDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lenovo
 */
public class BillingService {

    public Invoice tinhHoaDon(Contract ct, MeterReading cur, MeterReading prev,
            UtilityPrice gia, int year, int month) {

        /* 1. Số kWh, số m³ */
        int dienCu = (prev != null) ? prev.getElectricIndex() : ct.getStartElectricIndex();
        int nuocCu = (prev != null) ? prev.getWaterIndex() : ct.getStartWaterIndex();

        int dienMoi = cur.getElectricIndex();
        int nuocMoi = cur.getWaterIndex();

        int kWh = dienMoi - dienCu;
        int m3 = nuocMoi - nuocCu;

        /* 2. Tiền điện / nước */
        BigDecimal tienDien = BigDecimal.valueOf(kWh)
                .multiply(gia.getElectricPrice());          // UtilityPrice.getElectricPrice()
        BigDecimal tienNuoc = BigDecimal.valueOf(m3)
                .multiply(gia.getWaterPrice());             // UtilityPrice.getWaterPrice()

        /* 3. Dịch vụ */
        BigDecimal dienSu = BigDecimal.valueOf(ct.getNumPeople())
                .multiply(gia.getServiceFeePerPerson());

        /* 4. Tiền phòng */
        BigDecimal tienPhong = ct.getMonthlyRent(); // hoặc ct.getRoomRent()

        /* 5. Tổng cộng */
        BigDecimal tong = tienPhong.add(tienDien).add(tienNuoc).add(dienSu);

        /* 6. Hạn trả: ngày 10 tháng sau */
        LocalDate hanTra = LocalDate.of(year, month, 10).plusMonths(1);

        /* 7. Tạo Invoice và set các trường */
        Invoice inv = new Invoice();
        inv.setContractId(ct.getContractId());

        inv.setYear(year);
        inv.setMonth(month);

        inv.setElectricUsed(kWh);
        inv.setWaterUsed(m3);

        inv.setElectricFee(tienDien);
        inv.setWaterFee(tienNuoc);
        inv.setServiceFee(dienSu);
        inv.setRoomFee(tienPhong);
        inv.setTotal(tong);

        inv.setPriceId(gia.getPriceId());
        inv.setDueDate(hanTra);
        inv.setStatus("UNPAID");

        return inv;
    }

    public int lapHoaDonThang(int year, int month) throws SQLException {
        BillingDAO dao = new BillingDAO();
        UtilityPrice gia = dao.currentPrice();
        if (gia == null) {
            throw new SQLException("Chưa có bảng giá");
        }
        List<Contract> hopDongs = dao.findActiveContracts();
        List<Invoice> hoaDons = new ArrayList<>();
        for (Contract ct : hopDongs) {
            MeterReading cur = dao.findReading(ct.getRoomId(), year, month);
            if (cur == null) {
                throw new SQLException("Phòng " + ct.getRoomId() + " chưa nhập chỉ số tháng " + month);
            }
            MeterReading prev = dao.findReadingBefore(ct.getRoomId(), year, month);
            hoaDons.add(tinhHoaDon(ct, cur, prev, gia, year, month));
        }

        Connection con = dao.getConnection();
        try {
            con.setAutoCommit(false);
            for (Invoice inv : hoaDons) {
                dao.insertInvoice(con, inv);
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
        return hoaDons.size();
    }
}
