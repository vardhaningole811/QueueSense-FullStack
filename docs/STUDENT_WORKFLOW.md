# QueueSense — Student & Operator End-to-End Workflow Trace

## 1. Complete Workflow Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student (Browser)
    participant React as React Frontend
    participant Filter as JwtAuthenticationFilter
    participant Controller as REST Controller
    participant Service as Service Layer
    participant Repo as JPA Repository
    participant DB as PostgreSQL DB
    actor Operator as Operator (Browser)

    %% Step 1: Login
    Note over Student, DB: 1. Student Authentication
    Student->>React: Enter credentials (student@queuesense.demo)
    React->>Controller: POST /api/auth/login
    Controller->>Service: AuthService.login(req)
    Service->>DB: Check BCrypt password hash
    Service->>DB: INSERT INTO audit_logs (LOGIN)
    Service-->>React: Return JWT Token + User profile
    React->>React: Store token in localStorage

    %% Step 2: Browse & Select Resource
    Note over Student, DB: 2. Facility & Resource Discovery
    Student->>React: Navigate to Libraries -> E-Library
    React->>Controller: GET /api/facilities/1 & GET /api/resources/facility/1
    Controller->>Repo: ResourceRepository.findByFacilityAndActiveTrue()
    Repo->>DB: SELECT * FROM resources WHERE facility_id = 1
    DB-->>React: Array of resources (Seat 01 AVAILABLE, etc.)

    %% Step 3: Request Booking
    Note over Student, DB: 3. Resource Reservation Request
    Student->>React: Click "Request Seat 01" -> Submit BookingModal
    React->>Filter: POST /api/requests (Bearer JWT)
    Filter->>Controller: RequestController.createRequest(dto)
    Controller->>Service: UsageRequestService.createRequest(dto, studentEmail)
    Service->>Repo: Check duplicate active requests & resource status
    Service->>Repo: Save UsageRequest (status = PENDING, requestCode = 'QS-2026-XXXXX')
    Service->>DB: INSERT INTO usage_requests, INSERT INTO audit_logs
    Service-->>React: Return UsageRequestResponseDto
    React-->>Student: Navigate to My Requests (Shows PENDING)

    %% Step 4: Operator Approval
    Note over Operator, DB: 4. Operator Approval Queue
    Operator->>React: Open Operator Dashboard / Pending Requests
    React->>Controller: GET /api/operator/requests/pending (Bearer JWT)
    Controller->>Repo: UsageRequestRepository.findByStatus(PENDING)
    DB-->>React: List of pending requests
    Operator->>React: Click "Approve"
    React->>Controller: POST /api/operator/requests/{id}/approve
    Controller->>Service: UsageRequestService.approveRequest(id, operatorEmail)
    Service->>Repo: Update Resource (status = RESERVED)
    Service->>Repo: Update UsageRequest (status = APPROVED, expiresAt = NOW+10m)
    Service->>DB: UPDATE resources, UPDATE usage_requests, INSERT INTO audit_logs
    Service-->>React: Return approved request details

    %% Step 5: Digital QR Pass
    Note over Student, DB: 5. Student Accesses Digital QR Pass
    Student->>React: Open "Digital Pass & QR" (/student/confirmation/{id})
    React->>Controller: GET /api/requests/{id}
    Controller-->>React: Return status APPROVED + requestCode
    React->>React: Render QRCodeSVG(requestCode)

    %% Step 6: Physical Check-In
    Note over Operator, DB: 6. Physical Arrival & QR Verification
    Operator->>React: Open Check-In Terminal -> Scan / Enter code 'QS-2026-XXXXX'
    React->>Controller: POST /api/check-in/verify { requestCode, method: 'QR_SCAN' }
    Controller->>Service: CheckInService.verifyAndCheckIn(dto, operatorEmail)
    Service->>Repo: Verify request is APPROVED and not expired
    Service->>Repo: Update UsageRequest (status = CHECKED_IN)
    Service->>Repo: Update Resource (status = OCCUPIED)
    Service->>Repo: Save CheckIn & Save UsageSession (status = ACTIVE)
    Service->>DB: UPDATE usage_requests, UPDATE resources, INSERT INTO check_ins, INSERT INTO usage_sessions, INSERT INTO audit_logs
    Service-->>React: Return verified confirmation (Resource: OCCUPIED)

    %% Step 7: Checkout & History
    Note over Student, DB: 7. Session Checkout & History Logging
    Student->>React: Click "Complete Session / Check Out"
    React->>Controller: POST /api/requests/{id}/check-out
    Controller->>Service: CheckOutService.checkOut(id, studentEmail, role)
    Service->>Repo: Update UsageRequest (status = COMPLETED)
    Service->>Repo: Update Resource (status = AVAILABLE)
    Service->>Repo: Update UsageSession (status = COMPLETED, endedAt = NOW, duration = X mins)
    Service->>DB: UPDATE usage_requests, UPDATE resources, UPDATE usage_sessions, INSERT INTO audit_logs
    Service-->>React: Return COMPLETED status
    Student->>React: Navigate to History (/student/history)
    React->>Controller: GET /api/history
    Controller->>Repo: UsageSessionRepository.findByStudentOrderByStartedAtDesc()
    DB-->>React: Completed study session record
