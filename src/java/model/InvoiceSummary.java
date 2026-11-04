package model;

import java.math.BigDecimal;

/**
 * Tổng hợp của bộ lọc hoá đơn phía quản lý: số hoá đơn, tổng phải thu và phần chưa thu.
 */
public class InvoiceSummary {

    private final long count;
    private final BigDecimal totalAmount;
    private final BigDecimal unpaidAmount;

    public InvoiceSummary(long count, BigDecimal totalAmount, BigDecimal unpaidAmount) {
        this.count = count;
        this.totalAmount = totalAmount;
        this.unpaidAmount = unpaidAmount;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getUnpaidAmount() {
        return unpaidAmount;
    }
}
