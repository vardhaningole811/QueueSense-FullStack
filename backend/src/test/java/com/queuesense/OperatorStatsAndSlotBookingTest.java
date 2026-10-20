package com.queuesense;

import com.queuesense.dto.*;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.ResourceItem;
import com.queuesense.entity.UsageRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperatorStatsAndSlotBookingTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UsageRequestService usageRequestService;

    @Autowired
    private SportsInventoryService sportsInventoryService;

    @Autowired
    private BookingLifecycleService bookingLifecycleService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckOutService checkOutService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceItemRepository resourceItemRepository;

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

    @BeforeEach
    void setupUsersAndSports() {
        sportsInventoryService.initializeSportsInventory();

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
    }

    @AfterEach
    void resetClocks() {
        Clock sys = Clock.system(KOLKATA);
        sportsInventoryService.setClock(sys);
        usageRequestService.setClock(sys);
        bookingLifecycleService.setClock(sys);
        checkInService.setClock(sys);
        checkOutService.setClock(sys);
    }

    @Test
    @Order(1)
    @DisplayName("Case 1 to 4: Operator stats lifecycle with basketball booking (pending, approved, checked-in, checked-out)")
    void testOperatorStatsLifecycleBasketball() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource basketball = resourceRepository.findByFacilityAndName(sports, "Basketball").orElseThrow();

        DashboardStatsDto initialStats = dashboardService.getDashboardStats();
        long initialPending = initialStats.getPendingRequests();
        long initialReserved = initialStats.getReservedResources();
        long initialOccupied = initialStats.getOccupiedResources();
        long initialAvailable = initialStats.getAvailableResources();
        long initialCheckIns = initialStats.getTodayCheckIns();

        // 1. Student requests 1 basketball -> increments Pending Requests to initial + 1
        CreateUsageRequestDto reqDto = new CreateUsageRequestDto();
        reqDto.setResourceId(basketball.getId());
        reqDto.setQuantity(1);
        reqDto.setExpectedArrival("10:00 AM");
        reqDto.setExpectedDuration("1 hour");
        reqDto.setGroupSize(1);

        UsageRequestResponseDto created = usageRequestService.createRequest(reqDto, student1.getEmail());
        assertNotNull(created);
        assertEquals(RequestStatus.PENDING, created.getStatus());

        DashboardStatsDto statsAfterRequest = dashboardService.getDashboardStats();
        assertEquals(initialPending + 1, statsAfterRequest.getPendingRequests(), "Pending Requests must increment by 1");
        assertEquals(statsAfterRequest.getPendingRequests(), statsAfterRequest.getPendingRequestsCount());
        assertEquals(initialReserved, statsAfterRequest.getReservedResources());
        assertEquals(initialOccupied, statsAfterRequest.getOccupiedResources());
        // Available resources decreased by 1
        assertEquals(initialAvailable - 1, statsAfterRequest.getAvailableResources());

        // 2. Operator approves request -> decrements Pending Requests, increments Reserved (Approved)
        UsageRequestResponseDto approved = usageRequestService.approveRequest(created.getId(), "operator@queuesense.demo");
        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        DashboardStatsDto statsAfterApprove = dashboardService.getDashboardStats();
        assertEquals(initialPending, statsAfterApprove.getPendingRequests(), "Pending Requests decrements back");
        assertEquals(initialReserved + 1, statsAfterApprove.getReservedResources(), "Reserved (Approved) increments by 1");
        assertEquals(statsAfterApprove.getReservedResources(), statsAfterApprove.getReservedResourcesCount());
        assertEquals(initialOccupied, statsAfterApprove.getOccupiedResources());

        // 3. Student checks in -> changes from reserved to occupied, increments Today's Check-ins
        VerifyCheckInDto checkInDto = new VerifyCheckInDto(created.getRequestCode(), CheckInMethod.QR_SCAN);
        UsageRequestResponseDto checkedIn = checkInService.verifyAndCheckIn(checkInDto, "operator@queuesense.demo");
        assertEquals(RequestStatus.CHECKED_IN, checkedIn.getStatus());

        DashboardStatsDto statsAfterCheckIn = dashboardService.getDashboardStats();
        assertEquals(initialReserved, statsAfterCheckIn.getReservedResources(), "Reserved decrements back");
        assertEquals(initialOccupied + 1, statsAfterCheckIn.getOccupiedResources(), "Currently Occupied increments by 1");
        assertEquals(initialCheckIns + 1, statsAfterCheckIn.getTodayCheckIns(), "Today's Check-ins increments by 1");

        // 4. Student checks out -> removes from Currently Occupied and releases inventory unit
        checkOutService.checkOut(created.getId(), student1.getEmail(), "ROLE_STUDENT");

        DashboardStatsDto statsAfterCheckOut = dashboardService.getDashboardStats();
        assertEquals(initialOccupied, statsAfterCheckOut.getOccupiedResources(), "Currently Occupied decrements back");
        assertEquals(initialAvailable, statsAfterCheckOut.getAvailableResources(), "Available resources restored");
    }

    @Test
    @Order(2)
    @DisplayName("Case 6 & 9: 30-minute booking window boundary testing (rejected at 2:29 PM, allowed at 2:30 PM, future slot rejected)")
    void testRollingThirtyMinuteBookingWindow() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource padel = resourceRepository.findByFacilityAndName(sports, "Padel court").orElseThrow();

        LocalDate today = LocalDate.now(KOLKATA);
        LocalDateTime slotStart = today.atTime(15, 0); // 3:00 PM
        LocalDateTime slotEnd = today.atTime(16, 0);   // 4:00 PM

        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(padel.getId());
        req.setQuantity(1);
        req.setSlotStartTime(slotStart);
        req.setSlotEndTime(slotEnd);
        req.setExpectedDuration("1 hour");
        req.setGroupSize(2);

        // A. At 2:29 PM (31 mins before start): MUST BE REJECTED
        LocalDateTime at229 = today.atTime(14, 29);
        Clock clock229 = Clock.fixed(at229.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock229);
        usageRequestService.setClock(clock229);
        bookingLifecycleService.setClock(clock229);

        InvalidStateTransitionException exTooEarly = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req, student1.getEmail());
        });
        assertTrue(exTooEarly.getMessage().contains("Booking window not open yet") || exTooEarly.getMessage().contains("30 minutes"));

        // B. Case 9: Future slot far outside window (e.g. 6:00 PM - 7:00 PM) MUST BE REJECTED
        CreateUsageRequestDto futureReq = new CreateUsageRequestDto();
        futureReq.setResourceId(padel.getId());
        futureReq.setQuantity(1);
        futureReq.setSlotStartTime(today.atTime(18, 0));
        futureReq.setSlotEndTime(today.atTime(19, 0));
        futureReq.setExpectedDuration("1 hour");
        futureReq.setGroupSize(2);

        assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(futureReq, student1.getEmail());
        });

        // C. At 2:30 PM (exactly 30 mins before start): MUST SUCCEED
        LocalDateTime at230 = today.atTime(14, 30);
        Clock clock230 = Clock.fixed(at230.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock230);
        usageRequestService.setClock(clock230);
        bookingLifecycleService.setClock(clock230);

        UsageRequestResponseDto booked = usageRequestService.createRequest(req, student1.getEmail());
        assertNotNull(booked);
        assertEquals(RequestStatus.PENDING, booked.getStatus());
        assertEquals(slotStart, booked.getSlotStartTime());
        assertEquals(slotEnd, booked.getSlotEndTime());

        // Reset clocks to system
        sportsInventoryService.setClock(Clock.system(KOLKATA));
        usageRequestService.setClock(Clock.system(KOLKATA));
        bookingLifecycleService.setClock(Clock.system(KOLKATA));

        // Clean up
        usageRequestService.cancelRequest(booked.getId(), student1.getEmail());
    }

    @Test
    @Order(3)
    @DisplayName("Case 5: At one-hour end time, expired court booking no longer counts as occupied or blocks next slot")
    void testCourtSlotOneHourAutoCompletion() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource padel = resourceRepository.findByFacilityAndName(sports, "Padel court").orElseThrow();

        LocalDate today = LocalDate.now(KOLKATA);
        LocalDateTime slotStart = today.atTime(14, 0); // 2:00 PM
        LocalDateTime slotEnd = today.atTime(15, 0);   // 3:00 PM

        // Set clock to 1:45 PM (within 30 mins of 2:00 PM)
        LocalDateTime at145 = today.atTime(13, 45);
        Clock clock145 = Clock.fixed(at145.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock145);
        usageRequestService.setClock(clock145);
        bookingLifecycleService.setClock(clock145);
        checkInService.setClock(clock145);
        checkOutService.setClock(clock145);

        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(padel.getId());
        req.setQuantity(1);
        req.setSlotStartTime(slotStart);
        req.setSlotEndTime(slotEnd);
        req.setExpectedDuration("1 hour");
        req.setGroupSize(2);

        UsageRequestResponseDto booking = usageRequestService.createRequest(req, student1.getEmail());
        usageRequestService.approveRequest(booking.getId(), "operator@queuesense.demo");

        // Advance clock to 2:00 PM and check in
        LocalDateTime at200 = today.atTime(14, 0);
        Clock clock200 = Clock.fixed(at200.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock200);
        usageRequestService.setClock(clock200);
        bookingLifecycleService.setClock(clock200);
        checkInService.setClock(clock200);
        checkOutService.setClock(clock200);

        checkInService.verifyAndCheckIn(new VerifyCheckInDto(booking.getRequestCode(), CheckInMethod.QR_SCAN), "operator@queuesense.demo");

        DashboardStatsDto statsAt200 = dashboardService.getDashboardStats();
        long occupiedAt200 = statsAt200.getOccupiedResources();
        assertTrue(occupiedAt200 > 0, "Booking must be counted as occupied while in session");

        // Advance clock to 3:00 PM (end of slot)
        LocalDateTime at300 = today.atTime(15, 0);
        Clock clock300 = Clock.fixed(at300.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock300);
        usageRequestService.setClock(clock300);
        bookingLifecycleService.setClock(clock300);
        checkInService.setClock(clock300);
        checkOutService.setClock(clock300);

        // Process lifecycle expiration
        bookingLifecycleService.processExpiredBookings(at300);

        UsageRequest afterSlot = usageRequestRepository.findById(booking.getId()).orElseThrow();
        assertEquals(RequestStatus.COMPLETED, afterSlot.getStatus(), "Booking must transition to COMPLETED at slot end time");

        DashboardStatsDto statsAt300 = dashboardService.getDashboardStats();
        assertEquals(occupiedAt200 - 1, statsAt300.getOccupiedResources(), "Must no longer count as occupied");

        // Subsequent slot (3:00 PM - 4:00 PM) can now be booked by student2 at 3:00 PM
        CreateUsageRequestDto nextSlotReq = new CreateUsageRequestDto();
        nextSlotReq.setResourceId(padel.getId());
        nextSlotReq.setQuantity(1);
        nextSlotReq.setSlotStartTime(today.atTime(15, 0));
        nextSlotReq.setSlotEndTime(today.atTime(16, 0));
        nextSlotReq.setExpectedDuration("1 hour");
        nextSlotReq.setGroupSize(2);

        UsageRequestResponseDto student2Booking = usageRequestService.createRequest(nextSlotReq, student2.getEmail());
        assertNotNull(student2Booking, "Subsequent slot is available and bookable");

        // Reset clocks
        sportsInventoryService.setClock(Clock.system(KOLKATA));
        usageRequestService.setClock(Clock.system(KOLKATA));
        bookingLifecycleService.setClock(Clock.system(KOLKATA));

        // Clean up
        usageRequestService.cancelRequest(student2Booking.getId(), student2.getEmail());
    }

    @Test
    @Order(4)
    @DisplayName("Case 7: Two students cannot book overlapping times for the same physical court")
    void testOverlappingBookingsRejectedForSameCourt() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource tennis = resourceRepository.findByFacilityAndName(sports, "Tennis court").orElseThrow();
        ResourceItem tennisUnit = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(tennis).get(0);

        LocalDate today = LocalDate.now(KOLKATA);
        LocalDateTime slotStart = today.atTime(16, 0);
        LocalDateTime slotEnd = today.atTime(17, 0);

        LocalDateTime at1540 = today.atTime(15, 40);
        Clock clock1540 = Clock.fixed(at1540.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock1540);
        usageRequestService.setClock(clock1540);
        bookingLifecycleService.setClock(clock1540);

        // Student 1 books Tennis Court #1
        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(tennis.getId());
        req1.setResourceItemId(tennisUnit.getId());
        req1.setSlotStartTime(slotStart);
        req1.setSlotEndTime(slotEnd);
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(2);

        UsageRequestResponseDto res1 = usageRequestService.createRequest(req1, student1.getEmail());
        assertNotNull(res1);

        // Student 2 tries to book the SAME unit for the SAME overlapping slot
        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(tennis.getId());
        req2.setResourceItemId(tennisUnit.getId());
        req2.setSlotStartTime(slotStart);
        req2.setSlotEndTime(slotEnd);
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(2);

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req2, student2.getEmail());
        });
        assertTrue(ex.getMessage().contains("already reserved") || ex.getMessage().contains("occupied") || ex.getMessage().contains("All courts/nets"));

        // Clean up
        usageRequestService.cancelRequest(res1.getId(), student1.getEmail());
        sportsInventoryService.setClock(Clock.system(KOLKATA));
        usageRequestService.setClock(Clock.system(KOLKATA));
        bookingLifecycleService.setClock(Clock.system(KOLKATA));
    }

    @Test
    @Order(5)
    @DisplayName("Case 8: Separate courts/nets can have simultaneous independent bookings")
    void testIndependentSimultaneousCourtBookings() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource badminton = resourceRepository.findByFacilityAndName(sports, "Badminton court").orElseThrow();
        List<ResourceItem> courts = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(badminton);
        assertEquals(2, courts.size(), "Badminton court must have 2 physical court units");

        ResourceItem court1 = courts.get(0);
        ResourceItem court2 = courts.get(1);

        LocalDate today = LocalDate.now(KOLKATA);
        LocalDateTime slotStart = today.atTime(17, 0);
        LocalDateTime slotEnd = today.atTime(18, 0);

        LocalDateTime at1640 = today.atTime(16, 40);
        Clock clock1640 = Clock.fixed(at1640.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock1640);
        usageRequestService.setClock(clock1640);
        bookingLifecycleService.setClock(clock1640);

        // Student 1 books Court #1
        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(badminton.getId());
        req1.setResourceItemId(court1.getId());
        req1.setSlotStartTime(slotStart);
        req1.setSlotEndTime(slotEnd);
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(2);

        UsageRequestResponseDto res1 = usageRequestService.createRequest(req1, student1.getEmail());
        assertNotNull(res1);
        assertEquals(court1.getId(), res1.getResourceItemId());

        // Student 2 books Court #2 for the exact same slot -> MUST SUCCEED
        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(badminton.getId());
        req2.setResourceItemId(court2.getId());
        req2.setSlotStartTime(slotStart);
        req2.setSlotEndTime(slotEnd);
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(2);

        UsageRequestResponseDto res2 = usageRequestService.createRequest(req2, student2.getEmail());
        assertNotNull(res2);
        assertEquals(court2.getId(), res2.getResourceItemId());

        // Clean up
        usageRequestService.cancelRequest(res1.getId(), student1.getEmail());
        usageRequestService.cancelRequest(res2.getId(), student2.getEmail());
        sportsInventoryService.setClock(Clock.system(KOLKATA));
        usageRequestService.setClock(Clock.system(KOLKATA));
        bookingLifecycleService.setClock(Clock.system(KOLKATA));
    }

    @Test
    @Order(6)
    @DisplayName("Case 10: Closed or maintenance resources cannot be booked")
    void testClosedAndMaintenanceResourcesCannotBeBooked() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource cricketNets = resourceRepository.findByFacilityAndName(sports, "Cricket nets").orElseThrow();
        ResourceItem net1 = resourceItemRepository.findByResourceAndActiveTrueOrderByUnitNumberAsc(cricketNets).get(0);

        // Put Net #1 under maintenance
        sportsInventoryService.updateItemStatus(net1.getId(), new UpdateItemStatusDto("MAINTENANCE", "Pitch repair"), "admin@queuesense.demo");

        LocalDate today = LocalDate.now(KOLKATA);
        LocalDateTime slotStart = today.atTime(18, 0);
        LocalDateTime slotEnd = today.atTime(19, 0);

        LocalDateTime at1740 = today.atTime(17, 40);
        Clock clock1740 = Clock.fixed(at1740.atZone(KOLKATA).toInstant(), KOLKATA);
        sportsInventoryService.setClock(clock1740);
        usageRequestService.setClock(clock1740);
        bookingLifecycleService.setClock(clock1740);

        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(cricketNets.getId());
        req.setResourceItemId(net1.getId());
        req.setSlotStartTime(slotStart);
        req.setSlotEndTime(slotEnd);
        req.setExpectedDuration("1 hour");
        req.setGroupSize(4);

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req, student1.getEmail());
        });
        assertTrue(ex.getMessage().contains("maintenance") || ex.getMessage().contains("unavailable"));

        // Restore Net #1
        sportsInventoryService.updateItemStatus(net1.getId(), new UpdateItemStatusDto("AVAILABLE", "Repair complete"), "admin@queuesense.demo");

        // Close Tennis Court bookings administratively
        Resource tennis = resourceRepository.findByFacilityAndName(sports, "Tennis court").orElseThrow();
        sportsInventoryService.toggleBookingClosure(tennis.getId(), true, "Rain/wet court", "admin@queuesense.demo");

        CreateUsageRequestDto reqTennis = new CreateUsageRequestDto();
        reqTennis.setResourceId(tennis.getId());
        reqTennis.setSlotStartTime(slotStart);
        reqTennis.setSlotEndTime(slotEnd);
        reqTennis.setExpectedDuration("1 hour");
        reqTennis.setGroupSize(2);

        assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(reqTennis, student1.getEmail());
        });

        // Reopen Tennis
        sportsInventoryService.toggleBookingClosure(tennis.getId(), false, "admin@queuesense.demo");

        sportsInventoryService.setClock(Clock.system(KOLKATA));
        usageRequestService.setClock(Clock.system(KOLKATA));
        bookingLifecycleService.setClock(Clock.system(KOLKATA));
    }

    @Test
    @Order(7)
    @DisplayName("Case 11: Statistics remain correct after dashboard reload based on persisted database data")
    void testStatisticsPersistedCorrectly() {
        DashboardStatsDto stats1 = dashboardService.getDashboardStats();
        DashboardStatsDto stats2 = dashboardService.getDashboardStats();

        assertEquals(stats1.getPendingRequests(), stats2.getPendingRequests());
        assertEquals(stats1.getAvailableResources(), stats2.getAvailableResources());
        assertEquals(stats1.getReservedResources(), stats2.getReservedResources());
        assertEquals(stats1.getOccupiedResources(), stats2.getOccupiedResources());
        assertEquals(stats1.getTodayCheckIns(), stats2.getTodayCheckIns());
    }

    @Test
    @Order(8)
    @DisplayName("Case 12: Existing non-Sports facilities (E-Library, Reading Room) continue to work properly")
    void testNonSportsFacilitiesUnaffected() {
        Facility elibrary = resourceRepository.findAll().stream()
                .filter(r -> "Seat 01".equalsIgnoreCase(r.getName()))
                .map(Resource::getFacility)
                .findFirst()
                .orElseThrow();

        Resource seat01 = resourceRepository.findByFacilityAndName(elibrary, "Seat 01").orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, seat01.getStatus());

        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(seat01.getId());
        req.setQuantity(1);
        req.setExpectedArrival("11:00 AM");
        req.setExpectedDuration("2 hours");
        req.setGroupSize(1);

        UsageRequestResponseDto booked = usageRequestService.createRequest(req, student1.getEmail());
        assertNotNull(booked);

        // Approve
        usageRequestService.approveRequest(booked.getId(), "operator@queuesense.demo");
        Resource seatAfterApprove = resourceRepository.findById(seat01.getId()).orElseThrow();
        assertEquals(ResourceStatus.RESERVED, seatAfterApprove.getStatus(), "Non-sports resource status becomes RESERVED on approve");

        // Check in
        checkInService.verifyAndCheckIn(new VerifyCheckInDto(booked.getRequestCode(), CheckInMethod.QR_SCAN), "operator@queuesense.demo");
        Resource seatAfterCheckIn = resourceRepository.findById(seat01.getId()).orElseThrow();
        assertEquals(ResourceStatus.OCCUPIED, seatAfterCheckIn.getStatus(), "Non-sports resource status becomes OCCUPIED on check-in");

        // Check out
        checkOutService.checkOut(booked.getId(), student1.getEmail(), "ROLE_STUDENT");
        Resource seatAfterCheckOut = resourceRepository.findById(seat01.getId()).orElseThrow();
        assertEquals(ResourceStatus.AVAILABLE, seatAfterCheckOut.getStatus(), "Non-sports resource status returns to AVAILABLE on checkout");
    }
}
