package com.queuesense.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "operator_profiles")
public class OperatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String employeeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_facility_id")
    private Facility assignedFacility;

    public OperatorProfile() {}

    public OperatorProfile(User user, String employeeId, Facility assignedFacility) {
        this.user = user;
        this.employeeId = employeeId;
        this.assignedFacility = assignedFacility;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Facility getAssignedFacility() { return assignedFacility; }
    public void setAssignedFacility(Facility assignedFacility) { this.assignedFacility = assignedFacility; }
}