```

---

## 2. Step-by-Step Code Component Mapping

| Step | Action | Frontend Component | REST API Endpoint | Controller & Service | Database Mutation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | Student Sign In | `LoginPage.jsx` | `POST /api/auth/login` | `AuthController` -> `AuthService` | Inserts `audit_logs` (LOGIN) |
| **2** | View Facilities | `LibrariesList.jsx` | `GET /api/facilities` | `FacilityController` -> `FacilityService` | Read `facilities` |
| **3** | View Live Seating | `FacilityDetails.jsx` | `GET /api/resources/facility/{id}` | `ResourceController` -> `ResourceService` | Read `resources` |
| **4** | Submit Request | `BookingModal.jsx` | `POST /api/requests` | `RequestController` -> `UsageRequestService` | `INSERT INTO usage_requests` (`PENDING`), `INSERT INTO audit_logs` |
| **5** | Track Pending Status | `MyRequests.jsx` | `GET /api/requests/my` | `RequestController` -> `UsageRequestService` | Read `usage_requests` |
| **6** | View Operator Queue | `OperatorDashboard.jsx`, `OperatorRequests.jsx` | `GET /api/operator/requests/pending` | `OperatorController` -> `UsageRequestService` | Read `usage_requests` (`PENDING`) |
| **7** | Operator Approves | `OperatorRequests.jsx` | `POST /api/operator/requests/{id}/approve` | `OperatorController` -> `UsageRequestService` | `UPDATE usage_requests` (`APPROVED`), `UPDATE resources` (`RESERVED`), `INSERT INTO audit_logs` |
| **8** | View QR Pass | `DigitalConfirmation.jsx` | `GET /api/requests/{id}` | `RequestController` -> `UsageRequestService` | Read `usage_requests` (renders `qrcode.react`) |
| **9** | QR / Code Verification | `OperatorCheckIn.jsx` | `POST /api/check-in/verify` | `CheckInController` -> `CheckInService` | `UPDATE usage_requests` (`CHECKED_IN`), `UPDATE resources` (`OCCUPIED`), `INSERT INTO check_ins`, `INSERT INTO usage_sessions` (`ACTIVE`), `INSERT INTO audit_logs` |
| **10** | Student Checkout | `StudentDashboard.jsx`, `MyRequests.jsx` | `POST /api/requests/{id}/check-out` | `RequestController` -> `CheckOutService` | `UPDATE usage_requests` (`COMPLETED`), `UPDATE resources` (`AVAILABLE`), `UPDATE usage_sessions` (`COMPLETED`), `INSERT INTO audit_logs` |
| **11** | View Historical Logs | `StudentHistory.jsx`, `OperatorHistory.jsx` | `GET /api/history` | `HistoryController` -> `UsageSessionRepository` | Read `usage_sessions` |
