# QueueSense — Comprehensive System Audit & Evaluation Report

## 1. Executive Summary

A comprehensive full-system audit was performed on the QueueSense repository across frontend components, REST endpoints, Spring Security, JPA entities, database schema, state machines, and testing infrastructure.

The core workflow (Student Login &rarr; Facility/Resource Discovery &rarr; Booking &rarr; Operator Approval &rarr; Digital Pass & QR &rarr; QR Check-in &rarr; Active Usage &rarr; Checkout &rarr; History) is **functional and verified against PostgreSQL**. However, key security enhancements, facility-scoped operator boundaries, background scheduled jobs, and college policy integrations are required before real-world pilot deployment.

---

## 2. Classified Audit Findings

### [CRITICAL] None
*(No fatal system crashes, SQL injection vulnerabilities, or data loss vectors were found in the tested core workflow.)*

---

### [HIGH] Finding H-01: Broken Object-Level Authorization (BOLA/IDOR) on Request Details
- **Problem**: `GET /api/requests/{id}` and `GET /api/requests/code/{code}` return request details to any authenticated caller without checking if they own the request or supervise the facility.
- **Evidence**: [`RequestController.java` (Lines 40–48)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L40-L48)
- **Why it matters**: A student can enumerate numeric request IDs (`1, 2, 3...`) to inspect other students' names, roll numbers, allocated seats, and active QR request codes.
- **Impact**: Privacy leakage and potential seat hijacking before the genuine student arrives.
- **Recommended Fix**: Verify `request.getStudent().getEmail().equals(callerEmail)` or caller has `ROLE_OPERATOR` before returning response.
- **Priority**: High (Phase 1).

---

### [HIGH] Finding H-02: Missing Facility-Scoped Authorization for Operators
- **Problem**: The backend treats all operators as global administrators across the entire university.
- **Evidence**: [`UsageRequestService.java` (Lines 107–150)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L107-L150)
- **Why it matters**: Dr. Rajesh Kulkarni (assigned to E-Library) can see, approve, or reject reservations belonging to the Sports Complex or Reading Room.
- **Impact**: Operational boundary violation and cross-facility interference.
- **Recommended Fix**: Filter `getPendingRequests()` by the operator's `assignedFacility` and reject approval attempts for foreign facilities.
- **Priority**: High (Phase 1).

---

### [MEDIUM] Finding M-01: Predictable 5-Digit QR Request Code
- **Problem**: Request codes use `SecureRandom.nextInt(90000)` giving strings like `QS-2026-50608` without HMAC cryptographic signatures.
- **Evidence**: [`UsageRequestService.java` (Line 207)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L207)
- **Why it matters**: 90,000 combinations can be brute-forced or guessed if automated check-in turnstiles are ever used without human supervision.
- **Impact**: Acceptable for pilot with desk attendant, but vulnerable for unmanned kiosks.
- **Recommended Fix**: Sign QR payload with HMAC-SHA256 or use standard UUIDv4 tokens.
- **Priority**: Medium (Phase 2).

---

### [MEDIUM] Finding M-02: No Rate Limiting on Login & Verification Endpoints
- **Problem**: `/api/auth/login` and `/api/check-in/verify` have no request rate throttling.
- **Evidence**: [`SecurityConfig.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/config/SecurityConfig.java)
- **Why it matters**: An attacker can launch automated credential stuffing or code enumeration attacks.
- **Impact**: Denial of service or brute-force vulnerability.
- **Recommended Fix**: Add a Bucket4j filter or IP rate-limiter in Spring Security.
- **Priority**: Medium (Phase 2).

---

### [LOW] Finding L-01: Lazy Reservation Hold Expiration (No Background Worker)
- **Problem**: Expired reservations only release when an operator tries to verify that specific code or the user cancels.
- **Evidence**: [`CheckInService.java` (Lines 58–67)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/CheckInService.java#L58-L67)
- **Why it matters**: If a student is a no-show, the resource remains locked in `RESERVED` status indefinitely.
- **Impact**: Inefficient seat utilization during peak library hours.
- **Recommended Fix**: Add a 60-second `@Scheduled` cron job in Spring Boot to sweep and auto-release expired holds.
- **Priority**: Low / Enhancement (Phase 2).

---

### [COSMETIC] Finding C-01: Print CSS Header Artifacts on Digital Confirmation
- **Problem**: When a student clicks "Print Pass", the website top navbar, footer, and action buttons appear on the printed document.
- **Evidence**: [`App.css`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/App.css) missing `@media print` rules.
- **Impact**: Visual clutter on paper confirmation printouts.
- **Recommended Fix**: Add `@media print { .navbar-root, .app-footer, .btn { display: none !important; } }`.
- **Priority**: Cosmetic (Phase 3).

---

## 3. Current Completion Estimates

| Dimension | Completion % | Rationale |
| :--- | :---: | :--- |
| **Frontend** | **92%** | Complete student and operator UI, clean design system, QR generation, camera scanning, modals, and tables. Needs print media queries and mobile drawer navigation. |
| **Backend** | **90%** | All 18 endpoints, controllers, services, DTOs, and global exception handlers working. Needs background expiration scheduler. |
| **Database** | **95%** | Complete 10-table schema with constraints, indexes, enums, and ACID transactions verified. |
| **Security** | **70%** | Strong BCrypt & JWT implementation, but needs BOLA fix, facility-scoped operator isolation, and rate-limiting. |
| **Testing** | **75%** | 5 JUnit workflow tests and full 13-step automated E2E script passing. Needs controller unit tests and Cypress/Playwright frontend tests. |
| **College-Readiness** | **65%** | Core workflow ready for demo pilot; needs university SSO, no-show strike penalties, and multi-department rules. |
| **Production-Readiness** | **55%** | Requires containerization (Docker), environment variable management, reverse proxy (Nginx), SSL/TLS, and log aggregation. |

---

## 4. Recommended Implementation Roadmap

1. **Critical Bugs**: (None identified).
2. **Authorization & Security**:
   - Patch BOLA in `RequestController.java` (`GET /api/requests/{id}`).
   - Enforce facility-scoped query filtering in `UsageRequestService.java`.
   - Externalize JWT secret to environment variable.
3. **Database & Data Integrity**:
   - Add database check constraints and database indexes on `usage_requests(facility_id, status)`.
4. **Booking & State Machine Correctness**:
   - Implement `@Scheduled` background worker for automated no-show hold expiration.
   - Prevent operator from setting resource to `MAINTENANCE` while `OCCUPIED`.
5. **College Requirements**:
   - Interview library and athletic stakeholders (using [`docs/COLLEGE_REQUIREMENTS_QUESTIONS.md`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/docs/COLLEGE_REQUIREMENTS_QUESTIONS.md)).
   - Implement student daily quota limits and penalty strikes.
6. **Frontend UX**:
   - Add `@media print` styles for clean digital pass printing.
   - Introduce responsive left sidebar for Operator portal.
7. **Notifications**:
   - Wire backend `NotificationRepository` to send live in-app alerts on approval/rejection.
8. **Testing**:
   - Add Spring Security mock test suite for 401/403 forbidden cases.
9. **Deployment**:
   - Add `Dockerfile` and `docker-compose.yml` for PostgreSQL + Spring Boot + Nginx.
10. **Documentation**:
    - Keep architecture and student guides up to date.
