package com.queuesense.service;

import com.queuesense.dto.CreateUsageRequestDto;
import com.queuesense.dto.RejectRequestDto;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsageRequestService {

    private final UsageRequestRepository usageRequestRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditLogService auditLogService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.booking.grace-period-minutes:10}")
    private int gracePeriodMinutes;

    public UsageRequestService(UsageRequestRepository usageRequestRepository,
                               ResourceRepository resourceRepository,
                               UserRepository userRepository,
                               StudentProfileRepository studentProfileRepository,
                               AuditLogService auditLogService) {
        this.usageRequestRepository = usageRequestRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UsageRequestResponseDto createRequest(CreateUsageRequestDto dto, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found: " + studentEmail));

        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + dto.getResourceId()));

        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new InvalidStateTransitionException("Cannot request resource: Resource is currently " + resource.getStatus());
        }

        // Prevent duplicate active requests for the student
        List<RequestStatus> activeStatuses = Arrays.asList(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.CHECKED_IN);
        if (usageRequestRepository.existsByStudentAndStatusIn(student, activeStatuses)) {
            throw new DuplicateRequestException("You already have an active or pending request. Please complete or cancel it before submitting another.");
        }

        UsageRequest request = new UsageRequest();
        request.setRequestCode(generateRequestCode());
        request.setStudent(student);
        request.setResource(resource);
        request.setFacility(resource.getFacility());
        request.setExpectedArrival(dto.getExpectedArrival());
        request.setExpectedDuration(dto.getExpectedDuration());
        request.setGroupSize(dto.getGroupSize() != null ? dto.getGroupSize() : 1);
        request.setStatus(RequestStatus.PENDING);

        UsageRequest saved = usageRequestRepository.save(request);

        auditLogService.log(student.getEmail(), student.getRole().name(), "REQUEST_CREATED", "UsageRequest",
                saved.getId().toString(), "Request created for resource " + resource.getName() + " with code " + saved.getRequestCode());

        return mapToDto(saved);
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
        UsageRequest request = usageRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with ID: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStateTransitionException("Cannot approve request: Status must be PENDING, but is " + request.getStatus());
        }

        Resource resource = request.getResource();
        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new InvalidStateTransitionException("Cannot approve request: Resource " + resource.getName() + " is no longer AVAILABLE (current status: " + resource.getStatus() + ")");
        }

        // Atomically reserve resource and approve request
        resource.setStatus(ResourceStatus.RESERVED);
        resourceRepository.save(resource);

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedAt(now);
        request.setExpiresAt(now.plusMinutes(gracePeriodMinutes));

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
        request.setRejectedAt(LocalDateTime.now());
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

        // If approved, release the reserved resource back to AVAILABLE
        if (request.getStatus() == RequestStatus.APPROVED) {
            Resource resource = request.getResource();
            if (resource.getStatus() == ResourceStatus.RESERVED) {
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
