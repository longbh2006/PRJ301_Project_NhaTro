package service;

import dal.ReportDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.MonthlyRevenue;
import model.OccupancyStats;

public class ReportService {
    // Cùng giới hạn với CK_Invoice_period trong database/setup.sql.
    public static final int MIN_YEAR = 2020;
    public static final int MAX_YEAR = 2100;

    public RevenueReport buildReport(int year, LocalDate today) throws SQLException {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("Năm báo cáo phải từ " + MIN_YEAR + " đến " + MAX_YEAR + ".");
        }
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = from.plusYears(1);
        try (ReportDAO dao = new ReportDAO()) {
            if (dao.getConnection() == null) {
                throw new SQLException("Database connection is unavailable.");
            }
            Map<Integer, ReportDAO.Totals> paid = dao.findPaidByMonth(from, to);
            Map<Integer, ReportDAO.Totals> overdue = dao.findOverdueByMonth(from, to, today);
            OccupancyStats occupancy = dao.findOccupancy();

            // Luôn đủ 12 dòng: tháng không có hóa đơn vẫn hiện 0 để cộng tay khớp với dòng tổng.
            List<MonthlyRevenue> months = new ArrayList<>();
            BigDecimal paidTotal = BigDecimal.ZERO;
            BigDecimal overdueTotal = BigDecimal.ZERO;
            int paidCount = 0;
            int overdueCount = 0;
            for (int month = 1; month <= 12; month++) {
                MonthlyRevenue row = new MonthlyRevenue(month);
                ReportDAO.Totals p = paid.get(month);
                if (p != null) {
                    row.setPaidTotal(p.amount());
                    row.setPaidCount(p.count());
                }
                ReportDAO.Totals o = overdue.get(month);
                if (o != null) {
                    row.setOverdueTotal(o.amount());
                    row.setOverdueCount(o.count());
                }
                paidTotal = paidTotal.add(row.getPaidTotal());
                overdueTotal = overdueTotal.add(row.getOverdueTotal());
                paidCount += row.getPaidCount();
                overdueCount += row.getOverdueCount();
                months.add(row);
            }
            return new RevenueReport(year, months, paidTotal, paidCount, overdueTotal, overdueCount, occupancy);
        }
    }

    public static class RevenueReport {
        private final int year;
        private final List<MonthlyRevenue> months;
        private final BigDecimal paidTotal;
        private final int paidCount;
        private final BigDecimal overdueTotal;
        private final int overdueCount;
        private final OccupancyStats occupancy;

        public RevenueReport(int year, List<MonthlyRevenue> months, BigDecimal paidTotal, int paidCount,
                BigDecimal overdueTotal, int overdueCount, OccupancyStats occupancy) {
            this.year = year;
            this.months = months;
            this.paidTotal = paidTotal;
            this.paidCount = paidCount;
            this.overdueTotal = overdueTotal;
            this.overdueCount = overdueCount;
            this.occupancy = occupancy;
        }

        public int getYear() { return year; }
        public List<MonthlyRevenue> getMonths() { return months; }
        public BigDecimal getPaidTotal() { return paidTotal; }
        public int getPaidCount() { return paidCount; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public int getOverdueCount() { return overdueCount; }
        public OccupancyStats getOccupancy() { return occupancy; }
    }
}
