package model;

/** Một dòng trên trang ghi chỉ số: hợp đồng ACTIVE của phòng và chỉ số trong kỳ đang xem. */
public class MeterRow {
    private final Contract contract;
    private final MeterReading previous;
    private final MeterReading current;
    private final Integer invoiceId;
    private Integer oldElectric;
    private Integer oldWater;

    public MeterRow(Contract contract, MeterReading previous, MeterReading current, Integer invoiceId) {
        this.contract = contract;
        this.previous = previous;
        this.current = current;
        this.invoiceId = invoiceId;
    }

    public Contract getContract() { return contract; }
    public MeterReading getPrevious() { return previous; }
    public MeterReading getCurrent() { return current; }
    public Integer getInvoiceId() { return invoiceId; }
    public Integer getOldElectric() { return oldElectric; }
    public Integer getOldWater() { return oldWater; }

    public void setOldIndexes(Integer oldElectric, Integer oldWater) {
        this.oldElectric = oldElectric;
        this.oldWater = oldWater;
    }

    public boolean isRecorded() { return current != null; }
    public boolean isInvoiced() { return invoiceId != null; }
    public boolean isOldIndexKnown() { return oldElectric != null && oldWater != null; }

    public Integer getElectricUsed() {
        return isRecorded() && isOldIndexKnown() ? current.getElectricIndex() - oldElectric : null;
    }

    public Integer getWaterUsed() {
        return isRecorded() && isOldIndexKnown() ? current.getWaterIndex() - oldWater : null;
    }
}
