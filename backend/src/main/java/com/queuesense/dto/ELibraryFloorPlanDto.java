package com.queuesense.dto;

import java.util.List;

public class ELibraryFloorPlanDto {

    private Long facilityId;
    private String facilityName;
    private String description;
    private int totalSeats;
    private int availableCount;
    private int occupiedCount;
    private int reservedCount;
    private int pendingCount;
    private int maintenanceCount;
    private List<ELibrarySeatDto> seats;

    public ELibraryFloorPlanDto() {}

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }

    public int getOccupiedCount() { return occupiedCount; }
    public void setOccupiedCount(int occupiedCount) { this.occupiedCount = occupiedCount; }

    public int getReservedCount() { return reservedCount; }
    public void setReservedCount(int reservedCount) { this.reservedCount = reservedCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getMaintenanceCount() { return maintenanceCount; }
    public void setMaintenanceCount(int maintenanceCount) { this.maintenanceCount = maintenanceCount; }

    public List<ELibrarySeatDto> getSeats() { return seats; }
    public void setSeats(List<ELibrarySeatDto> seats) { this.seats = seats; }
}
