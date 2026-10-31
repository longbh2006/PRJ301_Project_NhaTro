package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MaintenanceRequest {

    private int ticketId;
    private int contractId;
    private String title;
    private String description;
    private String status;
    private String managerNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    
    // Từ JOIN bảng Room
    private String roomCode;

    public MaintenanceRequest() {
    }

    public MaintenanceRequest(int ticketId, int contractId, String title, String description, String status, String managerNote, LocalDateTime createdAt, LocalDateTime resolvedAt, String roomCode) {
        this.ticketId = ticketId;
        this.contractId = contractId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.managerNote = managerNote;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.roomCode = roomCode;
    }

    public String getTicketCode() {
        return String.format(Locale.ROOT, "SC%03d", ticketId);
    }

    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // SỬA LỖI 2: Thêm getter để hiển thị ngày xử lý xong trên JSP (trả về chuỗi rỗng nếu null)
    public String getResolvedAtDisplay() {
        return resolvedAt == null ? "" : resolvedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // Getters và Setters
    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagerNote() {
        return managerNote;
    }

    public void setManagerNote(String managerNote) {
        this.managerNote = managerNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}