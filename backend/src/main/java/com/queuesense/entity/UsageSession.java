package com.queuesense.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_sessions")
public class UsageSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usage_request_id", nullable = false, unique = true)
    private UsageRequest usageRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long actualDurationMinutes;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED

    public UsageSession() {}

    public UsageSession(UsageRequest usageRequest, Resource resource, User student) {
        this.usageRequest = usageRequest;
        this.resource = resource;
        this.student = student;
        this.startedAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UsageRequest getUsageRequest() { return usageRequest; }
    public void setUsageRequest(UsageRequest usageRequest) { this.usageRequest = usageRequest; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public Long getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(Long actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
