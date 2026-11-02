/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Lenovo
 */
public class Invoice {
    // Theo DEFAULT/CHECK của Invoice trong database/setup.sql.
    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_PAID = "PAID";

    private int invoiceId;
    private int contractId;
    private int year;
    private int month;
    private BigDecimal roomFee;
    private int electricUsed;
    private BigDecimal electricFee;
    private int waterUsed;
    private BigDecimal waterFee;
    private BigDecimal serviceFee;
    private BigDecimal total;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private int priceId;
    private LocalDateTime createdAt;

    // Trường hiển thị cho trang hoá đơn của người thuê (NT-38), lấy qua JOIN.
    private String roomCode;
    private boolean overdue;
    private int numPeople;
    private Integer electricIndex;
    private Integer waterIndex;
    private UtilityPrice utilityPrice;

    public String getCode() {
        return String.format("INV%03d", invoiceId);
    }

    public String getPeriodDisplay() {
        return String.format("%02d/%d", month, year);
    }

    public String getDueDateDisplay() {
        return dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    public String getPaidAtDisplay() {
        return paidAt != null ? paidAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    public boolean isUnpaid() {
        return STATUS_UNPAID.equals(status);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public int getNumPeople() {
        return numPeople;
    }

    public void setNumPeople(int numPeople) {
        this.numPeople = numPeople;
    }

    public Integer getElectricIndex() {
        return electricIndex;
    }

    public void setElectricIndex(Integer electricIndex) {
        this.electricIndex = electricIndex;
    }

    public Integer getWaterIndex() {
        return waterIndex;
    }

    public void setWaterIndex(Integer waterIndex) {
        this.waterIndex = waterIndex;
    }

    public UtilityPrice getUtilityPrice() {
        return utilityPrice;
    }

    public void setUtilityPrice(UtilityPrice utilityPrice) {
        this.utilityPrice = utilityPrice;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getRoomFee() {
        return roomFee;
    }

    public void setRoomFee(BigDecimal roomFee) {
        this.roomFee = roomFee;
    }

    public int getElectricUsed() {
        return electricUsed;
    }

    public void setElectricUsed(int electricUsed) {
        this.electricUsed = electricUsed;
    }

    public BigDecimal getElectricFee() {
        return electricFee;
    }

    public void setElectricFee(BigDecimal electricFee) {
        this.electricFee = electricFee;
    }

    public int getWaterUsed() {
        return waterUsed;
    }

    public void setWaterUsed(int waterUsed) {
        this.waterUsed = waterUsed;
    }

    public BigDecimal getWaterFee() {
        return waterFee;
    }

    public void setWaterFee(BigDecimal waterFee) {
        this.waterFee = waterFee;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public int getPriceId() {
        return priceId;
    }

    public void setPriceId(int priceId) {
        this.priceId = priceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

   
    
    
}
