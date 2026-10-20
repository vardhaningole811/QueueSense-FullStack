package com.queuesense.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_items")
public class ResourceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false)
    private Integer unitNumber;

    @Column(nullable = false, length = 50)
    private String itemCode; // e.g. "BB-01", "CRT-01", "NET-01"

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Basketball #1", "Badminton Court 1"

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE, DAMAGED, MAINTENANCE, LOST

    @Column(length = 255)
    private String conditionNotes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ResourceItem() {}

    public ResourceItem(Resource resource, Integer unitNumber, String itemCode, String name, String status) {
        this.resource = resource;
        this.unitNumber = unitNumber;
        this.itemCode = itemCode;
        this.name = name;
        this.status = status;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
