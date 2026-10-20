package com.queuesense.service;

import com.queuesense.dto.CreateUsageRequestDto;
import com.queuesense.dto.RejectRequestDto;
import com.queuesense.dto.SportsResourceDto;
import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.entity.*;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.DuplicateRequestException;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UsageRequestService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;
    private final SportsInventoryService sportsInventoryService;
    private final BookingLifecycleService bookingLifecycleService;
    private Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.booking.grace-period-minutes:10}")
    private int gracePeriodMinutes;

    public UsageRequestService(UsageRequestRepository usageRequestRepository,
                               ResourceRepository resourceRepository,
                               ResourceItemRepository resourceItemRepository,
                               UserRepository userRepository,
                               StudentProfileRepository studentProfileRepository,
                               AuditLogService auditLogService,
                               SportsInventoryService sportsInventoryService,
                               BookingLifecycleService bookingLifecycleService,
                               Clock clock) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.resourceItemRepository = resourceItemRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
        this.sportsInventoryService = sportsInventoryService;
        this.bookingLifecycleService = bookingLifecycleService;
        this.clock = clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return this.clock;
    }

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now(this.clock);
    }

    @Transactional
    public UsageRequestResponseDto createRequest(CreateUsageRequestDto dto, String studentEmail) {
        LocalDateTime now = getCurrentTime();
        // 1. Process any expired holds or slots first
        bookingLifecycleService.processExpiredBookings(now);

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found: " + studentEmail));

        Resource resource = resourceRepository.findByIdForUpdate(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + dto.getResourceId()));

        Facility facility = resource.getFacility();
        boolean isSports = facility != null && ("SPORTS".equalsIgnoreCase(facility.getType()) || "Sports Complex".equalsIgnoreCase(facility.getName()));
        boolean isCourt = isSports && "COURT".equalsIgnoreCase(resource.getType());

        int requestedQty = (dto.getQuantity() != null && dto.getQuantity() > 0) ? dto.getQuantity() : 1;
        List<RequestStatus> activeStatuses = Arrays.asList(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.CHECKED_IN);

        ResourceItem assignedItem = null;
        LocalDateTime slotStart = null;
        LocalDateTime slotEnd = null;

        if (isSports) {
            if (resource.isBookingClosed()) {
                String closureMsg = (resource.getClosureReason() != null && !resource.getClosureReason().isEmpty())
                        ? "Cannot request " + resource.getName() + ": Bookings are currently closed (" + resource.getClosureReason() + ")."
                        : "Cannot request " + resource.getName() + ": Bookings for this resource are currently closed by facility administration.";
                throw new InvalidStateTransitionException(closureMsg);
            }

            // Rule: Do not allow one student account to have more than one active Sports Complex booking at a time.
            if (usageRequestRepository.existsByStudentAndFacilityAndStatusIn(student, facility, activeStatuses)) {
                throw new DuplicateRequestException("You already have an active Sports Complex booking. Please complete or cancel your existing booking before submitting another.");
            }

            if (isCourt) {
                // Time-slot validation for courts and cricket nets
                LocalDateTime[] slotTimes = resolveSlotTimes(dto, now);
                slotStart = slotTimes[0];
                slotEnd = slotTimes[1];

                long durationMinutes = Duration.between(slotStart, slotEnd).toMinutes();
                if (durationMinutes != 60) {
                    throw new InvalidStateTransitionException("Court and cricket net bookings must be exactly one hour (60 minutes).");
                }
                if (!slotEnd.isAfter(slotStart)) {
                    throw new InvalidStateTransitionException("Slot end time must be after slot start time.");
                }

                // 30-minute booking window rule:
                // A student may book a slot only when slot start time is within the next 30 minutes or less (slotStartTime <= now + 30 mins)
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
                if (slotStart.isAfter(now.plusMinutes(30))) {
                    throw new InvalidStateTransitionException("Booking window not open yet. This slot opens at " +
                            slotStart.minusMinutes(30).format(timeFmt) + " (30 minutes before slot start). Future slots cannot be booked yet.");
                }
                if (slotEnd.isBefore(now) || slotEnd.isEqual(now)) {
                    throw new InvalidStateTransitionException("Cannot book a past time slot.");
                }

                // Resolve physical court/net unit
                if (dto.getResourceItemId() != null) {
                    ResourceItem item = resourceItemRepository.findById(dto.getResourceItemId())
                            .orElseThrow(() -> new ResourceNotFoundException("Court/net unit not found with ID: " + dto.getResourceItemId()));

                    if (!item.getResource().getId().equals(resource.getId())) {
                        throw new InvalidStateTransitionException("Selected unit " + item.getName() + " does not belong to " + resource.getName());
                    }
                    if (!item.isActive() || !"AVAILABLE".equalsIgnoreCase(item.getStatus())) {
                        throw new InvalidStateTransitionException(item.getName() + " is currently under maintenance or unavailable (" + item.getStatus() + ").");
                    }

                    List<UsageRequest> overlaps = usageRequestRepository.findOverlappingRequestsForItem(
                            item.getId(), slotStart, slotEnd, activeStatuses);
                    if (!overlaps.isEmpty()) {
                        throw new InvalidStateTransitionException(item.getName() + " is already reserved or occupied for this time slot (" +
                                slotStart.format(timeFmt) + " - " + slotEnd.format(timeFmt) + ").");
                    }
                    assignedItem = item;
                } else {
                    List<ResourceItem> activeItems = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(resource);
                    for (ResourceItem it : activeItems) {
                        if ("AVAILABLE".equalsIgnoreCase(it.getStatus())) {
                            List<UsageRequest> overlaps = usageRequestRepository.findOverlappingRequestsForItem(
                                    it.getId(), slotStart, slotEnd, activeStatuses);
                            if (overlaps.isEmpty()) {
                                assignedItem = it;
                                break;
                            }
                        }
                    }
                    if (assignedItem == null) {
                        throw new InvalidStateTransitionException("All courts/nets for " + resource.getName() + " are reserved, occupied, or under maintenance for this time slot.");
                    }
                }
                requestedQty = 1;
            } else {
                // Quantity-based equipment check
                SportsResourceDto inv = sportsInventoryService.calculateResourceInventory(resource);
                if (requestedQty > inv.getAvailableQuantity()) {
                    throw new InvalidStateTransitionException("Requested quantity (" + requestedQty + ") exceeds currently available inventory (" + inv.getAvailableQuantity() + ") for " + resource.getName() + ".");
                }
            }
        } else {
            // Non-sports facilities (E-Library, Reading Room)
            if (usageRequestRepository.existsByStudentAndStatusIn(student, activeStatuses)) {
                throw new DuplicateRequestException("You already have an active or pending request. Please complete or cancel it before submitting another.");
            }
            if (resource.getStatus() != ResourceStatus.AVAILABLE) {
                throw new InvalidStateTransitionException("Cannot request resource: Resource is currently " + resource.getStatus());
            }
            List<UsageRequest> existingActiveOnResource = usageRequestRepository.findByResourceAndStatusIn(resource, activeStatuses);
            if (!existingActiveOnResource.isEmpty()) {
                UsageRequest existing = existingActiveOnResource.get(0);
                String reason = existing.getStatus() == RequestStatus.PENDING
                        ? "a pending booking request awaiting operator verification"
                        : "an active reservation (" + existing.getStatus() + ")";
                throw new InvalidStateTransitionException("Workstation " + resource.getName() + " is currently unavailable. It already has " + reason + ".");
            }
        }

        UsageRequest request = new UsageRequest();
        request.setRequestCode(generateRequestCode());
        request.setStudent(student);
        request.setResource(resource);
        request.setResourceItem(assignedItem);
        request.setFacility(resource.getFacility());

        if (isCourt && slotStart != null && slotEnd != null) {
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");
            request.setSlotStartTime(slotStart);
            request.setSlotEndTime(slotEnd);
            request.setExpectedArrival(slotStart.format(tf) + " - " + slotEnd.format(tf));
            request.setExpectedDuration("1 hour");
            request.setQuantity(1);
        } else {
            request.setExpectedArrival(dto.getExpectedArrival());
            request.setExpectedDuration(dto.getExpectedDuration());
            request.setQuantity(requestedQty);
        }

        request.setGroupSize(dto.getGroupSize() != null ? dto.getGroupSize() : 1);
        request.setStatus(RequestStatus.PENDING);

        UsageRequest saved = usageRequestRepository.save(request);

        auditLogService.log(student.getEmail(), student.getRole().name(), "REQUEST_CREATED", "UsageRequest",
                saved.getId().toString(), "Request created for resource " + resource.getName() +
                        (assignedItem != null ? " (" + assignedItem.getName() + ")" : "") +
                        " (qty: " + requestedQty + ") with code " + saved.getRequestCode());

        return mapToDto(saved);
    }

    private LocalDateTime[] resolveSlotTimes(CreateUsageRequestDto dto, LocalDateTime now) {
        if (dto.getSlotStartTime() != null && dto.getSlotEndTime() != null) {
            return new LocalDateTime[]{dto.getSlotStartTime(), dto.getSlotEndTime()};
        }
        if (dto.getSlotStartTime() != null) {
            return new LocalDateTime[]{dto.getSlotStartTime(), dto.getSlotStartTime().plusHours(1)};
        }
        String arrival = dto.getExpectedArrival();
        if (arrival != null && !arrival.trim().isEmpty()) {
            Pattern p = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM|am|pm)?");
            Matcher m = p.matcher(arrival.trim());
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                int minute = Integer.parseInt(m.group(2));
                String ampm = m.group(3);
                if (ampm != null) {
                    if (ampm.equalsIgnoreCase("PM") && hour < 12) hour += 12;
                    if (ampm.equalsIgnoreCase("AM") && hour == 12) hour = 0;
                }
                LocalDate targetDate = now.toLocalDate();
                LocalDateTime start = targetDate.atTime(hour, minute);
                LocalDateTime end = start.plusHours(1);
                return new LocalDateTime[]{start, end};
            }
        }
        int nextHour = now.getMinute() <= 30 ? now.getHour() : now.getHour() + 1;
        LocalDateTime start = now.toLocalDate().atTime(nextHour, 0);
        return new LocalDateTime[]{start, start.plusHours(1)};
    }

    public List<UsageRequestResponseDto> getStudentRequests(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        return usageRequestRepository.findByStudentOrderByCreatedAtDesc(student)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public UsageRequestResponseDto getRequestById(Long id) {
        UsageRequest request = usageRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + id));
        return mapToDto(request);
    }

    public UsageRequestResponseDto getRequestByCode(String requestCode) {
        UsageRequest request = usageRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with code: " + requestCode));
        return mapToDto(request);
    }

    public List<UsageRequestResponseDto> getPendingRequests() {
        return usageRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<UsageRequestResponseDto> getApprovedRequests() {
        return usageRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.APPROVED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsageRequestResponseDto approveRequest(Long requestId, String operatorEmail) {
        LocalDateTime now = getCurrentTime();
        bookingLifecycleService.processExpiredBookings(now);

        UsageRequest request = usageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Cannot approve request: Status must be PENDING, but is " + request.getStatus());
        }

        Resource resource = request.getResource();
        Facility facility = resource.getFacility();
        boolean isSports = facility != null && ("SPORTS".equalsIgnoreCase(facility.getType()) || "Sports Complex".equalsIgnoreCase(facility.getName()));

        if (!isSports) {
            if (resource.getStatus() != ResourceStatus.AVAILABLE) {
                throw new InvalidStateTransitionException("Cannot approve request: Resource " + resource.getName() + " is no longer AVAILABLE (current status: " + resource.getStatus() + ")");
            }
            resource.setStatus(ResourceStatus.RESERVED);
            resourceRepository.save(resource);
        } else {
            if (resource.isBookingClosed()) {
                throw new InvalidStateTransitionException("Cannot approve request: Bookings for " + resource.getName() + " are currently closed.");
            }

            if (request.getSlotStartTime() != null && request.getResourceItem() != null) {
                // Ensure no conflict was approved for the same court/net item in this slot
                List<UsageRequest> conflicting = usageRequestRepository.findOverlappingRequestsForItem(
                        request.getResourceItem().getId(),
                        request.getSlotStartTime(),
                        request.getSlotEndTime(),
                        Arrays.asList(RequestStatus.APPROVED, RequestStatus.CHECKED_IN)
                ).stream().filter(r -> !r.getId().equals(request.getId())).collect(Collectors.toList());

                if (!conflicting.isEmpty()) {
                    throw new InvalidStateTransitionException("Cannot approve request: A conflicting reservation for " +
                            request.getResourceItem().getName() + " is already approved or checked in for this time slot.");
                }
            } else {
                SportsResourceDto inv = sportsInventoryService.calculateResourceInventory(resource);
                List<UsageRequest> activeReqs = usageRequestRepository.findByResourceAndStatusIn(resource, Arrays.asList(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.CHECKED_IN));
                int totalDemand = activeReqs.stream().mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 1).sum();
                if (totalDemand > inv.getUsableQuantity()) {
                    throw new InvalidStateTransitionException("Cannot approve request: Usable inventory for " + resource.getName() +
                            " (" + inv.getUsableQuantity() + ") is insufficient for current demand (" + totalDemand + ").");
                }
            }
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedAt(now);
        if (request.getSlotStartTime() != null) {
            LocalDateTime holdEnd = request.getSlotStartTime().isAfter(now)
                    ? request.getSlotStartTime().plusMinutes(gracePeriodMinutes)
                    : now.plusMinutes(gracePeriodMinutes);
            request.setExpiresAt(holdEnd);
        } else {
            request.setExpiresAt(now.plusMinutes(gracePeriodMinutes));
        }

        UsageRequest saved = usageRequestRepository.save(request);

        auditLogService.log(operatorEmail, "ROLE_OPERATOR", "REQUEST_APPROVED", "UsageRequest",
                saved.getId().toString(), "Approved request " + saved.getRequestCode() + " for resource " + resource.getName());

        return mapToDto(saved);
    }

    @Transactional
    public UsageRequestResponseDto rejectRequest(Long requestId, RejectRequestDto rejectDto, String operatorEmail) {
        UsageRequest request = usageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Cannot reject request: Status must be PENDING, but is " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectedAt(getCurrentTime());
        if (rejectDto != null && rejectDto.getRejectionReason() != null) {
            request.setRejectionReason(rejectDto.getRejectionReason());
        }

        UsageRequest saved = usageRequestRepository.save(request);

        auditLogService.log(operatorEmail, "ROLE_OPERATOR", "REQUEST_REJECTED", "UsageRequest",
                saved.getId().toString(), "Rejected request " + saved.getRequestCode() + ". Reason: " + request.getRejectionReason());

        return mapToDto(saved);
    }

    @Transactional
    public UsageRequestResponseDto cancelRequest(Long requestId, String userEmail) {
        UsageRequest request = usageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));

        if (!request.getStudent().getEmail().equals(userEmail)) {
            throw new InvalidStateTransitionException("You can only cancel your own requests.");
        }

        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.APPROVED) {
            throw new InvalidStateTransitionException("Cannot cancel request with status " + request.getStatus());
        }

        // If approved, release the reserved resource back to AVAILABLE for non-sports
        if (request.getStatus() == RequestStatus.APPROVED) {
            Resource resource = request.getResource();
            Facility facility = resource.getFacility();
            boolean isSports = facility != null && ("SPORTS".equalsIgnoreCase(facility.getType()) || "Sports Complex".equalsIgnoreCase(facility.getName()));
            if (!isSports && resource.getStatus() == ResourceStatus.RESERVED) {
                resource.setStatus(ResourceStatus.AVAILABLE);
                resourceRepository.save(resource);
            }
        }

        request.setStatus(RequestStatus.CANCELLED);
        UsageRequest saved = usageRequestRepository.save(request);

        auditLogService.log(userEmail, request.getStudent().getRole().name(), "REQUEST_CANCELLED", "UsageRequest",
                saved.getId().toString(), "Cancelled request " + saved.getRequestCode());

        return mapToDto(saved);
    }

    private String generateRequestCode() {
        int number = 10000 + random.nextInt(90000);
        return "QS-2026-" + number;
    }

    private UsageRequestResponseDto mapToDto(UsageRequest request) {
        String studentIdCode = null;
        if (request.getStudent() != null) {
            Optional<StudentProfile> profile = studentProfileRepository.findByUser(request.getStudent());
            if (profile.isPresent()) {
                studentIdCode = profile.get().getStudentId();
            }
        }
        return UsageRequestResponseDto.fromEntity(request, studentIdCode);
    }
}
