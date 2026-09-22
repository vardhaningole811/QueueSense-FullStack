package com.queuesense.entity;

import com.queuesense.enums.CheckInMethod;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usage_request_id", nullable = false, unique = true)
    private UsageRequest usageRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CheckInMethod method = CheckInMethod.QR_SCAN;

    @Column(nullable = false)
    private LocalDateTime checkedInAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkedInAt == null) {
            this.checkedInAt = LocalDateTime.now();
        }
    }

    public CheckIn() {}

    public CheckIn(UsageRequest usageRequest, User operator, CheckInMethod method) {
        this.usageRequest = usageRequest;
        this.operator = operator;
        this.method = method;
        this.checkedInAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UsageRequest getUsageRequest() { return usageRequest; }
    public void setUsageRequest(UsageRequest usageRequest) { this.usageRequest = usageRequest; }

    public User getOperator() { return operator; }
    public void setOperator(User operator) { this.operator = operator; }

    public CheckInMethod getMethod() { return method; }
    public void setMethod(CheckInMethod method) { this.method = method; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
}
