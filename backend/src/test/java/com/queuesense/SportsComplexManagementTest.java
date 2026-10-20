package com.queuesense;

import com.queuesense.dto.*;
import com.queuesense.entity.Facility;
import com.queuesense.entity.Resource;
import com.queuesense.entity.User;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.Role;
import com.queuesense.exception.DuplicateRequestException;
import com.queuesense.exception.InvalidStateTransitionException;
import com.queuesense.repository.FacilityRepository;
import com.queuesense.repository.ResourceRepository;
import com.queuesense.repository.UsageRequestRepository;
import com.queuesense.repository.UserRepository;
import com.queuesense.service.SportsInventoryService;
import com.queuesense.service.UsageRequestService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SportsComplexManagementTest {

    @Autowired
    private SportsInventoryService sportsInventoryService;

    @Autowired
    private UsageRequestService usageRequestService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsageRequestRepository usageRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.queuesense.service.CheckInService checkInService;

    @Autowired
    private com.queuesense.service.CheckOutService checkOutService;

    @Autowired
    private com.queuesense.repository.StudentProfileRepository studentProfileRepository;

    private static User testStudent2;

    @BeforeEach
    void ensureSportsInitialized() {
        sportsInventoryService.initializeSportsInventory();
    }

    @Test
    @Order(1)
    void testExactInitialInventoryQuantities() {
        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        assertNotNull(availability);
        assertNotNull(availability.getEquipment());
        assertNotNull(availability.getCourtsAndNets());

        // Verify Equipment
        assertEquals(5, findResource(availability.getEquipment(), "Basketball").getTotalQuantity());
        assertEquals("One basketball per unit", findResource(availability.getEquipment(), "Basketball").getBookingUnit());

        assertEquals(5, findResource(availability.getEquipment(), "Volleyball").getTotalQuantity());
        assertEquals("One volleyball per unit", findResource(availability.getEquipment(), "Volleyball").getBookingUnit());

        assertEquals(3, findResource(availability.getEquipment(), "Chess board").getTotalQuantity());
        assertEquals("One chess board per unit", findResource(availability.getEquipment(), "Chess board").getBookingUnit());

        assertEquals(2, findResource(availability.getEquipment(), "Carrom board").getTotalQuantity());
        assertEquals("One carrom board per unit", findResource(availability.getEquipment(), "Carrom board").getBookingUnit());

        assertEquals(3, findResource(availability.getEquipment(), "Badminton racket and shuttlecock set").getTotalQuantity());
        assertEquals("One complete set per unit", findResource(availability.getEquipment(), "Badminton racket and shuttlecock set").getBookingUnit());

        // Verify Courts & Nets
        assertEquals(2, findResource(availability.getCourtsAndNets(), "Badminton court").getTotalQuantity());
        assertEquals("One court", findResource(availability.getCourtsAndNets(), "Badminton court").getBookingUnit());

        assertEquals(1, findResource(availability.getCourtsAndNets(), "Tennis court").getTotalQuantity());
        assertEquals("One court", findResource(availability.getCourtsAndNets(), "Tennis court").getBookingUnit());

        assertEquals(1, findResource(availability.getCourtsAndNets(), "Padel court").getTotalQuantity());
        assertEquals("One court", findResource(availability.getCourtsAndNets(), "Padel court").getBookingUnit());

        assertEquals(1, findResource(availability.getCourtsAndNets(), "Pickleball court").getTotalQuantity());
        assertEquals("One court", findResource(availability.getCourtsAndNets(), "Pickleball court").getBookingUnit());

        assertEquals(4, findResource(availability.getCourtsAndNets(), "Cricket nets").getTotalQuantity());
        assertEquals("One net", findResource(availability.getCourtsAndNets(), "Cricket nets").getBookingUnit());

        // Verify Basketball Court is NOT in bookable inventory
        assertTrue(availability.getCourtsAndNets().stream().noneMatch(r -> "Basketball court".equalsIgnoreCase(r.getName())));
        assertTrue(availability.getEquipment().stream().noneMatch(r -> "Basketball court".equalsIgnoreCase(r.getName())));
    }

    @Test
    @Order(2)
    void testInventoryIdempotenceOnReinitialization() {
        // Re-run initialization to simulate backend restarts
        sportsInventoryService.initializeSportsInventory();
        sportsInventoryService.initializeSportsInventory();

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(5, findResource(availability.getEquipment(), "Basketball").getTotalQuantity());
        assertEquals(4, findResource(availability.getCourtsAndNets(), "Cricket nets").getTotalQuantity());
    }

    @Test
    @Order(3)
    void testEquipmentDamageReducesUsableQuantity() {
        List<SportsInventoryDetailDto> details = sportsInventoryService.getAllInventoryDetails();
        SportsInventoryDetailDto bbDetail = details.stream()
                .filter(d -> "Basketball".equalsIgnoreCase(d.getResource().getName()))
                .findFirst()
                .orElseThrow();

        ResourceItemDto firstBall = bbDetail.getItems().get(0);
        int initialUsable = bbDetail.getResource().getUsableQuantity();

        // Mark 1 basketball damaged
        UpdateItemStatusDto damageDto = new UpdateItemStatusDto("DAMAGED", "Punctured during tournament");
        sportsInventoryService.updateItemStatus(firstBall.getId(), damageDto, "admin@queuesense.demo");

        // Verify usable decreased by 1
        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        SportsResourceDto bbAfter = findResource(availability.getEquipment(), "Basketball");
        assertEquals(initialUsable - 1, bbAfter.getUsableQuantity());
        assertEquals(1, bbAfter.getUnavailableQuantity());

        // Return to service
        UpdateItemStatusDto repairDto = new UpdateItemStatusDto("AVAILABLE", "Repaired / new bladder");
        sportsInventoryService.updateItemStatus(firstBall.getId(), repairDto, "admin@queuesense.demo");

        availability = sportsInventoryService.getStudentAvailability(null);
        bbAfter = findResource(availability.getEquipment(), "Basketball");
        assertEquals(initialUsable, bbAfter.getUsableQuantity());
        assertEquals(0, bbAfter.getUnavailableQuantity());
    }

    @Test
    @Order(4)
    void testIncreaseAndDecreaseTotalInventory() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource volleyball = resourceRepository.findByFacilityAndName(sports, "Volleyball").orElseThrow();

        // Increase Volleyball inventory from 5 to 7
        sportsInventoryService.updateTotalQuantity(volleyball.getId(), 7, "admin@queuesense.demo");

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        SportsResourceDto vb = findResource(availability.getEquipment(), "Volleyball");
        assertEquals(7, vb.getTotalQuantity());
        assertEquals(7, vb.getUsableQuantity());

        // Restore back to 5
        sportsInventoryService.updateTotalQuantity(volleyball.getId(), 5, "admin@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        vb = findResource(availability.getEquipment(), "Volleyball");
        assertEquals(5, vb.getTotalQuantity());
    }

    @Test
    @Order(5)
    void testCourtMaintenanceLeavesOtherCourtsBookable() {
        List<SportsInventoryDetailDto> details = sportsInventoryService.getAllInventoryDetails();
        SportsInventoryDetailDto badminton = details.stream()
                .filter(d -> "Badminton court".equalsIgnoreCase(d.getResource().getName()))
                .findFirst()
                .orElseThrow();

        ResourceItemDto court2 = badminton.getItems().get(1);

        // Place Badminton court 2 under maintenance
        UpdateItemStatusDto maintDto = new UpdateItemStatusDto("MAINTENANCE", "Floor resurfacing");
        sportsInventoryService.updateItemStatus(court2.getId(), maintDto, "admin@queuesense.demo");

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        SportsResourceDto bc = findResource(availability.getCourtsAndNets(), "Badminton court");
        assertEquals(2, bc.getTotalQuantity());
        assertEquals(1, bc.getUnavailableQuantity());
        assertEquals(1, bc.getUsableQuantity());
        assertEquals(1, bc.getAvailableQuantity());
        assertEquals("Partially available", bc.getStatusText());

        // Restore court 2 to available
        UpdateItemStatusDto availableDto = new UpdateItemStatusDto("AVAILABLE", "Resurfacing complete");
        sportsInventoryService.updateItemStatus(court2.getId(), availableDto, "admin@queuesense.demo");
    }

    @Test
    @Order(6)
    void testTemporaryBookingClosureAndReopening() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource tennis = resourceRepository.findByFacilityAndName(sports, "Tennis court").orElseThrow();

        // Close tennis bookings
        sportsInventoryService.toggleBookingClosure(tennis.getId(), true, "admin@queuesense.demo");

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        SportsResourceDto tc = findResource(availability.getCourtsAndNets(), "Tennis court");
        assertTrue(tc.isBookingClosed());
        assertEquals(0, tc.getAvailableQuantity());
        assertEquals("Currently unavailable", tc.getStatusText());

        // Student cannot book when closed
        CreateUsageRequestDto requestDto = new CreateUsageRequestDto();
        requestDto.setResourceId(tennis.getId());
        requestDto.setQuantity(1);
        requestDto.setExpectedArrival("4:00 PM");
        requestDto.setExpectedDuration("1 hour");
        requestDto.setGroupSize(2);

        assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(requestDto, "student@queuesense.demo");
        });

        // Reopen tennis bookings
        sportsInventoryService.toggleBookingClosure(tennis.getId(), false, "admin@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        tc = findResource(availability.getCourtsAndNets(), "Tennis court");
        assertFalse(tc.isBookingClosed());
        assertEquals(1, tc.getAvailableQuantity());
    }

    @Test
    @Order(7)
    void testSingleActiveBookingRuleForSportsComplex() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource chess = resourceRepository.findByFacilityAndName(sports, "Chess board").orElseThrow();
        Resource carrom = resourceRepository.findByFacilityAndName(sports, "Carrom board").orElseThrow();

        // Ensure student has a test user or use student@queuesense.demo
        User student = userRepository.findByEmail("student@queuesense.demo").orElseThrow();

        // Clear any existing active requests for a clean test run
        usageRequestRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED)
                .forEach(r -> {
                    r.setStatus(RequestStatus.CANCELLED);
                    usageRequestRepository.save(r);
                });

        // 1. First booking request (Chess board, quantity = 1)
        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(chess.getId());
        req1.setQuantity(1);
        req1.setExpectedArrival("2:00 PM");
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(2);

        UsageRequestResponseDto res1 = usageRequestService.createRequest(req1, "student@queuesense.demo");
        assertNotNull(res1);
        assertEquals(RequestStatus.PENDING, res1.getStatus());

        // 2. Second booking request for Sports Complex while first is active MUST fail
        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(carrom.getId());
        req2.setQuantity(1);
        req2.setExpectedArrival("3:00 PM");
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(2);

        DuplicateRequestException ex = assertThrows(DuplicateRequestException.class, () -> {
            usageRequestService.createRequest(req2, "student@queuesense.demo");
        });
        assertTrue(ex.getMessage().contains("active Sports Complex booking"));

        // Clean up: cancel first request
        usageRequestService.cancelRequest(res1.getId(), "student@queuesense.demo");
    }

    @Test
    @Order(8)
    void testQuantityExceedingAvailableThrowsValidation() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource basketball = resourceRepository.findByFacilityAndName(sports, "Basketball").orElseThrow();

        // Request 10 basketballs when only 5 exist
        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(basketball.getId());
        req.setQuantity(10);
        req.setExpectedArrival("5:00 PM");
        req.setExpectedDuration("1 hour");
        req.setGroupSize(1);

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req, "student@queuesense.demo");
        });
        assertTrue(ex.getMessage().contains("exceeds currently available inventory"));
    }

    @Test
    @Order(9)
    void testRejectNegativeQuantityUpdate() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource carrom = resourceRepository.findByFacilityAndName(sports, "Carrom board").orElseThrow();

        assertThrows(InvalidStateTransitionException.class, () -> {
            sportsInventoryService.updateTotalQuantity(carrom.getId(), -1, "admin@queuesense.demo");
        });
    }

    @Test
    @Order(10)
    void testBookingClosureWithReasonAndStudentFeedback() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource padel = resourceRepository.findByFacilityAndName(sports, "Padel court").orElseThrow();

        String reason = "Resurfacing glass panels and turf";
        sportsInventoryService.toggleBookingClosure(padel.getId(), true, reason, "admin@queuesense.demo");

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        SportsResourceDto pc = findResource(availability.getCourtsAndNets(), "Padel court");
        assertTrue(pc.isBookingClosed());
        assertEquals(reason, pc.getClosureReason());

        CreateUsageRequestDto req = new CreateUsageRequestDto();
        req.setResourceId(padel.getId());
        req.setQuantity(1);
        req.setExpectedArrival("6:00 PM");
        req.setExpectedDuration("1 hour");
        req.setGroupSize(2);

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class, () -> {
            usageRequestService.createRequest(req, "student@queuesense.demo");
        });
        assertTrue(ex.getMessage().contains(reason) || ex.getMessage().contains("closed"));

        // Reopen
        sportsInventoryService.toggleBookingClosure(padel.getId(), false, "admin@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        pc = findResource(availability.getCourtsAndNets(), "Padel court");
        assertFalse(pc.isBookingClosed());
        assertNull(pc.getClosureReason());
    }

    @Test
    @Order(11)
    void testAllocationReleasesAfterCancellationRejectionAndCheckout() {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource volleyball = resourceRepository.findByFacilityAndName(sports, "Volleyball").orElseThrow();

        User student = userRepository.findByEmail("student@queuesense.demo").orElseThrow();

        // Clear existing requests for this student
        usageRequestRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.CHECKED_IN)
                .forEach(r -> {
                    r.setStatus(RequestStatus.CANCELLED);
                    usageRequestRepository.save(r);
                });

        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        int initialAvailable = findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity();

        // 1. Create request for 2 volleyballs
        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(volleyball.getId());
        req1.setQuantity(2);
        req1.setExpectedArrival("4:00 PM");
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(2);

        UsageRequestResponseDto res1 = usageRequestService.createRequest(req1, "student@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable - 2, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());

        // Cancel -> allocation released
        usageRequestService.cancelRequest(res1.getId(), "student@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());

        // 2. Create request -> reject -> allocation released
        UsageRequestResponseDto res2 = usageRequestService.createRequest(req1, "student@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable - 2, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());

        usageRequestService.rejectRequest(res2.getId(), new RejectRequestDto("Inter-college practice only"), "operator@queuesense.demo");
        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());

        // 3. Create request -> approve -> check in -> checkout -> allocation released
        CreateUsageRequestDto reqSingle = new CreateUsageRequestDto();
        reqSingle.setResourceId(volleyball.getId());
        reqSingle.setQuantity(1);
        reqSingle.setExpectedArrival("5:00 PM");
        reqSingle.setExpectedDuration("1 hour");
        reqSingle.setGroupSize(1);

        UsageRequestResponseDto res3 = usageRequestService.createRequest(reqSingle, "student@queuesense.demo");
        usageRequestService.approveRequest(res3.getId(), "operator@queuesense.demo");
        checkInService.verifyAndCheckIn(new VerifyCheckInDto(res3.getRequestCode(), com.queuesense.enums.CheckInMethod.QR_SCAN), "operator@queuesense.demo");

        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable - 1, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());

        // Check out
        checkOutService.checkOut(res3.getId(), "student@queuesense.demo", "ROLE_STUDENT");
        availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(initialAvailable, findResource(availability.getEquipment(), "Volleyball").getAvailableQuantity());
    }

    @Test
    @Order(12)
    void testConcurrentBookingRequestsOverbookingPrevention() throws Exception {
        Facility sports = sportsInventoryService.getSportsFacility();
        Resource pickleball = resourceRepository.findByFacilityAndName(sports, "Pickleball court").orElseThrow();

        // Ensure we have two distinct students
        User student1 = userRepository.findByEmail("student@queuesense.demo").orElseThrow();
        User student2 = userRepository.findByEmail("student2@queuesense.demo").orElseGet(() -> {
            User u = new User("student2@queuesense.demo", passwordEncoder.encode("Student@123"), Role.ROLE_STUDENT, "Pooja Patel");
            u = userRepository.save(u);
            com.queuesense.entity.StudentProfile sp = new com.queuesense.entity.StudentProfile(u, "STU-2026-002", "Information Technology", 2);
            studentProfileRepository.save(sp);
            return u;
        });

        // Cancel any active requests for both students
        for (User u : List.of(student1, student2)) {
            usageRequestRepository.findByStudentOrderByCreatedAtDesc(u).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.CHECKED_IN)
                    .forEach(r -> {
                        r.setStatus(RequestStatus.CANCELLED);
                        usageRequestRepository.save(r);
                    });
        }

        // Pickleball court has total quantity = 1
        SportsStudentAvailabilityResponseDto availability = sportsInventoryService.getStudentAvailability(null);
        assertEquals(1, findResource(availability.getCourtsAndNets(), "Pickleball court").getAvailableQuantity());

        // Attempt concurrent bookings from student1 and student2
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalDateTime slotStart = today.atTime(19, 0);
        LocalDateTime slotEnd = today.atTime(20, 0);

        LocalDateTime at1840 = today.atTime(18, 40);
        java.time.Clock clock1840 = java.time.Clock.fixed(at1840.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant(), java.time.ZoneId.of("Asia/Kolkata"));
        sportsInventoryService.setClock(clock1840);
        usageRequestService.setClock(clock1840);

        CreateUsageRequestDto req1 = new CreateUsageRequestDto();
        req1.setResourceId(pickleball.getId());
        req1.setQuantity(1);
        req1.setSlotStartTime(slotStart);
        req1.setSlotEndTime(slotEnd);
        req1.setExpectedDuration("1 hour");
        req1.setGroupSize(2);

        CreateUsageRequestDto req2 = new CreateUsageRequestDto();
        req2.setResourceId(pickleball.getId());
        req2.setQuantity(1);
        req2.setSlotStartTime(slotStart);
        req2.setSlotEndTime(slotEnd);
        req2.setExpectedDuration("1 hour");
        req2.setGroupSize(2);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.concurrent.Callable<Void> task1 = () -> {
            try {
                usageRequestService.createRequest(req1, "student@queuesense.demo");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        java.util.concurrent.Callable<Void> task2 = () -> {
            try {
                usageRequestService.createRequest(req2, "student2@queuesense.demo");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        executor.invokeAll(List.of(task1, task2));
        executor.shutdown();

        // Exactly one should succeed, and exactly one should fail (overbooking prevented!)
        assertEquals(1, successCount.get(), "Exactly one student should succeed booking the single court");
        assertEquals(1, failureCount.get(), "The second concurrent student should be prevented from overbooking");

        // Clean up
        usageRequestRepository.findByResourceAndStatusIn(pickleball, List.of(RequestStatus.PENDING, RequestStatus.APPROVED))
                .forEach(r -> {
                    r.setStatus(RequestStatus.CANCELLED);
                    usageRequestRepository.save(r);
                });

        sportsInventoryService.setClock(java.time.Clock.system(java.time.ZoneId.of("Asia/Kolkata")));
        usageRequestService.setClock(java.time.Clock.system(java.time.ZoneId.of("Asia/Kolkata")));
    }

    private SportsResourceDto findResource(List<SportsResourceDto> list, String name) {
        return list.stream()
                .filter(r -> name.equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Resource not found: " + name));
    }
}
