package model;

import java.math.BigDecimal;

public class MonthlyRevenue {
    private final int month;
    private BigDecimal paidTotal = BigDecimal.ZERO;
    private int paidCount;
    private BigDecimal overdueTotal = BigDecimal.ZERO;
    private int overdueCount;

    public MonthlyRevenue(int month) {
        this.month = month;
    }

    public int getMonth() { return month; }

    public BigDecimal getPaidTotal() { return paidTotal; }
    public void setPaidTotal(BigDecimal paidTotal) { this.paidTotal = paidTotal; }

    public int getPaidCount() { return paidCount; }
    public void setPaidCount(int paidCount) { this.paidCount = paidCount; }

    public BigDecimal getOverdueTotal() { return overdueTotal; }
    public void setOverdueTotal(BigDecimal overdueTotal) { this.overdueTotal = overdueTotal; }

    public int getOverdueCount() { return overdueCount; }
    public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }
}
