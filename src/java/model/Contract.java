package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Contract {

    private int contractId;
    private int requestId;
    private int tenantId;
    private int roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private int numPeople;
    private int startElectricIndex;
    private int startWaterIndex;
    private String status;
    private LocalDateTime endedAt;
    private String roomCode;
    private String tenantName;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public Contract() {
    }

    public Contract(int contractId, int requestId, int tenantId, int roomId,
            LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent,
            BigDecimal deposit, int numPeople, int startElectricIndex,
            int startWaterIndex, String status, LocalDateTime endedAt) {
        this.contractId = contractId;
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.deposit = deposit;
        this.numPeople = numPeople;
        this.startElectricIndex = startElectricIndex;
        this.startWaterIndex = startWaterIndex;
        this.status = status;
        this.endedAt = endedAt;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public int getNumPeople() {
        return numPeople;
    }

    public void setNumPeople(int numPeople) {
        this.numPeople = numPeople;
    }

    public int getStartElectricIndex() {
        return startElectricIndex;
    }

    public void setStartElectricIndex(int startElectricIndex) {
        this.startElectricIndex = startElectricIndex;
    }

    public int getStartWaterIndex() {
        return startWaterIndex;
    }

    public void setStartWaterIndex(int startWaterIndex) {
        this.startWaterIndex = startWaterIndex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
