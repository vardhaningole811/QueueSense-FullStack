package com.queuesense.dto;

public class ToggleBookingClosureDto {
    private boolean closed;
    private String reason;

    public ToggleBookingClosureDto() {}

    public ToggleBookingClosureDto(boolean closed) {
        this.closed = closed;
    }

    public ToggleBookingClosureDto(boolean closed, String reason) {
        this.closed = closed;
        this.reason = reason;
    }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
