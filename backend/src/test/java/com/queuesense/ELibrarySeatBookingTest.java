package com.queuesense;

import com.queuesense.dto.*;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.User;
import com.queuesense.enums.CheckInMethod;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.enums.Role;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.repository.*;
import com.queuesense.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ELibrarySeatBookingTest {

    @Autowired
    private ELibraryService eLibraryService;

    @Autowired
    private UsageRequestService usageRequestService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckOutService checkOutService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private UsageRequestRepository usageRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    private User student1;
    private User student2;
    private Facility elibrary;

    @BeforeEach
    void setup() {
        student1 = userRepository.findByEmail("student@queuesense.demo").orElseThrow();

        student2 = userRepository.findByEmail("student2@queuesense.demo").orElseGet(() -> {
            User u = new User("student2@queuesense.demo", passwordEncoder.encode("Student@123"), Role.ROLE_STUDENT, "Pooja Patel");
            u = userRepository.save(u);
            studentProfileRepository.save(new com.queuesense.entity.StudentProfile(u, "STU-2026-002", "Information Technology", 2));
            return u;
        });

        // Cancel any lingering active requests for both students
        for (User u : List.of(student1, student2)) {
            usageRequestRepository.findByStudentOrderByCreatedAtDesc(u).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.CHECKED_IN)
                    .forEach(r -> {
                        r.setStatus(RequestStatus.CANCELLED);
                        usageRequestRepository.save(r);
                    });
        }

        elibrary = facilityRepository.findAll().stream()
                .filter(f -> "E-Library".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElseThrow();

        // Ensure 68 workstations are initialized
        eLibraryService.initializeELibraryWorkstations();
    }

    @Test
    @Order(1)
    @DisplayName("Verify that all 68 workstations are initialized and mapped accurately")
    void testAll68WorkstationsSeeded() {
        ELibraryFloorPlanDto floorPlan = eLibraryService.getFloorPlan("student@queuesense.demo");
        assertNotNull(floorPlan);
        assertEquals(68, floorPlan.getTotalSeats(), "E-Library must have exactly 68 workstations");
        assertEquals(68, floorPlan.getSeats().size(), "Floor plan must contain 68 seat DTOs");

        // Verify Seat 07 is MAINTENANCE
        ELibrarySeatDto seat07 = floorPlan.getSeats().stream()
                .filter(s -> s.getSeatNumber() == 7)
                .findFirst()
                .orElseThrow();
        assertEquals("MAINTENANCE", seat07.getStatus(), "Seat 07 must be in MAINTENANCE status");
        assertFalse(seat07.isBookable(), "Seat 07 must not be bookable");

        // Verify Seat 01 and Workstation 10 exist
        ELibrarySeatDto seat01 = floorPlan.getSeats().stream()
                .filter(s -> s.getSeatNumber() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals(1, seat01.getSeatNumber());

        ELibrarySeatDto seat51 = floorPlan.getSeats().stream()
                .filter(s -> s.getSeatNumber() == 51)
                .findFirst()
                .orElseThrow();
        assertEquals(51, seat51.getSeatNumber(), "Workstation 51 (reconciled from image label 61) must exist");

        ELibrarySeatDto seat68 = floorPlan.getSeats().stream()
                .filter(s -> s.getSeatNumber() == 68)
                .findFirst()
                .orElseThrow();
        assertEquals(68, seat68.getSeatNumber(), "Workstation 68 must exist");
    }

    @Test
    @Order(2)
    @DisplayName("Verify concurrency & conflict prevention: Two students cannot book the same seat for overlapping periods")
    void testPreventConcurrentConflictingBookingsOnSameWorkstation() {
        // Find Workstation 12
        Resource workstation12 = resourceRepository.findAll().stream()
                .filter(r -> r.getFacility().getId().equals(elibrary.getId()) && ELibraryService.extractSeatNumber(r.getName()) == 12)
                .findFirst()
                .orElseThrow();

        workstation12.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(workstation12);

        // 1. Student 1 books Workstation 12
        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(workstation12.getId());
        req1.setExpectedArrival("Immediate (Next 10 mins)");
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(1);

        UsageRequestResponseDto booking1 = usageRequestService.createRequest(req1, student1.getEmail());
        assertNotNull(booking1);
        assertEquals(RequestStatus.PENDING, booking1.getStatus());

        // 2. Check floor plan status: Workstation 12 should now report PENDING
        ELibraryFloorPlanDto floorPlanDuring = eLibraryService.getFloorPlan(student1.getEmail());
        ELibrarySeatDto seat12During = floorPlanDuring.getSeats().stream()
                .filter(s -> s.getSeatNumber() == 12)
                .findFirst()
                .orElseThrow();
        assertEquals("PENDING", seat12During.getStatus());
        assertTrue(seat12During.isMyBooking(), "Student 1 must see this as their own booking");
        assertFalse(seat12During.isBookable(), "Workstation 12 cannot be booked while pending");

        // 3. Student 2 attempts to book the same Workstation 12 while pending: MUST FAIL
        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(workstation12.getId());
        req2.setExpectedArrival("Immediate (Next 10 mins)");
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(1);

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req2, student2.getEmail());
        });
        assertTrue(ex.getMessage().contains("already has a pending booking request") || ex.getMessage().contains("unavailable"),
                "Exception message must clearly state the workstation is unavailable due to existing pending booking");

        // Clean up
        usageRequestService.cancelRequest(booking1.getId(), student1.getEmail());
    }

    @Test
    @Order(3)
    @DisplayName("Verify full workstation booking lifecycle: Request -> Operator Approval -> QR Check-in -> Checkout -> Released")
    void testFullWorkstationLifecycle() {
        Resource workstation15 = resourceRepository.findAll().stream()
                .filter(r -> r.getFacility().getId().equals(elibrary.getId()) && ELibraryService.extractSeatNumber(r.getName()) == 15)
                .findFirst()
                .orElseThrow();

        workstation15.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(workstation15);

        // 1. Student 1 requests Workstation 15
        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(workstation15.getId());
        req.setExpectedArrival("Immediate (Next 10 mins)");
        req.setExpectedDuration("1 hour");
        req.setGroupSize(1);

        UsageRequestResponseDto booked = usageRequestService.createRequest(req, student1.getEmail());
        assertEquals(RequestStatus.PENDING, booked.getStatus());

        // Floor plan shows PENDING
        ELibrarySeatDto seatPending = eLibraryService.getFloorPlan(student1.getEmail()).getSeats().stream()
                .filter(s -> s.getSeatNumber() == 15).findFirst().orElseThrow();
        assertEquals("PENDING", seatPending.getStatus());

        // 2. Operator approves request
        UsageRequestResponseDto approved = usageRequestService.approveRequest(booked.getId(), "operator@queuesense.demo");
        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        // Floor plan shows RESERVED
        ELibrarySeatDto seatReserved = eLibraryService.getFloorPlan(student1.getEmail()).getSeats().stream()
                .filter(s -> s.getSeatNumber() == 15).findFirst().orElseThrow();
        assertEquals("RESERVED", seatReserved.getStatus());
        assertFalse(seatReserved.isBookable());

        // 3. QR Check-in by operator
        VerifyCheckInDto checkInDto = new VerifyCheckInDto(booked.getRequestCode(), CheckInMethod.QR_SCAN);
        UsageRequestResponseDto checkedIn = checkInService.verifyAndCheckIn(checkInDto, "operator@queuesense.demo");
        assertEquals(RequestStatus.CHECKED_IN, checkedIn.getStatus());

        // Floor plan shows OCCUPIED
        ELibrarySeatDto seatOccupied = eLibraryService.getFloorPlan(student1.getEmail()).getSeats().stream()
                .filter(s -> s.getSeatNumber() == 15).findFirst().orElseThrow();
        assertEquals("OCCUPIED", seatOccupied.getStatus());
        assertFalse(seatOccupied.isBookable());

        // 4. Student checks out
        UsageRequestResponseDto checkedOut = checkOutService.checkOut(booked.getId(), student1.getEmail(), "ROLE_STUDENT");
        assertEquals(RequestStatus.COMPLETED, checkedOut.getStatus());

        // Floor plan shows AVAILABLE again
        ELibrarySeatDto seatReleased = eLibraryService.getFloorPlan(student1.getEmail()).getSeats().stream()
                .filter(s -> s.getSeatNumber() == 15).findFirst().orElseThrow();
        assertEquals("AVAILABLE", seatReleased.getStatus());
        assertTrue(seatReleased.isBookable(), "Workstation must be bookable again once session is completed");

        // 5. Subsequent student (Student 2) can now book Workstation 15
        CreateUsageRequestDto nextReq = new CreateUsageRequestDto();
        nextReq.setResourceId(workstation15.getId());
        nextReq.setExpectedArrival("In 15 minutes");
        nextReq.setExpectedDuration("2 hours");
        nextReq.setGroupSize(1);

        UsageRequestResponseDto student2Booking = usageRequestService.createRequest(nextReq, student2.getEmail());
        assertNotNull(student2Booking);
        assertEquals(RequestStatus.PENDING, student2Booking.getStatus());

        usageRequestService.cancelRequest(student2Booking.getId(), student2.getEmail());
    }
}
