package com.queuesense;

import com.queuesense.dto.AuthResponse;
import com.queuesense.dto.CreateUsageRequestDto;
import com.queuesense.dto.LoginRequest;
import com.queuesense.dto.UsageRequestResponseDto;
import com.queuesense.dto.VerifyCheckInDto;
import com.queuesense.enums.CheckInMethod;
import com.queuesense.enums.RequestStatus;
import com.queuesense.enums.ResourceStatus;
import com.queuesense.service.AuthService;
import com.queuesense.service.CheckInService;
import com.queuesense.service.CheckOutService;
import com.queuesense.service.ResourceService;
import com.queuesense.service.UsageRequestService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueueSenseWorkflowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsageRequestService usageRequestService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckOutService checkOutService;

    @Autowired
    private ResourceService resourceService;

    private static String createdRequestCode;
    private static Long createdRequestId;

    @Test
    @Order(1)
    void testAuthentication() {
        LoginRequest studentLogin = new LoginRequest("student@queuesense.demo", "Student@123");
        AuthResponse studentAuth = authService.login(studentLogin);
        assertNotNull(studentAuth.getToken());
        assertEquals("ROLE_STUDENT", studentAuth.getRole());
        assertEquals("STU-2026-001", studentAuth.getStudentId());

        LoginRequest operatorLogin = new LoginRequest("operator@queuesense.demo", "Operator@123");
        AuthResponse operatorAuth = authService.login(operatorLogin);
        assertNotNull(operatorAuth.getToken());
        assertEquals("ROLE_OPERATOR", operatorAuth.getRole());
    }

    @Test
    @Order(2)
    void testRequestCreationAndPendingState() {
        CreateUsageRequestDto requestDto = new CreateUsageRequestDto();
        requestDto.setResourceId(3L); // Seat 03
        requestDto.setExpectedArrival("10:30 AM");
        requestDto.setExpectedDuration("2 hours");
        requestDto.setGroupSize(1);

        UsageRequestResponseDto response = usageRequestService.createRequest(requestDto, "student@queuesense.demo");
        assertNotNull(response);
        assertNotNull(response.getRequestCode());
        assertEquals(RequestStatus.PENDING, response.getStatus());
        assertEquals("Seat 03", response.getResourceName());

        createdRequestCode = response.getRequestCode();
        createdRequestId = response.getId();
    }

    @Test
    @Order(3)
    void testOperatorApproval() {
        assertNotNull(createdRequestId);
        UsageRequestResponseDto approved = usageRequestService.approveRequest(createdRequestId, "operator@queuesense.demo");
        assertEquals(RequestStatus.APPROVED, approved.getStatus());

        // Verify resource status transitioned to RESERVED
        assertEquals(ResourceStatus.RESERVED, resourceService.getResourceById(3L).getStatus());
    }

    @Test
    @Order(4)
    void testOperatorCheckIn() {
        assertNotNull(createdRequestCode);
        VerifyCheckInDto verifyDto = new VerifyCheckInDto(createdRequestCode, CheckInMethod.QR_SCAN);
        UsageRequestResponseDto checkedIn = checkInService.verifyAndCheckIn(verifyDto, "operator@queuesense.demo");
        assertEquals(RequestStatus.CHECKED_IN, checkedIn.getStatus());

        // Verify resource status transitioned to OCCUPIED
        assertEquals(ResourceStatus.OCCUPIED, resourceService.getResourceById(3L).getStatus());
    }

    @Test
    @Order(5)
    void testCheckoutAndCompletedState() {
        assertNotNull(createdRequestId);
        UsageRequestResponseDto checkedOut = checkOutService.checkOut(createdRequestId, "student@queuesense.demo", "ROLE_STUDENT");
        assertEquals(RequestStatus.COMPLETED, checkedOut.getStatus());

        // Verify resource status returned to AVAILABLE
        assertEquals(ResourceStatus.AVAILABLE, resourceService.getResourceById(3L).getStatus());
    }
}
