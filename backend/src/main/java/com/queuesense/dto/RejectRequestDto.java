package com.queuesense.dto;

public class RejectRequestDto {
    private String rejectionReason;

    public RejectRequestDto() {}

    public RejectRequestDto(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
