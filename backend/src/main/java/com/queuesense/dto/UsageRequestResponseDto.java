package com.queuesense.dto;

import com.queuesense.entity.UsageRequest;
import com.queuesense.enums.RequestStatus;
import java.time.LocalDateTime;

public class UsageRequestResponseDto {
    private Long id;
    private String requestCode;
    private Long studentUserId;
    private String studentName;
    private String studentEmail;
    private String studentIdCode;
    private Long resourceId;
    private String resourceName;
    private String resourceType;
    private Long facilityId;
    private String facilityName;
    private String expectedArrival;
    private String expectedDuration;
    private Integer groupSize;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDateTime expiresAt;

    public static UsageRequestResponseDto fromEntity(UsageRequest request, String studentIdCode) {
        UsageRequestResponseDto dto = new UsageRequestResponseDto();
        dto.setId(request.getId());
        dto.setRequestCode(request.getRequestCode());
        if (request.getStudent() != null) {
            dto.setStudentUserId(request.getStudent().getId());
            dto.setStudentName(request.getStudent().getName());
            dto.setStudentEmail(request.getStudent().getEmail());
        }
        dto.setStudentIdCode(studentIdCode);
        if (request.getResource() != null) {
            dto.setResourceId(request.getResource().getId());
            dto.setResourceName(request.getResource().getName());
            dto.setResourceType(request.getResource().getType());
        }
        if (request.getFacility() != null) {
            dto.setFacilityId(request.getFacility().getId());
            dto.setFacilityName(request.getFacility().getName());
        }
        dto.setExpectedArrival(request.getExpectedArrival());
        dto.setExpectedDuration(request.getExpectedDuration());
        dto.setGroupSize(request.getGroupSize());
        dto.setStatus(request.getStatus());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setApprovedAt(request.getApprovedAt());
        dto.setRejectedAt(request.getRejectedAt());
        dto.setRejectionReason(request.getRejectionReason());
        dto.setCheckInAt(request.getCheckInAt());
        dto.setCheckOutAt(request.getCheckOutAt());
        dto.setExpiresAt(request.getExpiresAt());
        return dto;
    }

    public UsageRequestResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentIdCode() { return studentIdCode; }
    public void setStudentIdCode(String studentIdCode) { this.studentIdCode = studentIdCode; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String expectedDuration) { this.expectedDuration = expectedDuration; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
