package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OccupancyStats {
    private final int openRooms;
    private final int rentedRooms;

    public OccupancyStats(int openRooms, int rentedRooms) {
        this.openRooms = openRooms;
        this.rentedRooms = rentedRooms;
    }

    public int getOpenRooms() { return openRooms; }
    public int getRentedRooms() { return rentedRooms; }
    public int getVacantRooms() { return openRooms - rentedRooms; }

    public BigDecimal getOccupancyRate() {
        if (openRooms == 0) { return BigDecimal.ZERO; }
        return BigDecimal.valueOf(rentedRooms * 100L)
                .divide(BigDecimal.valueOf(openRooms), 1, RoundingMode.HALF_UP);
    }
}
