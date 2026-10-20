package com.queuesense.dto;

import java.util.ArrayList;
import java.util.List;

public class SportsStudentAvailabilityResponseDto {
    private boolean hasActiveSportsBooking;
    private String activeBookingCode;
    private String activeBookingResourceName;
    private String activeBookingStatus;
    private List<SportsResourceDto> equipment = new ArrayList<>();
    private List<SportsResourceDto> courtsAndNets = new ArrayList<>();

    public SportsStudentAvailabilityResponseDto() {}

    public boolean isHasActiveSportsBooking() { return hasActiveSportsBooking; }
    public void setHasActiveSportsBooking(boolean hasActiveSportsBooking) { this.hasActiveSportsBooking = hasActiveSportsBooking; }

    public String getActiveBookingCode() { return activeBookingCode; }
    public void setActiveBookingCode(String activeBookingCode) { this.activeBookingCode = activeBookingCode; }

    public String getActiveBookingResourceName() { return activeBookingResourceName; }
    public void setActiveBookingResourceName(String activeBookingResourceName) { this.activeBookingResourceName = activeBookingResourceName; }

    public String getActiveBookingStatus() { return activeBookingStatus; }
    public void setActiveBookingStatus(String activeBookingStatus) { this.activeBookingStatus = activeBookingStatus; }

    public List<SportsResourceDto> getEquipment() { return equipment; }
    public void setEquipment(List<SportsResourceDto> equipment) { this.equipment = equipment; }

    public List<SportsResourceDto> getCourtsAndNets() { return courtsAndNets; }
    public void setCourtsAndNets(List<SportsResourceDto> courtsAndNets) { this.courtsAndNets = courtsAndNets; }
}
