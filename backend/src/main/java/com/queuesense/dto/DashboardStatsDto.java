package com.queuesense.dto;

public class DashboardStatsDto {
    private long pendingRequests;
    private long availableResources;
    private long reservedResources;
    private long occupiedResources;
    private long maintenanceResources;
    private long totalResources;
    private long todayCheckIns;

    public DashboardStatsDto() {}

    public DashboardStatsDto(long pendingRequests, long availableResources, long reservedResources,
                             long occupiedResources, long maintenanceResources, long totalResources,
                             long todayCheckIns) {
        this.pendingRequests = pendingRequests;
        this.availableResources = availableResources;
        this.reservedResources = reservedResources;
        this.occupiedResources = occupiedResources;
        this.maintenanceResources = maintenanceResources;
        this.totalResources = totalResources;
        this.todayCheckIns = todayCheckIns;
    }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }
    public long getPendingRequestsCount() { return pendingRequests; }
    public void setPendingRequestsCount(long count) { this.pendingRequests = count; }

    public long getAvailableResources() { return availableResources; }
    public void setAvailableResources(long availableResources) { this.availableResources = availableResources; }
    public long getAvailableResourcesCount() { return availableResources; }
    public void setAvailableResourcesCount(long count) { this.availableResources = count; }

    public long getReservedResources() { return reservedResources; }
    public void setReservedResources(long reservedResources) { this.reservedResources = reservedResources; }
    public long getReservedResourcesCount() { return reservedResources; }
    public void setReservedResourcesCount(long count) { this.reservedResources = count; }

    public long getOccupiedResources() { return occupiedResources; }
    public void setOccupiedResources(long occupiedResources) { this.occupiedResources = occupiedResources; }
    public long getOccupiedResourcesCount() { return occupiedResources; }
    public void setOccupiedResourcesCount(long count) { this.occupiedResources = count; }

    public long getMaintenanceResources() { return maintenanceResources; }
    public void setMaintenanceResources(long maintenanceResources) { this.maintenanceResources = maintenanceResources; }
    public long getMaintenanceResourcesCount() { return maintenanceResources; }
    public void setMaintenanceResourcesCount(long count) { this.maintenanceResources = count; }

    public long getTotalResources() { return totalResources; }
    public void setTotalResources(long totalResources) { this.totalResources = totalResources; }
    public long getTotalResourcesCount() { return totalResources; }
    public void setTotalResourcesCount(long count) { this.totalResources = count; }

    public long getTodayCheckIns() { return todayCheckIns; }
    public void setTodayCheckIns(long todayCheckIns) { this.todayCheckIns = todayCheckIns; }
    public long getTodayCheckInsCount() { return todayCheckIns; }
    public void setTodayCheckInsCount(long count) { this.todayCheckIns = count; }
}
