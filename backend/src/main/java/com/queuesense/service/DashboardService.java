package com.queuesense.service;

import com.queuesense.dto.DashboardStatsDto;
import com.queuesense.dto.SportsResourceDto;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.repository.CheckInRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final CheckInRepository checkInRepository;
    private final SportsInventoryService sportsInventoryService;
    private final BookingLifecycleService bookingLifecycleService;

    public DashboardService(UsageRequestRepository usageRequestRepository,
                            ResourceRepository resourceRepository,
                            CheckInRepository checkInRepository,
                            SportsInventoryService sportsInventoryService,
                            BookingLifecycleService bookingLifecycleService) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.checkInRepository = checkInRepository;
        this.sportsInventoryService = sportsInventoryService;
        this.bookingLifecycleService = bookingLifecycleService;
    }

    public DashboardStatsDto getDashboardStats() {
        LocalDateTime now = bookingLifecycleService.getCurrentTime();
        // 1. Process any stale holds or expired slots so metrics are 100% current
        bookingLifecycleService.processExpiredBookings(now);

        // 1. Pending requests genuinely awaiting approval
        long pending = usageRequestRepository.countByStatus(RequestStatus.PENDING);

        // 2. Reserved (Approved) bookings that have not yet transitioned to active use or check-in
        long reserved = usageRequestRepository.countByStatus(RequestStatus.APPROVED);

        // 3. Currently Occupied: active checked-in sessions
        long nonSportsOccupied = resourceRepository.countNonSportsByStatus(ResourceStatus.OCCUPIED);
        long sportsOccupied = usageRequestRepository.sumSportsQuantityByStatus(RequestStatus.CHECKED_IN);
        long occupied = nonSportsOccupied + sportsOccupied;

        // 4. Available Resources: count usable resources/units actually available for booking
        long nonSportsAvailable = resourceRepository.countNonSportsByStatus(ResourceStatus.AVAILABLE);

        Facility sportsFacility = sportsInventoryService.getSportsFacility();
        List<Resource> activeSports = resourceRepository.findByFacilityAndActiveTrue(sportsFacility);
        long sportsAvailable = 0;
        long sportsMaintenance = 0;
        long sportsTotal = 0;

        for (Resource res : activeSports) {
            SportsResourceDto inv = sportsInventoryService.calculateResourceInventory(res);
            sportsAvailable += inv.getAvailableQuantity();
            sportsMaintenance += inv.getUnavailableQuantity();
            sportsTotal += inv.getTotalQuantity();
        }

        long available = nonSportsAvailable + sportsAvailable;
        long maintenance = resourceRepository.countNonSportsByStatus(ResourceStatus.MAINTENANCE) + sportsMaintenance;
        long total = resourceRepository.countNonSportsTotal() + sportsTotal;

        // 5. Today's Check-ins on the current calendar date in Asia/Kolkata
        LocalDate todayKolkata = now.toLocalDate();
        LocalDateTime startOfDay = todayKolkata.atStartOfDay();
        LocalDateTime startOfNextDay = todayKolkata.plusDays(1).atStartOfDay();
        long todayCheckIns = checkInRepository.countTodayCheckIns(startOfDay, startOfNextDay);

        return new DashboardStatsDto(pending, available, reserved, occupied, maintenance, total, todayCheckIns);
    }
}
