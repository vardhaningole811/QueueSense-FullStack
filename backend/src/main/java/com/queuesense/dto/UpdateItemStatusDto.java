package com.queuesense.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateItemStatusDto {
    @NotBlank(message = "Status is required")
    private String status; // AVAILABLE, DAMAGED, MAINTENANCE, LOST
    private String conditionNotes;

    public UpdateItemStatusDto() {}

    public UpdateItemStatusDto(String status, String conditionNotes) {
        this.status = status;
        this.conditionNotes = conditionNotes;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }
}
