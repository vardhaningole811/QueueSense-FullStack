package com.queuesense.dto.analytics;

import java.util.List;

public class CrowdAnalyticsResponseDto {

    private Long facilityId;
    private String facilityName;
    private String facilityType;
    private long recordedActiveSessions;
    private Long usableCapacity;
    private long totalResources;
    private long maintenanceResources;
    private Double recordedOccupancyPercentage;
    private String recordedCrowdLevel; // LOW, MODERATE, HIGH, FULL
    private Long availableUsableResources;
    private String forecastStatus; // AVAILABLE, ML_SERVICE_OFFLINE, INSUFFICIENT_DATA
    private List<ForecastHorizonDto> forecasts;
    private Integer estimatedTimeUntilFullMinutes;
    private String timeUntilFullMessage;
    private String bestTimeToVisit;
    private ModelTransparencyDto modelTransparency;
    private String asOfTimestamp;
    private String occupancyTypeClarification;

    public CrowdAnalyticsResponseDto() {}

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

    public long getTotalResources() { return totalResources; }
    public void setTotalResources(long totalResources) { this.totalResources = totalResources; }

    public long getMaintenanceResources() { return maintenanceResources; }
    public void setMaintenanceResources(long maintenanceResources) { this.maintenanceResources = maintenanceResources; }

    public Double getRecordedOccupancyPercentage() { return recordedOccupancyPercentage; }
    public void setRecordedOccupancyPercentage(Double recordedOccupancyPercentage) { this.recordedOccupancyPercentage = recordedOccupancyPercentage; }

    public String getRecordedCrowdLevel() { return recordedCrowdLevel; }
    public void setRecordedCrowdLevel(String recordedCrowdLevel) { this.recordedCrowdLevel = recordedCrowdLevel; }

    public Long getAvailableUsableResources() { return availableUsableResources; }
    public void setAvailableUsableResources(Long availableUsableResources) { this.availableUsableResources = availableUsableResources; }

    public String getForecastStatus() { return forecastStatus; }
    public void setForecastStatus(String forecastStatus) { this.forecastStatus = forecastStatus; }

    public List<ForecastHorizonDto> getForecasts() { return forecasts; }
    public void setForecasts(List<ForecastHorizonDto> forecasts) { this.forecasts = forecasts; }

    public Integer getEstimatedTimeUntilFullMinutes() { return estimatedTimeUntilFullMinutes; }
    public void setEstimatedTimeUntilFullMinutes(Integer estimatedTimeUntilFullMinutes) { this.estimatedTimeUntilFullMinutes = estimatedTimeUntilFullMinutes; }

    public String getTimeUntilFullMessage() { return timeUntilFullMessage; }
    public void setTimeUntilFullMessage(String timeUntilFullMessage) { this.timeUntilFullMessage = timeUntilFullMessage; }

    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }

    public ModelTransparencyDto getModelTransparency() { return modelTransparency; }
    public void setModelTransparency(ModelTransparencyDto modelTransparency) { this.modelTransparency = modelTransparency; }

    public String getAsOfTimestamp() { return asOfTimestamp; }
    public void setAsOfTimestamp(String asOfTimestamp) { this.asOfTimestamp = asOfTimestamp; }

    public String getOccupancyTypeClarification() { return occupancyTypeClarification; }
    public void setOccupancyTypeClarification(String occupancyTypeClarification) { this.occupancyTypeClarification = occupancyTypeClarification; }
}
