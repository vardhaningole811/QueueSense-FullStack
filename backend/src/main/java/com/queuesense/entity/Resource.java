package com.queuesense.entity;

import com.queuesense.enums.ResourceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // e.g. "SEAT", "PC", "COURT"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String bookingUnit; // e.g. "One basketball per unit", "One court", "One net"

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean bookingClosed = false;

    @Column(length = 255)
    private String closureReason;

    public Resource() {}

    public Resource(Facility facility, String name, String type, ResourceStatus status) {
        this.facility = facility;
        this.name = name;
        this.type = type;
        this.status = status;
        this.active = true;
    }

    public Resource(Facility facility, String name, String type, String bookingUnit, ResourceStatus status) {
        this.facility = facility;
        this.name = name;
        this.type = type;
        this.bookingUnit = bookingUnit;
        this.status = status;
        this.active = true;
        this.bookingClosed = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getBookingUnit() { return bookingUnit; }
    public void setBookingUnit(String bookingUnit) { this.bookingUnit = bookingUnit; }

    public boolean isBookingClosed() { return bookingClosed; }
    public void setBookingClosed(boolean bookingClosed) { this.bookingClosed = bookingClosed; }

    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }
}
