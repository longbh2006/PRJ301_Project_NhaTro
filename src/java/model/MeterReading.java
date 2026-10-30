/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.time.LocalDateTime;

/**
 *
 * @author Lenovo
 */
public class MeterReading {
    private int readingId;
    private int roomId;
    private int year;
    private int month;
    private int electricIndex;
    private int waterIndex;
    private LocalDateTime recordedAt;

    public int getReadingId() {
        return readingId;
    }

    public void setReadingId(int readingId) {
        this.readingId = readingId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
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

    public int getElectricIndex() {
        return electricIndex;
    }

    public void setElectricIndex(int electricIndex) {
        this.electricIndex = electricIndex;
    }

    public int getWaterIndex() {
        return waterIndex;
    }

    public void setWaterIndex(int waterIndex) {
        this.waterIndex = waterIndex;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
     

    

 
    
}
