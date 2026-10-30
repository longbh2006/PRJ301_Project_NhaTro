package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalRequest {

    private int requestId;
    private int tenantId;
    private int roomId;
    private LocalDate moveInDate;
    private int months;
    private int numPeople;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private Integer decidedBy;

    public RentalRequest() {
    }

    public RentalRequest(int requestId, int tenantId, int roomId, LocalDate moveInDate,
            int months, int numPeople, String note, String status,
            LocalDateTime createdAt, LocalDateTime decidedAt, Integer decidedBy) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.moveInDate = moveInDate;
        this.months = months;
        this.numPeople = numPeople;
        this.note = note;
        this.status = status;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
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

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(LocalDate moveInDate) {
        this.moveInDate = moveInDate;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public int getNumPeople() {
        return numPeople;
    }

    public void setNumPeople(int numPeople) {
        this.numPeople = numPeople;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Integer getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Integer decidedBy) {
        this.decidedBy = decidedBy;
    }
}
