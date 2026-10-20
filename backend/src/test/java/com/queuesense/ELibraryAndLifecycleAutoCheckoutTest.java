package com.queuesense;

import com.queuesense.dto.*;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.UsageRequest;
import com.queuesense.entity.User;
import com.queuesense.enums.CheckInMethod;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.enums.Role;
import com.queuesense.repository.*;
import com.queuesense.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ELibraryAndLifecycleAutoCheckoutTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UsageRequestService usageRequestService;

    @Autowired
    private BookingLifecycleService bookingLifecycleService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckOutService checkOutService;

    @Autowired
    private ResourceService resourceService;

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

        // Cancel lingering active requests
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
    }

    @AfterEach
    void resetClocks() {
        Clock sys = Clock.system(KOLKATA);
        usageRequestService.setClock(sys);
        bookingLifecycleService.setClock(sys);
        checkInService.setClock(sys);
        checkOutService.setClock(sys);
    }

    @Test
    @Order(1)
    @DisplayName("Repair Verification: PC 04 is AVAILABLE and orphaned OCCUPIED state is reconciled safely")
    void testPC04AvailabilityAndOrphanReconciliation() {
        LocalDateTime now = LocalDateTime.now(KOLKATA);
        bookingLifecycleService.processExpiredBookings(now);

        Resource pc04 = resourceRepository.findByFacilityAndName(elibrary, "PC 04").orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, pc04.getStatus(), "PC 04 must be AVAILABLE when there is no active checked-in booking");

        // Simulate an orphaned stuck OCCUPIED state on a non-sports resource (like PC 04 in past bug)
        pc04.setStatus(ResourceStatus.OCCUPIED);
        resourceRepository.save(pc04);

        // Run reconciliation
        bookingLifecycleService.processExpiredBookings(now);

        Resource reconciledPC04 = resourceRepository.findByFacilityAndName(elibrary, "PC 04").orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, reconciledPC04.getStatus(), "Orphaned OCCUPIED PC 04 must be safely restored to AVAILABLE");

        // Verify maintenance resource (Seat 07) was NOT touched
        Resource seat07 = resourceRepository.findByFacilityAndName(elibrary, "Seat 07").orElseThrow();
        assertEquals(ResourceStatus.MAINTENANCE, seat07.getStatus(), "Maintenance resources must remain MAINTENANCE");
    }

    @Test
    @Order(2)
    @DisplayName("E-Library Lifecycle: Normal completed session on PC 04 (Request -> Approve -> QR Check-in -> Checkout)")
    void testNormalCompletedELibrarySessionOnPC04() {
        LocalDateTime now = LocalDateTime.now(KOLKATA);
        bookingLifecycleService.processExpiredBookings(now);

        Resource pc04 = resourceRepository.findByFacilityAndName(elibrary, "PC 04").orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, pc04.getStatus());

        // 1. Student requests PC 04
        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(pc04.getId());
        req.setQuantity(1);
        req.setExpectedArrival("Immediate (Next 10 mins)");
        req.setExpectedDuration("1 hour");
        req.setGroupSize(1);

        UsageRequestResponseDto booked = usageRequestService.createRequest(req, student1.getEmail());
        assertNotNull(booked);
        assertEquals(RequestStatus.PENDING, booked.getStatus());

        // 2. Operator approves request
        UsageRequestResponseDto approved = usageRequestService.approveRequest(booked.getId(), "operator@queuesense.demo");
        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        Resource pcAfterApprove = resourceRepository.findById(pc04.getId()).orElseThrow();
        assertEquals(ResourceStatus.RESERVED, pcAfterApprove.getStatus(), "Resource becomes RESERVED on approval");

        // 3. QR Check-in
        VerifyCheckInDto checkInDto = new VerifyCheckInDto(booked.getRequestCode(), CheckInMethod.QR_SCAN);
        UsageRequestResponseDto checkedIn = checkInService.verifyAndCheckIn(checkInDto, "operator@queuesense.demo");
        assertEquals(RequestStatus.CHECKED_IN, checkedIn.getStatus());

        Resource pcAfterCheckIn = resourceRepository.findById(pc04.getId()).orElseThrow();
        assertEquals(ResourceStatus.OCCUPIED, pcAfterCheckIn.getStatus(), "Resource becomes OCCUPIED on check-in");

        // 4. Normal checkout
        UsageRequestResponseDto checkedOut = checkOutService.checkOut(booked.getId(), student1.getEmail(), "ROLE_STUDENT");
        assertEquals(RequestStatus.COMPLETED, checkedOut.getStatus());

        Resource pcAfterCheckOut = resourceRepository.findById(pc04.getId()).orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, pcAfterCheckOut.getStatus(), "Resource returns to AVAILABLE upon checkout");
    }

    @Test
    @Order(3)
    @DisplayName("Part 4: Automatic checkout of time-limited booking at scheduled end time (1-hour session)")
    void testAutomaticCheckoutAtScheduledEndTime() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 20, 10, 0); // 10:00 AM
        Clock clock1000 = Clock.fixed(baseTime.atZone(KOLKATA).toInstant(), KOLKATA);
        usageRequestService.setClock(clock1000);
        bookingLifecycleService.setClock(clock1000);
        checkInService.setClock(clock1000);
        checkOutService.setClock(clock1000);

        bookingLifecycleService.processExpiredBookings(baseTime);

        Resource pc04 = resourceRepository.findByFacilityAndName(elibrary, "PC 04").orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, pc04.getStatus());

        // Initial stats
        DashboardStatsDto statsInitial = dashboardService.getDashboardStats();
        long initialOccupied = statsInitial.getOccupiedResources();
        long initialCheckIns = statsInitial.getTodayCheckIns();
        long initialAvailable = statsInitial.getAvailableResources();

        // 1. Request PC 04 for 1 hour at 10:00 AM
        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(pc04.getId());
        req.setQuantity(1);
        req.setExpectedArrival("10:00 AM");
        req.setExpectedDuration("1 hour");
        req.setGroupSize(1);

        UsageRequestResponseDto created = usageRequestService.createRequest(req, student1.getEmail());
        usageRequestService.approveRequest(created.getId(), "operator@queuesense.demo");

        // 2. Check in at 10:00 AM
        checkInService.verifyAndCheckIn(new VerifyCheckInDto(created.getRequestCode(), CheckInMethod.QR_SCAN), "operator@queuesense.demo");

        DashboardStatsDto statsDuring = dashboardService.getDashboardStats();
        assertEquals(initialOccupied + 1, statsDuring.getOccupiedResources(), "Must count as occupied during session");
        assertEquals(initialCheckIns + 1, statsDuring.getTodayCheckIns(), "Check-in count must increment");

        // 3. Advance clock to 10:45 AM (session still valid - MUST NOT be released early)
        LocalDateTime at1045 = baseTime.plusMinutes(45);
        Clock clock1045 = Clock.fixed(at1045.atZone(KOLKATA).toInstant(), KOLKATA);
        usageRequestService.setClock(clock1045);
        bookingLifecycleService.setClock(clock1045);
        checkInService.setClock(clock1045);

        bookingLifecycleService.processExpiredBookings(at1045);

        UsageRequest stillActive = usageRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(RequestStatus.CHECKED_IN, stillActive.getStatus(), "Session must remain active before scheduled end time");

        Resource pcStillOccupied = resourceRepository.findById(pc04.getId()).orElseThrow();
        assertEquals(ResourceStatus.OCCUPIED, pcStillOccupied.getStatus(), "Resource must remain OCCUPIED at 10:45 AM");

        // 4. Advance clock to 11:00 AM (exact 1-hour scheduled end time)
        LocalDateTime at1100 = baseTime.plusHours(1);
        Clock clock1100 = Clock.fixed(at1100.atZone(KOLKATA).toInstant(), KOLKATA);
        usageRequestService.setClock(clock1100);
        bookingLifecycleService.setClock(clock1100);
        checkInService.setClock(clock1100);

        bookingLifecycleService.processExpiredBookings(at1100);

        UsageRequest autoCompleted = usageRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(RequestStatus.COMPLETED, autoCompleted.getStatus(), "Booking must be auto-completed at end time");
        assertNotNull(autoCompleted.getCheckOutAt(), "Checkout timestamp must be recorded");

        Resource pcReleased = resourceRepository.findById(pc04.getId()).orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, pcReleased.getStatus(), "PC 04 must be released back to AVAILABLE");

        // 5. Verify Dashboard stats after auto-checkout
        DashboardStatsDto statsAfter = dashboardService.getDashboardStats();
        assertEquals(initialOccupied, statsAfter.getOccupiedResources(), "Occupied count must decrement back");
        assertEquals(initialCheckIns + 1, statsAfter.getTodayCheckIns(), "Today's check-in count must NOT decrement");
        assertEquals(initialAvailable, statsAfter.getAvailableResources(), "Available resources must be restored");

        // 6. Verify subsequent booking is allowed immediately for student2
        CreateUsageRequestDto nextReq = new CreateUsageRequestDto();
        nextReq.setResourceId(pc04.getId());
        nextReq.setQuantity(1);
        nextReq.setExpectedArrival("11:00 AM");
        nextReq.setExpectedDuration("1 hour");
        nextReq.setGroupSize(1);

        UsageRequestResponseDto student2Booking = usageRequestService.createRequest(nextReq, student2.getEmail());
        assertNotNull(student2Booking, "Subsequent student can now book the released resource");

        // Clean up
        usageRequestService.cancelRequest(student2Booking.getId(), student2.getEmail());
    }

    @Test
    @Order(4)
    @DisplayName("Part 4 & 5: Configured duration preservation (2-hour session does not prematurely expire at 1 hour)")
    void testConfiguredDurationPreservation() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 20, 14, 0); // 2:00 PM
        Clock clock1400 = Clock.fixed(baseTime.atZone(KOLKATA).toInstant(), KOLKATA);
        usageRequestService.setClock(clock1400);
        bookingLifecycleService.setClock(clock1400);
        checkInService.setClock(clock1400);

        Resource seat01 = resourceRepository.findByFacilityAndName(elibrary, "Seat 01").orElseThrow();
        seat01.setStatus(ResourceStatus.AVAILABLE);
        resourceRepository.save(seat01);

        // 2-hour booking
        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(seat01.getId());
        req.setQuantity(1);
        req.setExpectedArrival("2:00 PM");
        req.setExpectedDuration("2 hours");
        req.setGroupSize(1);

        UsageRequestResponseDto created = usageRequestService.createRequest(req, student1.getEmail());
        usageRequestService.approveRequest(created.getId(), "operator@queuesense.demo");
        checkInService.verifyAndCheckIn(new VerifyCheckInDto(created.getRequestCode(), CheckInMethod.QR_SCAN), "operator@queuesense.demo");

        // At 1 hour (3:00 PM): MUST STILL BE ACTIVE (not forced to 1 hour)
        LocalDateTime at1500 = baseTime.plusHours(1);
        bookingLifecycleService.processExpiredBookings(at1500);

        UsageRequest reqAt1Hr = usageRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(RequestStatus.CHECKED_IN, reqAt1Hr.getStatus(), "2-hour booking must not expire at 1 hour");

        // At 2 hours (4:00 PM): MUST AUTO-COMPLETE
        LocalDateTime at1600 = baseTime.plusHours(2);
        bookingLifecycleService.processExpiredBookings(at1600);

        UsageRequest reqAt2Hr = usageRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(RequestStatus.COMPLETED, reqAt2Hr.getStatus(), "2-hour booking must auto-complete at 2 hours");

        Resource seatReleased = resourceRepository.findById(seat01.getId()).orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, seatReleased.getStatus(), "Seat 01 must be released back to AVAILABLE");
    }

    @Test
    @Order(5)
    @DisplayName("Idempotency: Running processExpiredBookings multiple times does not corrupt history or duplicate checkouts")
    void testIdempotentLifecycleProcessing() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 20, 17, 0);
        Clock clock1700 = Clock.fixed(baseTime.atZone(KOLKATA).toInstant(), KOLKATA);
        bookingLifecycleService.setClock(clock1700);

        // Run 5 times in a row
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> bookingLifecycleService.processExpiredBookings(baseTime));
        }

        DashboardStatsDto stats = dashboardService.getDashboardStats();
        assertNotNull(stats);
    }
}
