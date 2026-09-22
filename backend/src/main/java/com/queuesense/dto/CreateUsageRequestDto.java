package com.queuesense.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUsageRequestDto {
    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Expected arrival time is required")
    private String expectedArrival;

    @NotBlank(message = "Expected duration is required")
    private String expectedDuration;

    @Min(value = 1, message = "Group size must be at least 1")
    @Max(value = 3, message = "Group size cannot exceed 3")
    private Integer groupSize = 1;

    public CreateUsageRequestDto() {}

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String expectedDuration) { this.expectedDuration = expectedDuration; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }
}
