package com.queuesense.dto;

import com.queuesense.enums.ResourceStatus;

public class SportsResourceDto {
    private Long id;
    private String name;
    private String type; // "EQUIPMENT" or "COURT"
    private String bookingUnit;
    private int totalQuantity;
    private int usableQuantity;
    private int unavailableQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private boolean bookingClosed;
    private String closureReason;
    private String statusText;
    private ResourceStatus status;

    public SportsResourceDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBookingUnit() { return bookingUnit; }
    public void setBookingUnit(String bookingUnit) { this.bookingUnit = bookingUnit; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public int getUsableQuantity() { return usableQuantity; }
    public void setUsableQuantity(int usableQuantity) { this.usableQuantity = usableQuantity; }

    public int getUnavailableQuantity() { return unavailableQuantity; }
    public void setUnavailableQuantity(int unavailableQuantity) { this.unavailableQuantity = unavailableQuantity; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public boolean isBookingClosed() { return bookingClosed; }
    public void setBookingClosed(boolean bookingClosed) { this.bookingClosed = bookingClosed; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
}
