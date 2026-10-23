package com.queuesense.dto.analytics;

public class CrowdSummaryDto {

    private Long facilityId;
    private String facilityName;
    private String facilityType;
    private long recordedActiveSessions;
    private Long usableCapacity;
    private Double recordedOccupancyPercentage;
    private String recordedCrowdLevel;
    private Long availableUsableResources;

    public CrowdSummaryDto() {}

    public CrowdSummaryDto(Long facilityId, String facilityName, String facilityType,
                           long recordedActiveSessions, Long usableCapacity,
                           Double recordedOccupancyPercentage, String recordedCrowdLevel,
                           Long availableUsableResources) {
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.recordedActiveSessions = recordedActiveSessions;
        this.usableCapacity = usableCapacity;
        this.recordedOccupancyPercentage = recordedOccupancyPercentage;
        this.recordedCrowdLevel = recordedCrowdLevel;
        this.availableUsableResources = availableUsableResources;
    }

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public long getRecordedActiveSessions() { return recordedActiveSessions; }
    public void setRecordedActiveSessions(long recordedActiveSessions) { this.recordedActiveSessions = recordedActiveSessions; }

    public Long getUsableCapacity() { return usableCapacity; }
    public void setUsableCapacity(Long usableCapacity) { this.usableCapacity = usableCapacity; }

    public Double getRecordedOccupancyPercentage() { return recordedOccupancyPercentage; }
    public void setRecordedOccupancyPercentage(Double recordedOccupancyPercentage) { this.recordedOccupancyPercentage = recordedOccupancyPercentage; }

    public String getRecordedCrowdLevel() { return recordedCrowdLevel; }
    public void setRecordedCrowdLevel(String recordedCrowdLevel) { this.recordedCrowdLevel = recordedCrowdLevel; }

    public Long getAvailableUsableResources() { return availableUsableResources; }
    public void setAvailableUsableResources(Long availableUsableResources) { this.availableUsableResources = availableUsableResources; }
}
