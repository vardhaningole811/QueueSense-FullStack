package com.queuesense.service;

import com.queuesense.dto.ELibraryFloorPlanDto;
import com.queuesense.dto.ELibrarySeatDto;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.UsageRequest;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.FacilityRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ELibraryService {

    private static final Logger log = LoggerFactory.getLogger(ELibraryService.class);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final FacilityRepository facilityRepository;
    private final ResourceRepository resourceRepository;
    private final UsageRequestRepository usageRequestRepository;
    private final BookingLifecycleService bookingLifecycleService;

    public ELibraryService(FacilityRepository facilityRepository,
                           ResourceRepository resourceRepository,
                           UsageRequestRepository usageRequestRepository,
                           BookingLifecycleService bookingLifecycleService) {
        this.facilityRepository = facilityRepository;
        this.resourceRepository = resourceRepository;
        this.usageRequestRepository = usageRequestRepository;
        this.bookingLifecycleService = bookingLifecycleService;
    }

    @PostConstruct
    @Transactional
    public void initializeELibraryWorkstations() {
        Optional<Facility> elibraryOpt = facilityRepository.findAll().stream()
                .filter(f -> "E-Library".equalsIgnoreCase(f.getName()))
                .findFirst();

        if (elibraryOpt.isEmpty()) {
            return;
        }

        Facility elibrary = elibraryOpt.get();
        List<Resource> existingResources = resourceRepository.findByFacilityAndActiveTrue(elibrary);

        Map<Integer, Resource> seatMap = new HashMap<>();
        for (Resource r : existingResources) {
            Integer num = extractSeatNumber(r.getName());
            if (num != null && num >= 1 && num <= 68 && !seatMap.containsKey(num)) {
                seatMap.put(num, r);
            }
        }

        int addedCount = 0;
        for (int i = 1; i <= 68; i++) {
            if (!seatMap.containsKey(i)) {
                String name = (i <= 9) ? String.format("Seat %02d", i) : String.format("Workstation %02d", i);
                Resource newResource = new Resource(elibrary, name, "PC", ResourceStatus.AVAILABLE);
                resourceRepository.save(newResource);
                seatMap.put(i, newResource);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            log.info("Initialized {} missing E-Library workstations (total 68 bookable workstations).", addedCount);
        }
    }

    @Transactional
    public ELibraryFloorPlanDto getFloorPlan(String currentUserEmail) {
        LocalDateTime now = bookingLifecycleService.getCurrentTime();
        bookingLifecycleService.processExpiredBookings(now);

        Facility elibrary = facilityRepository.findAll().stream()
                .filter(f -> "E-Library".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("E-Library facility not found"));

        List<Resource> resources = resourceRepository.findByFacilityAndActiveTrue(elibrary);

        Map<Integer, Resource> seatResourceMap = new HashMap<>();
        for (Resource r : resources) {
            Integer num = extractSeatNumber(r.getName());
            if (num != null && num >= 1 && num <= 68 && !seatResourceMap.containsKey(num)) {
                seatResourceMap.put(num, r);
            }
        }

        List<RequestStatus> activeStatuses = Arrays.asList(
                RequestStatus.PENDING,
                RequestStatus.APPROVED,
                RequestStatus.CHECKED_IN
        );

        List<UsageRequest> activeRequests = usageRequestRepository.findByFacilityAndStatusOrderByCreatedAtDesc(elibrary, RequestStatus.PENDING);
        activeRequests.addAll(usageRequestRepository.findByFacilityAndStatusOrderByCreatedAtDesc(elibrary, RequestStatus.APPROVED));
        activeRequests.addAll(usageRequestRepository.findByFacilityAndStatusOrderByCreatedAtDesc(elibrary, RequestStatus.CHECKED_IN));

        // Group active requests by resource ID
        Map<Long, List<UsageRequest>> reqByResource = new HashMap<>();
        for (UsageRequest req : activeRequests) {
            if (req.getResource() != null) {
                reqByResource.computeIfAbsent(req.getResource().getId(), k -> new ArrayList<>()).add(req);
            }
        }

        List<ELibrarySeatDto> seatDtos = new ArrayList<>();
        int availableCount = 0;
        int occupiedCount = 0;
        int reservedCount = 0;
        int pendingCount = 0;
        int maintenanceCount = 0;

        for (int i = 1; i <= 68; i++) {
            Resource r = seatResourceMap.get(i);
            if (r == null) {
                // Should not happen once seeded, but safeguard
                ELibrarySeatDto fallback = new ELibrarySeatDto(i, null, "Workstation " + i, "PC", "AVAILABLE", true);
                seatDtos.add(fallback);
                availableCount++;
                continue;
            }

            List<UsageRequest> reqsOnSeat = reqByResource.getOrDefault(r.getId(), Collections.emptyList());
            UsageRequest activeBooking = reqsOnSeat.isEmpty() ? null : reqsOnSeat.get(0);

            String status;
            boolean bookable = false;

            if (r.getStatus() == ResourceStatus.MAINTENANCE) {
                status = "MAINTENANCE";
                maintenanceCount++;
            } else if (activeBooking != null) {
                if (activeBooking.getStatus() == RequestStatus.CHECKED_IN || r.getStatus() == ResourceStatus.OCCUPIED) {
                    status = "OCCUPIED";
                    occupiedCount++;
                } else if (activeBooking.getStatus() == RequestStatus.APPROVED || r.getStatus() == ResourceStatus.RESERVED) {
                    status = "RESERVED";
                    reservedCount++;
                } else if (activeBooking.getStatus() == RequestStatus.PENDING) {
                    status = "PENDING";
                    pendingCount++;
                } else {
                    status = "OCCUPIED";
                    occupiedCount++;
                }
            } else if (r.getStatus() == ResourceStatus.OCCUPIED) {
                status = "OCCUPIED";
                occupiedCount++;
            } else if (r.getStatus() == ResourceStatus.RESERVED) {
                status = "RESERVED";
                reservedCount++;
            } else {
                status = "AVAILABLE";
                bookable = true;
                availableCount++;
            }

            ELibrarySeatDto dto = new ELibrarySeatDto(
                    i,
                    r.getId(),
                    r.getName(),
                    r.getType(),
                    status,
                    bookable
            );

            if (activeBooking != null) {
                boolean isMine = currentUserEmail != null &&
                        activeBooking.getStudent() != null &&
                        currentUserEmail.equalsIgnoreCase(activeBooking.getStudent().getEmail());
                dto.setMyBooking(isMine);
                dto.setCurrentRequestCode(activeBooking.getRequestCode());
                dto.setExpectedArrival(activeBooking.getExpectedArrival());
                dto.setExpectedDuration(activeBooking.getExpectedDuration());
                dto.setSlotEndTime(activeBooking.getSlotEndTime());
                if (activeBooking.getStudent() != null) {
                    dto.setStudentName(activeBooking.getStudent().getName());
                }
            }

            seatDtos.add(dto);
        }

        ELibraryFloorPlanDto floorPlan = new ELibraryFloorPlanDto();
        floorPlan.setFacilityId(elibrary.getId());
        floorPlan.setFacilityName(elibrary.getName());
        floorPlan.setDescription(elibrary.getDescription());
        floorPlan.setTotalSeats(68);
        floorPlan.setAvailableCount(availableCount);
        floorPlan.setOccupiedCount(occupiedCount);
        floorPlan.setReservedCount(reservedCount);
        floorPlan.setPendingCount(pendingCount);
        floorPlan.setMaintenanceCount(maintenanceCount);
        floorPlan.setSeats(seatDtos);

        return floorPlan;
    }

    public static Integer extractSeatNumber(String resourceName) {
        if (resourceName == null) return null;
        Matcher m = NUMBER_PATTERN.matcher(resourceName);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
