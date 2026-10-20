package com.queuesense.dto;

import java.time.LocalDateTime;

public class ELibrarySeatDto {

    private Integer seatNumber;
    private Long resourceId;
    private String resourceName;
    private String type;
    private String status; // "AVAILABLE", "PENDING", "RESERVED", "OCCUPIED", "MAINTENANCE"
    private boolean bookable;
    private boolean isMyBooking;
    private String currentRequestCode;
    private String studentName;
    private String expectedArrival;
    private String expectedDuration;
    private LocalDateTime slotEndTime;

    public ELibrarySeatDto() {}

    public ELibrarySeatDto(Integer seatNumber,
                           Long resourceId,
                           String resourceName,
                           String type,
                           String status,
                           boolean bookable) {
        this.seatNumber = seatNumber;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.type = type;
        this.status = status;
        this.bookable = bookable;
    }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBookable() { return bookable; }
    public void setBookable(boolean bookable) { this.bookable = bookable; }

    public boolean isMyBooking() { return isMyBooking; }
    public void setMyBooking(boolean myBooking) { isMyBooking = myBooking; }

    public String getCurrentRequestCode() { return currentRequestCode; }
    public void setCurrentRequestCode(String currentRequestCode) { this.currentRequestCode = currentRequestCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String expectedDuration) { this.expectedDuration = expectedDuration; }

    public LocalDateTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalDateTime slotEndTime) { this.slotEndTime = slotEndTime; }
}
