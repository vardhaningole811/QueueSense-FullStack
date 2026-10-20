package com.queuesense.service;

import com.queuesense.dto.*;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.ResourceItem;
import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.User;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.FacilityRepository;
import com.queuesense.repository.ResourceItemRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import com.queuesense.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SportsInventoryService {

    private final FacilityRepository facilityRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final UsageRequestRepository usageRequestRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private java.time.Clock clock;

    private static final List<RequestStatus> ACTIVE_REQUEST_STATUSES = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.APPROVED,
            RequestStatus.CHECKED_IN
    );

    public SportsInventoryService(FacilityRepository facilityRepository,
                                  ResourceRepository resourceRepository,
                                  ResourceItemRepository resourceItemRepository,
                                  UsageRequestRepository usageRequestRepository,
                                  UserRepository userRepository,
                                  AuditLogService auditLogService,
                                  java.time.Clock clock) {
        this.facilityRepository = facilityRepository;
        this.resourceRepository = resourceRepository;
        this.resourceItemRepository = resourceItemRepository;
        this.usageRequestRepository = usageRequestRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    public void setClock(java.time.Clock clock) {
        this.clock = clock;
    }

    public java.time.Clock getClock() {
        return this.clock;
    }

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now(this.clock);
    }

    @PostConstruct
    @Transactional
    public void initializeSportsInventory() {
        Optional<Facility> sportsOpt = facilityRepository.findAll().stream()
                .filter(f -> "SPORTS".equalsIgnoreCase(f.getType()) || "Sports Complex".equalsIgnoreCase(f.getName()))
                .findFirst();

        if (sportsOpt.isEmpty()) {
            return;
        }

        Facility sports = sportsOpt.get();

        // 1. Deactivate obsolete basketball court safely (never offered for new bookings)
        resourceRepository.findByFacilityAndActiveTrue(sports).stream()
                .filter(r -> "Basketball Court".equalsIgnoreCase(r.getName()))
                .forEach(r -> {
                    r.setActive(false);
                    resourceRepository.save(r);
                });

        // 2. Define the exact requested Sports Complex inventory specifications
        class Spec {
            final String name;
            final String type; // "EQUIPMENT" or "COURT"
            final String unit;
            final int initialQty;
            final String prefix;

            Spec(String name, String type, String unit, int initialQty, String prefix) {
                this.name = name;
                this.type = type;
                this.unit = unit;
                this.initialQty = initialQty;
                this.prefix = prefix;
            }
        }

        List<Spec> specs = Arrays.asList(
                // A. Individually issuable equipment
                new Spec("Basketball", "EQUIPMENT", "One basketball per unit", 5, "BB"),
                new Spec("Volleyball", "EQUIPMENT", "One volleyball per unit", 5, "VB"),
                new Spec("Chess board", "EQUIPMENT", "One chess board per unit", 3, "CB"),
                new Spec("Carrom board", "EQUIPMENT", "One carrom board per unit", 2, "CAR"),
                new Spec("Badminton racket and shuttlecock set", "EQUIPMENT", "One complete set per unit", 3, "BRS"),

                // B. Bookable courts and facilities
                new Spec("Badminton court", "COURT", "One court", 2, "BC"),
                new Spec("Tennis court", "COURT", "One court", 1, "TC"),
                new Spec("Padel court", "COURT", "One court", 1, "PC"),
                new Spec("Pickleball court", "COURT", "One court", 1, "PK"),
                new Spec("Cricket nets", "COURT", "One net", 4, "CN")
        );

        for (Spec spec : specs) {
            Resource resource = resourceRepository.findByFacilityAndNameIgnoreCase(sports, spec.name)
                    .orElseGet(() -> {
                        Resource newR = new Resource(sports, spec.name, spec.type, spec.unit, ResourceStatus.AVAILABLE);
                        return resourceRepository.save(newR);
                    });

            // Update metadata if needed
            boolean changed = false;
            if (!spec.type.equalsIgnoreCase(resource.getType())) {
                resource.setType(spec.type);
                changed = true;
            }
            if (resource.getBookingUnit() == null || !resource.getBookingUnit().equals(spec.unit)) {
                resource.setBookingUnit(spec.unit);
                changed = true;
            }
            if (!resource.isActive()) {
                resource.setActive(true);
                changed = true;
            }
            if (changed) {
                resource = resourceRepository.save(resource);
            }

            // Check if physical items have ever been initialized for this resource
            if (!resourceItemRepository.existsByResource(resource)) {
                // Initialize exact initial items once
                for (int i = 1; i <= spec.initialQty; i++) {
                    String itemCode = spec.prefix + String.format("-%02d", i);
                    String itemName = spec.name + " #" + i;
                    ResourceItem item = new ResourceItem(resource, i, itemCode, itemName, "AVAILABLE");
                    resourceItemRepository.save(item);
                }
            }
        }
    }

    public Facility getSportsFacility() {
        return facilityRepository.findAll().stream()
                .filter(f -> "SPORTS".equalsIgnoreCase(f.getType()) || "Sports Complex".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Facility sc = new Facility("Sports Complex", "SPORTS", "Campus athletic complex featuring indoor and outdoor courts");
                    return facilityRepository.save(sc);
                });
    }

    public SportsStudentAvailabilityResponseDto getStudentAvailability(String studentEmail) {
        Facility sports = getSportsFacility();
        SportsStudentAvailabilityResponseDto response = new SportsStudentAvailabilityResponseDto();

        // 1. Check if student has an active Sports Complex booking
        if (studentEmail != null) {
            userRepository.findByEmail(studentEmail).ifPresent(student -> {
                // Auto-expire stale approved requests
                List<UsageRequest> studentRequests = usageRequestRepository
                        .findByStudentAndFacilityAndStatusIn(student, sports, ACTIVE_REQUEST_STATUSES);
                LocalDateTime now = LocalDateTime.now();
                for (UsageRequest req : studentRequests) {
                    if (req.getStatus() == RequestStatus.APPROVED && req.getExpiresAt() != null && now.isAfter(req.getExpiresAt())) {
                        req.setStatus(RequestStatus.EXPIRED);
                        usageRequestRepository.save(req);
                    }
                }

                Optional<UsageRequest> activeReq = usageRequestRepository
                        .findFirstByStudentAndFacilityAndStatusInOrderByCreatedAtDesc(student, sports, ACTIVE_REQUEST_STATUSES);
                if (activeReq.isPresent()) {
                    response.setHasActiveSportsBooking(true);
                    response.setActiveBookingCode(activeReq.get().getRequestCode());
                    if (activeReq.get().getResource() != null) {
                        response.setActiveBookingResourceName(activeReq.get().getResource().getName());
                    }
                    response.setActiveBookingStatus(activeReq.get().getStatus().name());
                }
            });
        }

        // 2. Compute live resource availability
        List<Resource> activeResources = resourceRepository.findByFacilityAndActiveTrueOrderByIdAsc(sports);
        List<SportsResourceDto> equipment = new ArrayList<>();
        List<SportsResourceDto> courtsAndNets = new ArrayList<>();

        for (Resource resource : activeResources) {
            SportsResourceDto dto = calculateResourceInventory(resource);
            if ("EQUIPMENT".equalsIgnoreCase(resource.getType())) {
                equipment.add(dto);
            } else {
                courtsAndNets.add(dto);
            }
        }

        response.setEquipment(equipment);
        response.setCourtsAndNets(courtsAndNets);
        return response;
    }

    public List<SportsInventoryDetailDto> getAllInventoryDetails() {
        Facility sports = getSportsFacility();
        List<Resource> resources = resourceRepository.findByFacilityAndActiveTrueOrderByIdAsc(sports);
        List<SportsInventoryDetailDto> result = new ArrayList<>();

        for (Resource resource : resources) {
            SportsResourceDto resDto = calculateResourceInventory(resource);
            List<ResourceItemDto> itemDtos = resourceItemRepository
                    .findByResourceAndActiveTrueOrderByUnitNumberAsc(resource)
                    .stream()
                    .map(ResourceItemDto::fromEntity)
                    .collect(Collectors.toList());

            result.add(new SportsInventoryDetailDto(resDto, itemDtos));
        }

        return result;
    }

    public SportsResourceDto calculateResourceInventory(Resource resource) {
        SportsResourceDto dto = new SportsResourceDto();
        dto.setId(resource.getId());
        dto.setName(resource.getName());
        dto.setType(resource.getType());
        dto.setBookingUnit(resource.getBookingUnit());
        dto.setBookingClosed(resource.isBookingClosed());
        dto.setClosureReason(resource.getClosureReason());

        List<ResourceItem> items = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(resource);
        int totalQty = items.size();
        int unavailableQty = (int) items.stream()
                .filter(it -> !"AVAILABLE".equalsIgnoreCase(it.getStatus()))
                .count();
        int usableQty = Math.max(0, totalQty - unavailableQty);

        // Sum reserved or issued quantities in active requests (auto-expiring stale holds)
        List<UsageRequest> activeRequests = usageRequestRepository
                .findByResourceAndStatusIn(resource, ACTIVE_REQUEST_STATUSES);
        LocalDateTime now = getCurrentTime();
        List<UsageRequest> validActiveRequests = new ArrayList<>();
        boolean isCourt = "COURT".equalsIgnoreCase(resource.getType());

        for (UsageRequest req : activeRequests) {
            LocalDateTime scheduledEnd = req.getSlotEndTime();
            if (scheduledEnd == null && req.getCheckInAt() != null) {
                scheduledEnd = req.getCheckInAt().plus(BookingLifecycleService.parseDuration(req.getExpectedDuration()));
            }

            if (req.getStatus() == RequestStatus.APPROVED && req.getExpiresAt() != null && now.isAfter(req.getExpiresAt())) {
                req.setStatus(RequestStatus.EXPIRED);
                usageRequestRepository.save(req);
            } else if (scheduledEnd != null && (now.isAfter(scheduledEnd) || now.isEqual(scheduledEnd))) {
                if (req.getStatus() == RequestStatus.CHECKED_IN) {
                    req.setStatus(RequestStatus.COMPLETED);
                    req.setCheckOutAt(scheduledEnd);
                } else {
                    req.setStatus(RequestStatus.EXPIRED);
                }
                usageRequestRepository.save(req);
            } else {
                if (isCourt && req.getSlotStartTime() != null && req.getSlotEndTime() != null) {
                    // Overlaps with current rolling hour
                    if (req.getSlotStartTime().isBefore(now.plusMinutes(30)) && req.getSlotEndTime().isAfter(now)) {
                        validActiveRequests.add(req);
                    }
                } else {
                    validActiveRequests.add(req);
                }
            }
        }
        int reservedQty = validActiveRequests.stream()
                .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 1)
                .sum();

        int availableQty = resource.isBookingClosed() ? 0 : Math.max(0, usableQty - reservedQty);

        dto.setTotalQuantity(totalQty);
        dto.setUnavailableQuantity(unavailableQty);
        dto.setUsableQuantity(usableQty);
        dto.setReservedQuantity(reservedQty);
        dto.setAvailableQuantity(availableQty);

        // Status text derivation matching requirements
        String statusText;
        ResourceStatus overallStatus;
        if (resource.isBookingClosed()) {
            statusText = "Currently unavailable";
            overallStatus = ResourceStatus.MAINTENANCE;
        } else if (usableQty == 0 && totalQty > 0) {
            statusText = "COURT".equalsIgnoreCase(resource.getType()) ? "Under maintenance" : "Out of stock";
            overallStatus = ResourceStatus.MAINTENANCE;
        } else if (availableQty == 0) {
            statusText = "Fully booked";
            overallStatus = ResourceStatus.OCCUPIED;
        } else if (availableQty < totalQty) {
            statusText = "Partially available";
            overallStatus = ResourceStatus.AVAILABLE;
        } else {
            statusText = "Available";
            overallStatus = ResourceStatus.AVAILABLE;
        }

        dto.setStatusText(statusText);
        dto.setStatus(overallStatus);
        return dto;
    }

    @Transactional
    public SportsResourceDto updateTotalQuantity(Long resourceId, int newTotal, String operatorEmail) {
        if (newTotal < 0) {
            throw new InvalidStateTransitionException("Total inventory quantity cannot be negative.");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        List<ResourceItem> activeItems = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(resource);
        int currentTotal = activeItems.size();

        if (newTotal == currentTotal) {
            return calculateResourceInventory(resource);
        }

        // Active reservations validation
        List<UsageRequest> activeRequests = usageRequestRepository
                .findByResourceAndStatusIn(resource, ACTIVE_REQUEST_STATUSES);
        int activeReservedQty = activeRequests.stream()
                .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 1)
                .sum();

        if (newTotal < activeReservedQty) {
            throw new InvalidStateTransitionException("Cannot reduce total inventory to " + newTotal +
                    ". There are currently " + activeReservedQty + " physical items reserved or issued to students.");
        }

        if (newTotal > currentTotal) {
            // Expand inventory
            int highestUnitNumber = activeItems.stream().mapToInt(ResourceItem::getUnitNumber).max().orElse(0);
            String prefix = derivePrefix(resource.getName());

            for (int i = 1; i <= (newTotal - currentTotal); i++) {
                int unitNum = highestUnitNumber + i;
                String itemCode = prefix + String.format("-%02d", unitNum);
                String itemName = resource.getName() + " #" + unitNum;
                ResourceItem item = new ResourceItem(resource, unitNum, itemCode, itemName, "AVAILABLE");
                resourceItemRepository.save(item);
            }
        } else {
            // Reduce inventory: only remove available (unreserved & undamaged) items starting from highest unit number
            int toRemove = currentTotal - newTotal;
            List<ResourceItem> candidates = activeItems.stream()
                    .filter(it -> "AVAILABLE".equalsIgnoreCase(it.getStatus()))
                    .sorted((a, b) -> Integer.compare(b.getUnitNumber(), a.getUnitNumber()))
                    .collect(Collectors.toList());

            if (candidates.size() < toRemove) {
                throw new InvalidStateTransitionException("Cannot reduce inventory by " + toRemove +
                        ": only " + candidates.size() + " items are currently unreserved and available to decommission.");
            }

            for (int i = 0; i < toRemove; i++) {
                ResourceItem item = candidates.get(i);
                item.setActive(false);
                resourceItemRepository.save(item);
            }
        }

        String userRole = userRepository.findByEmail(operatorEmail).map(u -> u.getRole().name()).orElse("ROLE_OPERATOR");
        auditLogService.log(operatorEmail, userRole, "INVENTORY_QUANTITY_CHANGED", "Resource",
                resourceId.toString(), "Changed total inventory of " + resource.getName() + " from " + currentTotal + " to " + newTotal);

        return calculateResourceInventory(resource);
    }

    @Transactional
    public ResourceItemDto updateItemStatus(Long itemId, UpdateItemStatusDto dto, String operatorEmail) {
        ResourceItem item = resourceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource item not found with ID: " + itemId));

        String oldStatus = item.getStatus();
        String newStatus = dto.getStatus().trim().toUpperCase();

        if (!Arrays.asList("AVAILABLE", "DAMAGED", "MAINTENANCE", "LOST").contains(newStatus)) {
            throw new InvalidStateTransitionException("Invalid item status: " + newStatus +
                    ". Must be one of AVAILABLE, DAMAGED, MAINTENANCE, LOST.");
        }

        item.setStatus(newStatus);
        if (dto.getConditionNotes() != null) {
            item.setConditionNotes(dto.getConditionNotes());
        }
        ResourceItem saved = resourceItemRepository.save(item);

        String userRole = userRepository.findByEmail(operatorEmail).map(u -> u.getRole().name()).orElse("ROLE_OPERATOR");
        auditLogService.log(operatorEmail, userRole, "ITEM_STATUS_CHANGED", "ResourceItem",
                itemId.toString(), "Changed " + item.getName() + " (" + item.getItemCode() + ") status from " +
                        oldStatus + " to " + newStatus + (item.getConditionNotes() != null ? " (" + item.getConditionNotes() + ")" : ""));

        return ResourceItemDto.fromEntity(saved);
    }

    @Transactional
    public SportsResourceDto toggleBookingClosure(Long resourceId, boolean closed, String operatorEmail) {
        return toggleBookingClosure(resourceId, closed, null, operatorEmail);
    }

    @Transactional
    public SportsResourceDto toggleBookingClosure(Long resourceId, boolean closed, String reason, String operatorEmail) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        resource.setBookingClosed(closed);
        resource.setClosureReason(closed ? (reason != null && !reason.trim().isEmpty() ? reason.trim() : null) : null);
        Resource saved = resourceRepository.save(resource);

        String userRole = userRepository.findByEmail(operatorEmail).map(u -> u.getRole().name()).orElse("ROLE_OPERATOR");
        auditLogService.log(operatorEmail, userRole, closed ? "RESOURCE_BOOKINGS_CLOSED" : "RESOURCE_BOOKINGS_REOPENED",
                "Resource", resourceId.toString(),
                (closed ? "Closed" : "Reopened") + " bookings for " + saved.getName() + (saved.getClosureReason() != null ? " (Reason: " + saved.getClosureReason() + ")" : ""));

        return calculateResourceInventory(saved);
    }

    public List<CourtSlotDto> getCourtSlots(Long resourceId, LocalDateTime currentTime) {
        LocalDateTime now = currentTime != null ? currentTime : getCurrentTime();
        Facility sports = getSportsFacility();
        List<Resource> courtResources;
        if (resourceId != null) {
            courtResources = resourceRepository.findById(resourceId).stream()
                    .filter(r -> "COURT".equalsIgnoreCase(r.getType()) && r.isActive())
                    .collect(Collectors.toList());
        } else {
            courtResources = resourceRepository.findByFacilityAndActiveTrueOrderByIdAsc(sports).stream()
                    .filter(r -> "COURT".equalsIgnoreCase(r.getType()))
                    .collect(Collectors.toList());
        }

        LocalDate today = now.toLocalDate();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        List<CourtSlotDto> slots = new ArrayList<>();

        for (Resource court : courtResources) {
            List<ResourceItem> items = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(court);

            for (int hour = 6; hour < 22; hour++) {
                LocalDateTime slotStart = today.atTime(hour, 0);
                LocalDateTime slotEnd = today.atTime(hour + 1, 0);
                String slotLabel = slotStart.format(timeFormatter) + " - " + slotEnd.format(timeFormatter);

                for (ResourceItem item : items) {
                    CourtSlotDto dto = new CourtSlotDto();
                    dto.setResourceId(court.getId());
                    dto.setResourceName(court.getName());
                    dto.setResourceItemId(item.getId());
                    dto.setResourceItemName(item.getName());
                    dto.setItemCode(item.getItemCode());
                    dto.setSlotStartTime(slotStart);
                    dto.setSlotEndTime(slotEnd);
                    dto.setSlotLabel(slotLabel);

                    LocalDateTime bookingOpensAt = slotStart.minusMinutes(30);
                    dto.setBookingOpensAt(bookingOpensAt);

                    if (court.isBookingClosed()) {
                        dto.setStatus("MAINTENANCE");
                        dto.setCanBook(false);
                        dto.setNotes(court.getClosureReason() != null ? court.getClosureReason() : "Court bookings closed");
                    } else if (!"AVAILABLE".equalsIgnoreCase(item.getStatus())) {
                        dto.setStatus("MAINTENANCE");
                        dto.setCanBook(false);
                        dto.setNotes(item.getName() + " is under " + item.getStatus().toLowerCase());
                    } else if (slotEnd.isBefore(now) || slotEnd.isEqual(now)) {
                        dto.setStatus("PASSED");
                        dto.setCanBook(false);
                        dto.setNotes("Slot completed");
                    } else {
                        List<UsageRequest> overlaps = usageRequestRepository.findOverlappingRequestsForItem(
                                item.getId(), slotStart, slotEnd, ACTIVE_REQUEST_STATUSES);

                        if (!overlaps.isEmpty()) {
                            UsageRequest activeReq = overlaps.get(0);
                            if (activeReq.getStatus() == RequestStatus.CHECKED_IN) {
                                dto.setStatus("OCCUPIED");
                                dto.setNotes("Currently occupied (" + activeReq.getRequestCode() + ")");
                            } else {
                                dto.setStatus("RESERVED");
                                dto.setNotes("Reserved / Pending (" + activeReq.getRequestCode() + ")");
                            }
                            dto.setCanBook(false);
                        } else if (slotStart.isAfter(now.plusMinutes(30))) {
                            dto.setStatus("UPCOMING");
                            dto.setCanBook(false);
                            dto.setNotes("Booking opens at " + bookingOpensAt.format(timeFormatter));
                        } else {
                            dto.setStatus("AVAILABLE_TO_BOOK");
                            dto.setCanBook(true);
                            dto.setNotes("Available to book now");
                        }
                    }

                    slots.add(dto);
                }
            }
        }

        return slots;
    }

    private String derivePrefix(String name) {
        String[] words = name.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "EQ";
    }
}

