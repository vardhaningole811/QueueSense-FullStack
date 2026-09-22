package com.queuesense.service;

import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.entity.*;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CheckOutService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;

    public CheckOutService(UsageRequestRepository usageRequestRepository,
                           ResourceRepository resourceRepository,
                           UsageSessionRepository usageSessionRepository,
                           StudentProfileRepository studentProfileRepository,
                           AuditLogService auditLogService) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UsageRequestResponseDto checkOut(Long requestId, String userEmail, String userRole) {
        UsageRequest request = usageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.CHECKED_IN) {
            throw new InvalidStateTransitionException("Checkout failed: Request status must be CHECKED_IN, but is " + request.getStatus());
        }

        // Complete request
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(RequestStatus.COMPLETED);
        request.setCheckOutAt(now);
        UsageRequest savedRequest = usageRequestRepository.save(request);

        // Release resource back to AVAILABLE
        Resource resource = request.getResource();
        resource.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

        // Close active usage session
        Optional<UsageSession> sessionOpt = usageSessionRepository.findByUsageRequest(savedRequest);
        if (sessionOpt.isPresent()) {
            UsageSession session = sessionOpt.get();
            session.setEndedAt(now);
            long minutes = Duration.between(session.getStartedAt(), now).toMinutes();
            if (minutes == 0) minutes = 1; // minimum 1 minute represented
            session.setActualDurationMinutes(minutes);
            session.setStatus("COMPLETED");
            usageSessionRepository.save(session);
        }

        // Audit log
        auditLogService.log(userEmail, userRole, "CHECK_OUT", "UsageRequest",
                savedRequest.getId().toString(), "Checked out request " + savedRequest.getRequestCode() + ". Resource " + resource.getName() + " is now AVAILABLE.");

        String studentIdCode = null;
        if (savedRequest.getStudent() != null) {
            studentIdCode = studentProfileRepository.findByUser(savedRequest.getStudent())
                    .map(StudentProfile::getStudentId)
                    .orElse(null);
        }

        return UsageRequestResponseDto.fromEntity(savedRequest, studentIdCode);
    }
}
