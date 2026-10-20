package com.queuesense.dto;

import java.time.LocalDateTime;

public class CourtSlotDto {
    private Long resourceId;
    private String resourceName;
    private Long resourceItemId;
    private String resourceItemName;
    private String itemCode;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;
    private String slotLabel; // e.g. "02:00 PM - 03:00 PM"
    private String status; // AVAILABLE_TO_BOOK, UPCOMING, RESERVED, OCCUPIED, MAINTENANCE, PASSED
    private LocalDateTime bookingOpensAt;
    private boolean canBook;
    private String notes;

    public CourtSlotDto() {}

    public CourtSlotDto(Long resourceId, String resourceName, Long resourceItemId,
                        String resourceItemName, String itemCode, LocalDateTime slotStartTime,
                        LocalDateTime slotEndTime, String slotLabel, String status,
                        LocalDateTime bookingOpensAt, boolean canBook, String notes) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceItemId = resourceItemId;
        this.resourceItemName = resourceItemName;
        this.itemCode = itemCode;
        this.slotStartTime = slotStartTime;
        this.slotEndTime = slotEndTime;
        this.slotLabel = slotLabel;
        this.status = status;
        this.bookingOpensAt = bookingOpensAt;
        this.canBook = canBook;
        this.notes = notes;
    }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public Long getResourceItemId() { return resourceItemId; }
    public void setResourceItemId(Long resourceItemId) { this.resourceItemId = resourceItemId; }

    public String getResourceItemName() { return resourceItemName; }
    public void setResourceItemName(String resourceItemName) { this.resourceItemName = resourceItemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public LocalDateTime getSlotStartTime() { return slotStartTime; }
    public void setSlotStartTime(LocalDateTime slotStartTime) { this.slotStartTime = slotStartTime; }

    public LocalDateTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalDateTime slotEndTime) { this.slotEndTime = slotEndTime; }

    public String getSlotLabel() { return slotLabel; }
    public void setSlotLabel(String slotLabel) { this.slotLabel = slotLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBookingOpensAt() { return bookingOpensAt; }
    public void setBookingOpensAt(LocalDateTime bookingOpensAt) { this.bookingOpensAt = bookingOpensAt; }

    public boolean isCanBook() { return canBook; }
    public void setCanBook(boolean canBook) { this.canBook = canBook; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
