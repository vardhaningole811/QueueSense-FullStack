package com.queuesense.service;

import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.UsageSession;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import com.queuesense.repository.UsageSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BookingLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(BookingLifecycleService.class);

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final AuditLogService auditLogService;
    private Clock clock;

    public BookingLifecycleService(UsageRequestRepository usageRequestRepository,
                                   ResourceRepository resourceRepository,
                                   UsageSessionRepository usageSessionRepository,
                                   AuditLogService auditLogService,
                                   Clock clock) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return this.clock;
    }

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now(clock);
    }

    public static Duration parseDuration(String expectedDuration) {
        if (expectedDuration == null || expectedDuration.trim().isEmpty()) {
            return Duration.ofHours(1);
        }
        String d = expectedDuration.trim().toLowerCase();
        if (d.contains("30 min")) {
            return Duration.ofMinutes(30);
        } else if (d.contains("1.5 hour")) {
            return Duration.ofMinutes(90);
        } else if (d.contains("1 hour")) {
            return Duration.ofHours(1);
        } else if (d.contains("2 hour")) {
            return Duration.ofHours(2);
        } else if (d.contains("3 hour")) {
            return Duration.ofHours(3);
        } else if (d.contains("4 hour")) {
            return Duration.ofHours(4);
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)\\s*(hour|hr|min|minute)");
        java.util.regex.Matcher matcher = pattern.matcher(d);
        if (matcher.find()) {
            double val = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(3);
            if (unit.startsWith("min")) {
                return Duration.ofMinutes((long) val);
            } else {
                return Duration.ofMinutes((long) (val * 60));
            }
        }
        return Duration.ofHours(1);
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void scheduledProcessExpiredBookings() {
        processExpiredBookings(getCurrentTime());
    }

    @Transactional
    public void processExpiredBookings() {
        processExpiredBookings(getCurrentTime());
    }

    @Transactional
    public void processExpiredBookings(LocalDateTime now) {
        // 1. Expire approved reservations whose hold grace period has passed
        List<UsageRequest> staleHolds = usageRequestRepository
                .findByStatusAndExpiresAtLessThanEqual(RequestStatus.APPROVED, now);

        for (UsageRequest req : staleHolds) {
            req.setStatus(RequestStatus.EXPIRED);
            releaseNonSportsResource(req.getResource());
            usageRequestRepository.save(req);
            auditLogService.log("SYSTEM", "SYSTEM", "HOLD_EXPIRED", "UsageRequest",
                    req.getId().toString(), "Hold reservation expired for request " + req.getRequestCode());
            log.info("Auto-expired unverified hold for request: {}", req.getRequestCode());
        }

        // 2. Complete checked-in bookings (both fixed court/net slots and duration-based equipment/library bookings)
        List<UsageRequest> checkedInRequests = usageRequestRepository
                .findByStatusOrderByCreatedAtDesc(RequestStatus.CHECKED_IN);

        for (UsageRequest req : checkedInRequests) {
            LocalDateTime scheduledEnd;
            if (req.getSlotEndTime() != null) {
                // Fixed court/net slot end time (late check-in does not extend it)
                scheduledEnd = req.getSlotEndTime();
            } else if (req.getCheckInAt() != null) {
                Duration duration = parseDuration(req.getExpectedDuration());
                scheduledEnd = req.getCheckInAt().plus(duration);
            } else {
                Duration duration = parseDuration(req.getExpectedDuration());
                scheduledEnd = req.getCreatedAt().plus(duration);
            }

            if (!now.isBefore(scheduledEnd)) {
                req.setStatus(RequestStatus.COMPLETED);
                req.setCheckOutAt(scheduledEnd);
                if (req.getSlotEndTime() == null) {
                    req.setSlotEndTime(scheduledEnd);
                }
                releaseNonSportsResource(req.getResource());
                usageRequestRepository.save(req);

                Optional<UsageSession> sessionOpt = usageSessionRepository.findByUsageRequest(req);
                if (sessionOpt.isPresent()) {
                    UsageSession session = sessionOpt.get();
                    session.setEndedAt(scheduledEnd);
                    long minutes = Duration.between(session.getStartedAt(), scheduledEnd).toMinutes();
                    session.setActualDurationMinutes(minutes > 0 ? minutes : 1);
                    session.setStatus("COMPLETED");
                    usageSessionRepository.save(session);
                }

                auditLogService.log("SYSTEM", "SYSTEM", "AUTO_CHECKOUT", "UsageRequest",
                        req.getId().toString(), "Session auto-completed at scheduled end time (" + scheduledEnd + ") for request " + req.getRequestCode());
                log.info("Auto-completed session for request: {}", req.getRequestCode());
            }
        }

        // 3. Expire pending or approved court/net bookings whose slot end time has passed without check-in
        List<UsageRequest> pastSlots = usageRequestRepository
                .findByStatusInAndSlotEndTimeLessThanEqual(Arrays.asList(RequestStatus.PENDING, RequestStatus.APPROVED), now);

        for (UsageRequest req : pastSlots) {
            req.setStatus(RequestStatus.EXPIRED);
            releaseNonSportsResource(req.getResource());
            usageRequestRepository.save(req);
            auditLogService.log("SYSTEM", "SYSTEM", "SLOT_EXPIRED_NO_SHOW", "UsageRequest",
                    req.getId().toString(), "Court/net booking " + req.getRequestCode() + " expired due to slot end time passed.");
            log.info("Auto-expired no-show slot booking for request: {}", req.getRequestCode());
        }

        // 4. Reconcile non-sports resources (such as PC 04) that have stuck or orphaned OCCUPIED or RESERVED states
        reconcileNonSportsResourceStatuses(now);
    }

    @Transactional
    public void reconcileNonSportsResourceStatuses(LocalDateTime now) {
        List<Resource> nonSportsResources = resourceRepository.findAll().stream()
                .filter(r -> r.isActive() && r.getFacility() != null &&
                        !"SPORTS".equalsIgnoreCase(r.getFacility().getType()) &&
                        !"Sports Complex".equalsIgnoreCase(r.getFacility().getName()))
                .filter(r -> r.getStatus() == ResourceStatus.OCCUPIED || r.getStatus() == ResourceStatus.RESERVED)
                .collect(java.util.stream.Collectors.toList());

        for (Resource res : nonSportsResources) {
            if (res.getStatus() == ResourceStatus.OCCUPIED) {
                List<UsageRequest> activeCheckedIn = usageRequestRepository.findByResourceAndStatus(res, RequestStatus.CHECKED_IN);
                if (activeCheckedIn.isEmpty()) {
                    log.warn("Reconciling orphaned OCCUPIED resource {} (id: {}) with no active checked-in booking. Releasing to AVAILABLE.",
                            res.getName(), res.getId());
                    res.setStatus(ResourceStatus.AVAILABLE);
                    resourceRepository.save(res);
                    auditLogService.log("SYSTEM", "SYSTEM", "ORPHANED_OCCUPANCY_REPAIRED", "Resource",
                            res.getId().toString(), "Repaired orphaned OCCUPIED state for resource " + res.getName() + " with no active session.");
                }
            } else if (res.getStatus() == ResourceStatus.RESERVED) {
                List<UsageRequest> activeApproved = usageRequestRepository.findByResourceAndStatus(res, RequestStatus.APPROVED)
                        .stream()
                        .filter(r -> r.getExpiresAt() == null || !now.isAfter(r.getExpiresAt()))
                        .collect(java.util.stream.Collectors.toList());
                if (activeApproved.isEmpty()) {
                    log.warn("Reconciling orphaned RESERVED resource {} (id: {}) with no active unexpired hold. Releasing to AVAILABLE.",
                            res.getName(), res.getId());
                    res.setStatus(ResourceStatus.AVAILABLE);
                    resourceRepository.save(res);
                    auditLogService.log("SYSTEM", "SYSTEM", "ORPHANED_HOLD_REPAIRED", "Resource",
                            res.getId().toString(), "Repaired orphaned RESERVED state for resource " + res.getName() + " with no active hold.");
                }
            }
        }
    }

    private void releaseNonSportsResource(Resource resource) {
        if (resource == null) return;
        Facility facility = resource.getFacility();
        boolean isSports = facility != null && ("SPORTS".equalsIgnoreCase(facility.getType()) || "Sports Complex".equalsIgnoreCase(facility.getName()));
        if (!isSports && (resource.getStatus() == ResourceStatus.RESERVED || resource.getStatus() == ResourceStatus.OCCUPIED)) {
            resource.setStatus(ResourceStatus.AVAILABLE);
            resourceRepository.save(resource);
        }
    }
}
