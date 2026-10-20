package com.queuesense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CreateUsageRequestDto {
    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    private Long resourceItemId;

    @NotBlank(message = "Expected arrival time is required")
    private String expectedArrival;

    @NotBlank(message = "Expected duration is required")
    private String expectedDuration;

    @Min(value = 1, message = "Group size must be at least 1")
    @Max(value = 10, message = "Group size cannot exceed 10")
    private Integer groupSize = 1;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;

    public CreateUsageRequestDto() {}

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getResourceItemId() { return resourceItemId; }
    public void setResourceItemId(Long resourceItemId) { this.resourceItemId = resourceItemId; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String expectedDuration) { this.expectedDuration = expectedDuration; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }

    public Integer getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = (quantity != null && quantity > 0) ? quantity : 1; }

    public LocalDateTime getSlotStartTime() { return slotStartTime; }
    public void setSlotStartTime(LocalDateTime slotStartTime) { this.slotStartTime = slotStartTime; }

    public LocalDateTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalDateTime slotEndTime) { this.slotEndTime = slotEndTime; }
}
