# QueueSense — Security Threat Model, Authorization Audit & Vulnerability Assessment

## 1. Security Architecture & Threat Matrix

| # | Threat / Vulnerability | Severity | Current Protection | Missing Protection | Recommended Fix |
| :- | :--- | :--- | :--- | :--- | :--- |
| **1** | **BOLA / IDOR on Request Details** (`GET /api/requests/{id}`) | **HIGH** | Requires authentication. | Any logged-in student can query any other student's request by ID and read their student ID, facility, and QR code. | Check if caller is request owner, assigned operator, or admin before returning DTO. |
| **2** | **Missing Facility Scope in Operator Authorization** | **HIGH** | Restricted to `ROLE_OPERATOR`. | Any operator can approve, reject, or check in requests for facilities other than their assigned facility. | Filter queries by `operator.assignedFacilityId` and enforce checks in service methods. |
| **3** | **Predictable / Unsigned QR Request Codes** | **MEDIUM** | Uses `SecureRandom.nextInt(90000)` (`QS-2026-XXXXX`). | Codes have only 90,000 combinations, no HMAC signature, no cryptographic tamper-proofing. | Sign QR payload with short-lived HMAC token or use UUIDv4 / timestamped hash. |
| **4** | **No Rate Limiting on Login & Check-in Endpoints** | **MEDIUM** | Handled by Spring Security auth. | Vulnerable to automated brute-force attacks on passwords or 5-digit request codes. | Implement bucket4j / Spring Security rate-limiting filter (e.g., max 5 failed attempts per IP/minute). |
| **5** | **Hardcoded JWT Secret in Repository** | **MEDIUM** | Key is 256+ bits long. | Secret is committed in `application.yml`. | Move to environment variable (`${JWT_SECRET}`). |
| **6** | **Lack of Background Hold-Expiry Sweeper** | **LOW** | Lazy check upon verification. | No-show seats remain locked in `RESERVED` until manual interaction. | Implement Spring `@Scheduled` background worker to sweep expired reservations. |
| **7** | **Print Styling Overflow on Digital Confirmation** | **COSMETIC** | Standard browser print dialog. | Navbar and footer print across the digital pass. | Add `@media print` CSS rules to hide navbar/footer and isolate the pass card. |

---

## 2. In-Depth Authorization & Vulnerability Findings

### Finding 1: Broken Object Level Authorization (BOLA) in `RequestController.java`
- **Location**: [`RequestController.java` (Lines 40–48)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L40-L48)
```java
@GetMapping("/{id}")
public ResponseEntity<UsageRequestResponseDto> getRequestById(@PathVariable Long id) {
    return ResponseEntity.ok(usageRequestService.getRequestById(id));
}
```
- **Vulnerability**: Any authenticated student who sends `GET /api/requests/14` can view student Aarav Sharma's full reservation details, including their `requestCode`.
- **Exploitation Impact**: An attacker can enumerate integer IDs to steal other students' QR codes and check into seats before them.
- **Recommended Remediation**:
```java
// Check authorization against current authenticated user:
String currentUserEmail = authentication.getName();
boolean isOwner = request.getStudent().getEmail().equals(currentUserEmail);
boolean isOperator = authentication.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR"));
if (!isOwner && !isOperator) {
    throw new UnauthorizedException("You are not authorized to view this request.");
}
```

---

### Finding 2: Missing Facility-Scoped Authorization for Operators
- **Location**: [`UsageRequestService.java` (Lines 107–120, 122–150)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L107-L150)
- **Vulnerability**: 
  - `getPendingRequests()` performs `findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)` globally across the entire university.
  - `approveRequest()` and `rejectRequest()` do not verify whether the operator's `assignedFacility` matches `request.getFacility()`.
- **Exploitation Impact**: Dr. Rajesh Kulkarni (assigned to E-Library) can approve or reject reservations for the Sports Complex or Reading Room.
- **Recommended Remediation**:
```java
public List<UsageRequestResponseDto> getPendingRequestsForOperator(String operatorEmail) {
    User operator = userRepository.findByEmail(operatorEmail).orElseThrow(...);
    OperatorProfile profile = operatorProfileRepository.findByUser(operator).orElseThrow(...);
    Facility facility = profile.getAssignedFacility();
    
    return usageRequestRepository.findByFacilityAndStatusOrderByCreatedAtDesc(facility, RequestStatus.PENDING)
            .stream().map(this::mapToDto).collect(Collectors.toList());
}
```

---

### Finding 3: QR Code Security Analysis
- **Data Encoded**: Plaintext string `QS-2026-XXXXX` (e.g. `QS-2026-50608`).
- **Cryptographic Assessment**:
  - Is it signed? **No.**
  - Is it encrypted? **No.**
  - Can it be forged? **Yes, if an attacker guesses or intercepts the 5-digit number.**
- **Verdict for Pilot vs Production**:
  - *For a supervised college pilot (human desk attendant)*: **Acceptable**, because the desk operator sees the student's name and photo ID in person before clicking confirm.
  - *For production / turnstiles / automated doors*: **Unacceptable**. Needs an HMAC-SHA256 signature containing `{requestCode, studentId, timestamp, nonce}`.

---

### Finding 4: Digital Confirmation Print Styling Bug Analysis
- **Issue**: Triggering `window.print()` prints the website navigation bar and footer across the top and bottom of the paper sheet.
- **Root Cause**: `Navbar.jsx` and `AppLayout.jsx` lack CSS print media rules (`@media print`).
- **Exact Fix (Do NOT implement yet)**:
```css
/* Add to index.css / App.css */
@media print {
  .navbar-root,
  .app-footer,
  .header-actions,
  .btn,
  .nav-link {
    display: none !important;
  }
  
  body, .app-shell, .app-main {
    background: #ffffff !important;
    padding: 0 !important;
    margin: 0 !important;
  }
  
  .digital-pass-card {
    border: 2px solid #000000 !important;
    box-shadow: none !important;
    page-break-inside: avoid;
    max-width: 100% !important;
  }
}
```

---

### Finding 5: Operator Navigation UI Tradeoff Analysis
- **Current Layout**: Horizontal top navbar with 5 links (`Dashboard`, `Pending Requests`, `Resources`, `QR Check-in`, `History`).
- **Tradeoff Analysis**:
  - *Top Navbar (Current)*: Clean on desktop, but horizontally constrained on tablets/laptops and when longer facility names are displayed.
  - *Sidebar Navigation (Recommended for Operator)*:
    - **Pros**: Scales naturally to 8+ operational tools (Live Queue, Floor Map, Maintenance, Penalties, Analytics, Settings), gives dedicated vertical space for real-time status badges, and provides an "operator terminal" feel.
    - **Cons**: Takes up ~240px of horizontal width; requires responsive drawer collapse for mobile devices.
- **Recommendation**: Maintain top navbar for Students (simpler consumer workflow); transition Operator to a collapsable left sidebar in Phase 2.
