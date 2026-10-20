package com.queuesense.entity;

import com.queuesense.enums.RequestStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_requests")
public class UsageRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String requestCode; // e.g. "QS-2026-00124"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_item_id")
    private ResourceItem resourceItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false, length = 50)
    private String expectedArrival; // e.g. "10:30 AM" or "2026-09-20 10:30"

    @Column(nullable = false, length = 50)
    private String expectedDuration; // e.g. "2 hours"

    @Column(nullable = false)
    private Integer groupSize = 1;

    @Column(nullable = false)
    @ColumnDefault("1")
    private Integer quantity = 1;

    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;

    @Column(length = 255)
    private String rejectionReason;

    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UsageRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }

    public String getExpectedArrival() { return expectedArrival; }
    public void setExpectedArrival(String expectedArrival) { this.expectedArrival = expectedArrival; }

    public String getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(String expectedDuration) { this.expectedDuration = expectedDuration; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }

    public Integer getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = (quantity != null && quantity > 0) ? quantity : 1; }

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

    public ResourceItem getResourceItem() { return resourceItem; }
    public void setResourceItem(ResourceItem resourceItem) { this.resourceItem = resourceItem; }

    public LocalDateTime getSlotStartTime() { return slotStartTime; }
    public void setSlotStartTime(LocalDateTime slotStartTime) { this.slotStartTime = slotStartTime; }

    public LocalDateTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalDateTime slotEndTime) { this.slotEndTime = slotEndTime; }
}

