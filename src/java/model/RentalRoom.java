package model;

import java.math.BigDecimal;

/** Thông tin phòng dùng cho luồng gửi yêu cầu, độc lập với bộ lọc tìm phòng. */
public class RentalRoom {
    private final int roomId;
    private final String roomCode;
    private final String roomTypeName;
    private final int capacity;
    private final BigDecimal monthlyPrice;
    private final String status;
    private final String description;
    private final boolean occupied;
    private final int floor;
    private final BigDecimal area;
    private final String imageUrl;

    public RentalRoom(int roomId, String roomCode, String roomTypeName, int capacity,
            BigDecimal monthlyPrice, String status, String description, boolean occupied,
            int floor, BigDecimal area, String imageUrl) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.roomTypeName = roomTypeName;
        this.capacity = capacity;
        this.monthlyPrice = monthlyPrice;
        this.status = status;
        this.description = description;
        this.occupied = occupied;
        this.floor = floor;
        this.area = area;
        this.imageUrl = imageUrl;
    }

    public int getRoomId() { return roomId; }
    public String getRoomCode() { return roomCode; }
    public String getRoomTypeName() { return roomTypeName; }
    public int getCapacity() { return capacity; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public boolean isOccupied() { return occupied; }
    public int getFloor() { return floor; }
    public BigDecimal getArea() { return area; }
    public String getImageUrl() { return imageUrl; }
}
