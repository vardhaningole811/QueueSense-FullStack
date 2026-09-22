package com.queuesense.service;

import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.dto.VerifyCheckInDto;
import com.queuesense.entity.*;
import com.queuesense.enums.CheckInMethod;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.exception.ResourceNotFoundException;
import com.queuesense.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CheckInService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final CheckInRepository checkInRepository;
    private final UsageSessionRepository usageSessionRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;

    public CheckInService(UsageRequestRepository usageRequestRepository,
                          ResourceRepository resourceRepository,
                          CheckInRepository checkInRepository,
                          UsageSessionRepository usageSessionRepository,
                          UserRepository userRepository,
                          StudentProfileRepository studentProfileRepository,
                          AuditLogService auditLogService) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.checkInRepository = checkInRepository;
        this.usageSessionRepository = usageSessionRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UsageRequestResponseDto verifyAndCheckIn(VerifyCheckInDto verifyDto, String operatorEmail) {
        User operator = userRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found: " + operatorEmail));

        UsageRequest request = usageRequestRepository.findByRequestCode(verifyDto.getRequestCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No request found with code: " + verifyDto.getRequestCode()));

        // Validation 1: status must be APPROVED
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new InvalidStateTransitionException("Check-in failed: Request status is " + request.getStatus() + ", but must be APPROVED.");
        }

        // Validation 2: check expiry
        if (request.getExpiresAt() != null && LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(RequestStatus.EXPIRED);
            Resource resource = request.getResource();
            if (resource.getStatus() == ResourceStatus.RESERVED) {
                resource.setStatus(ResourceStatus.AVAILABLE);
                resourceRepository.save(resource);
            }
            usageRequestRepository.save(request);
            throw new InvalidStateTransitionException("Check-in failed: Request has expired. Reservation has been released.");
        }

        // Validation 3: resource status
        Resource resource = request.getResource();

        // Perform Check-in
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(RequestStatus.CHECKED_IN);
        request.setCheckInAt(now);
        UsageRequest savedRequest = usageRequestRepository.save(request);

        // Update resource to OCCUPIED
        resource.setStatus(ResourceStatus.OCCUPIED);
        resourceRepository.save(resource);

        // Create CheckIn record
        CheckInMethod method = verifyDto.getMethod() != null ? verifyDto.getMethod() : CheckInMethod.QR_SCAN;
        CheckIn checkIn = new CheckIn(savedRequest, operator, method);
        checkIn.setCheckedInAt(now);
        checkInRepository.save(checkIn);

        // Create UsageSession record
        UsageSession session = new UsageSession(savedRequest, resource, savedRequest.getStudent());
        session.setStartedAt(now);
        session.setStatus("ACTIVE");
        usageSessionRepository.save(session);

        // Audit log
        auditLogService.log(operatorEmail, "ROLE_OPERATOR", "CHECK_IN", "UsageRequest",
                savedRequest.getId().toString(), "Checked in request " + savedRequest.getRequestCode() + " via " + method);

        String studentIdCode = null;
        if (savedRequest.getStudent() != null) {
            studentIdCode = studentProfileRepository.findByUser(savedRequest.getStudent())
                    .map(StudentProfile::getStudentId)
                    .orElse(null);
        }

        return UsageRequestResponseDto.fromEntity(savedRequest, studentIdCode);
    }
}
