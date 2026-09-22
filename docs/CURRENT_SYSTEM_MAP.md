# QueueSense — Comprehensive System Map & Takeover Audit

> **Target Audience**: B.Tech Computer Science & Engineering students, academic reviewers, and full-stack developers.  
> **Status**: Read-Only Takeover Audit & Complete Architecture Specification.  
> **Repository Location**: `C:\My_career_Projects_Ideas_lab\QueueSense-FullStack`

---

## 📖 Table of Contents
1. [Foundational Full-Stack Concepts for Beginners](#1-foundational-full-stack-concepts-for-beginners)
2. [Technology Stack & Selection Rationale](#2-technology-stack--selection-rationale)
3. [End-to-End System Architecture](#3-end-to-end-system-architecture)
4. [Complete Student Workflow Trace](#4-complete-student-workflow-trace)
5. [Complete Operator Workflow Trace](#5-complete-operator-workflow-trace)
6. [Database Schema, Entities & Relational Design](#6-database-schema-entities--relational-design)
7. [Authentication Mechanics (Deep Dive)](#7-authentication-mechanics-deep-dive)
8. [Authorization & Role-Based Access Control (RBAC)](#8-authorization--role-based-access-control-rbac)
9. [Finite State Machines & Lifecycle Invariants](#9-finite-state-machines--lifecycle-invariants)
10. [Anatomy of a Single Action: "Approve Request" Step-by-Step Trace](#10-anatomy-of-a-single-action-approve-request-step-by-step-trace)
11. [Audit Findings: Known Bugs, Security Vulnerabilities & Edge Cases](#11-audit-findings-known-bugs-security-vulnerabilities--edge-cases)
12. [Feature Matrix: Implemented vs. Missing](#12-feature-matrix-implemented-vs-missing)
13. [Git, Secrets & Repository Readiness Assessment](#13-git-secrets--repository-readiness-assessment)
14. [Actionable Next Steps & Student Learning Roadmap](#14-actionable-next-steps--student-learning-roadmap)

---

# 1. Foundational Full-Stack Concepts for Beginners

Before diving into QueueSense's codebase, here is a simple explanation of every foundational concept used across this project:

| Concept | Plain English Analogy | Technical Definition & QueueSense Context |
| :--- | :--- | :--- |
| **React** | A Lego kit for building user interfaces out of reusable, dynamic blocks. | A JavaScript library created by Meta for rendering declarative, component-driven user interfaces. It updates only the specific elements on the screen that change rather than refreshing the entire page. |
| **Vite** | A lightning-fast engine starter and build machine. | A modern frontend build tool and development server that bundles JavaScript/React code with sub-second hot reload times and proxies API calls to port `8080`. |
| **JSX** | Writing HTML-like syntax directly inside JavaScript. | JavaScript XML — an extension of JavaScript that allows developers to describe UI elements using familiar HTML-like syntax inside `.jsx` files. |
| **React Component** | A reusable custom UI brick (e.g., a "Status Badge" or "Booking Modal"). | A self-contained JavaScript function that accepts inputs (`props`), manages internal memory (`state`), and returns JSX describing what should appear on the screen. |
| **State (`useState`)** | A component's short-term memory. | A JavaScript object or value managed inside a component that, when updated (e.g., when new data arrives from the server), automatically causes the component to re-render. |
| **Props** | Arguments passed to a function to customize how it behaves. | Short for *properties* — read-only parameters passed from a parent component down to a child component (e.g., `<StatusBadge status="AVAILABLE" />`). |
| **Context (`useContext`)** | A campus-wide loudspeaker broadcasting information to all rooms at once. | A React mechanism to share data globally across the entire component tree (such as current logged-in user profile and JWT token) without passing props through every intermediate component. |
| **`localStorage`** | A tiny locker inside the student's web browser that survives page reloads. | A key-value storage engine built into web browsers that stores persistent strings (such as `queuesense_token` and `queuesense_user`) across browser refreshes and tab closures. |
| **HTTP Request** | Sending a letter to a server asking for data or requesting an action. | Hypertext Transfer Protocol message sent from browser to server containing a Method (`GET`, `POST`), a URL Path, Headers (e.g., authorization token), and an optional Body (JSON payload). |
| **REST API** | A standardized restaurant menu for ordering data over the web. | Representational State Transfer Application Programming Interface — a software architectural style where backend endpoints (URLs) represent resources and standard HTTP methods perform actions on them. |
| **HTTP Methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)** | Action verbs for server interactions. | - `GET`: Read data (e.g., `GET /api/facilities`).<br>- `POST`: Create a new record or trigger an action (e.g., `POST /api/requests`).<br>- `PUT`: Replace an entire record.<br>- `PATCH`: Partially update specific fields of a record (e.g., `PATCH /api/operator/resources/1/status`).<br>- `DELETE`: Remove a record. |
| **JSON** | A universal, human-readable text format for sharing structured data. | JavaScript Object Notation — lightweight data interchange format using key-value pairs (`{"studentName": "Aarav", "status": "PENDING"}`). |
| **Axios** | A messenger service delivering HTTP requests and handling replies. | A Promise-based HTTP client library for the browser and Node.js that simplifies network requests, automatic JSON transformation, and request/response interception (see [`api.js`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/services/api.js)). |
| **Spring Boot** | A robust Java framework that takes care of heavy infrastructure so you can write business rules. | An enterprise-grade Java framework that provides embedded web servers (Tomcat), dependency injection, automated configuration, and production-ready security filters. |
| **Controller** | The front-desk receptionist of the backend application. | A Spring Java class annotated with `@RestController` that receives incoming HTTP requests, unpacks URLs and JSON bodies, validates input, calls business services, and returns HTTP responses. |
| **Service** | The brain and decision-maker of the backend. | A Spring Java class annotated with `@Service` where core university business logic, eligibility checks, state transitions, and ACID transactions are executed. |
| **Repository** | The librarian who goes to the database shelves to fetch or store records. | A Spring Data JPA interface (`extends JpaRepository<T, ID>`) that provides automated SQL query generation without writing manual SQL strings. |
| **JPA & Hibernate** | The universal translator between Java classes and SQL database tables. | **JPA** (Java Persistence API) is the Java standard specification for Object-Relational Mapping (ORM). **Hibernate** is the actual engine that implements JPA, converting Java objects into SQL `INSERT`, `UPDATE`, and `SELECT` statements. |
| **Entity** | A Java class that represents a single database table. | A Java class annotated with `@Entity` and `@Table` (e.g., `UsageRequest.java`) where each instance corresponds to a row in that table. |
| **DTO (Data Transfer Object)** | A specialized envelope designed specifically for sending data over the wire. | A lightweight Java class (e.g., `UsageRequestResponseDto.java`) that packages only the necessary fields for API inputs and outputs, preventing direct exposure of internal database entity structures. |
| **PostgreSQL** | A professional, battle-tested electronic filing cabinet. | An open-source, enterprise-grade relational database management system (RDBMS) that stores structured tables on disk with strong consistency guarantees. |
| **Primary Key (PK)** | A student's unique university roll number. | A database column (typically `id BIGINT`) containing a unique identifier for every row in a table, ensuring no two rows are identical. |
| **Foreign Key (FK)** | A reference on a library card pointing to the registered student's master file. | A column in one table that stores the Primary Key of another table, creating a relational link (e.g., `student_id` in `usage_requests` referencing `id` in `users`). |
| **Database JOIN** | Combining two related spreadsheets into one view based on matching IDs. | A SQL operation that connects rows from two or more tables based on a related column between them (e.g., joining `usage_requests` with `resources` and `users`). |
| **Database Transaction & ACID** | An "all-or-nothing" contract for database operations. | A sequence of database operations executed as a single atomic unit. If any step fails, all previous steps are rolled back.<br>- **A**tomicity: All succeed or all fail.<br>- **C**onsistency: Data always follows constraints.<br>- **I**solation: Concurrent operations do not corrupt each other.<br>- **D**urability: Committed data is permanently saved to disk. |
| **JWT (JSON Web Token)** | A tamper-proof digital wristband stamped with your identity and role. | A compact, URL-safe string containing cryptographic signatures (`Header.Payload.Signature`) used to verify a user's identity on every API call without storing session state in server memory. |
| **Authentication vs. Authorization** | *Who are you?* vs. *What are you allowed to do?* | - **Authentication**: Verifying who the user is (e.g., checking email & password at `/api/auth/login`).<br>- **Authorization**: Checking permissions (e.g., ensuring only `ROLE_OPERATOR` can access `/api/operator/requests/pending`). |
| **Role-Based Access Control (RBAC)** | Giving library keys only to librarians and athletic passes to coaches. | A security model where system access permissions are assigned to specific roles (`ROLE_STUDENT`, `ROLE_OPERATOR`, `ROLE_ADMIN`) rather than individual users. |
| **CORS (Cross-Origin Resource Sharing)** | A security guard checking if requests from `localhost:5173` are allowed to talk to `localhost:8080`. | A browser security mechanism that restricts web pages running on one domain/port from making HTTP requests to a different domain/port unless the server explicitly allows it. |
| **Hashing & BCrypt** | A one-way meat grinder for passwords. | A cryptographic mathematical function that converts plaintext into a scrambled irreversible string. **BCrypt** incorporates a salt (random data) and multiple work rounds to protect against brute-force rainbow table attacks. |
| **QR Code** | A 2D barcode easily scanned by smartphones or webcams. | Quick Response code — an optical label storing encoded alphanumeric data (such as request code `QS-2026-50608`) for fast check-in verification. |
| **State Machine** | A traffic light with strict rules on which color can follow which. | A formal behavioral model where an entity can only exist in one of several predefined states at a time, and transitions between states are governed by strict conditional triggers. |
| **CRUD** | The 4 basic database verbs. | **C**reate (`INSERT`), **R**ead (`SELECT`), **U**pdate (`UPDATE`), **D**elete (`DELETE`). |
| **HTTP Status Codes** | Standardized 3-digit server status responses. | - `200 OK`: Request succeeded.<br>- `201 Created`: New record successfully inserted.<br>- `400 Bad Request`: Client submitted invalid parameters or violated state machine logic.<br>- `401 Unauthorized`: Missing or invalid credentials/token (*"Who are you?"*).<br>- `403 Forbidden`: Authenticated, but lacking required permissions (*"You are a student, but this is an operator-only desk"*).<br>- `404 Not Found`: Target resource does not exist.<br>- `409 Conflict`: Duplicate entry or conflicting active request.<br>- `500 Internal Server Error`: Unhandled server exception. |

---

# 2. Technology Stack & Selection Rationale

QueueSense was intentionally architected using an industry-standard full-stack enterprise stack:

```
+-------------------------------------------------------------------------------+
| FRONTEND: React 19 + Vite + React Router DOM v7 + Axios + Lucide + QR Tools   |
+-------------------------------------------------------------------------------+
                                       │ HTTP REST (JSON + Bearer JWT)
+-------------------------------------------------------------------------------+
| BACKEND: Spring Boot 3.3.5 (Java 23) + Spring Security + Spring Data JPA      |
+-------------------------------------------------------------------------------+
                                       │ JDBC (HikariCP Connection Pool)
+-------------------------------------------------------------------------------+
| DATABASE: PostgreSQL 16.3 Relational Database Server                          |
+-------------------------------------------------------------------------------+
```

### Why each technology was selected:
1. **React 19**:
   - *Why*: Enables a responsive Single Page Application (SPA). Students see instant seating updates and dynamic QR codes without full-page reloads.
2. **Vite**:
   - *Why*: Modern replacement for legacy Create-React-App. Provides sub-second local build times and an embedded reverse proxy (`/api` &rarr; `http://localhost:8080`) to eliminate cross-origin configuration friction during local development.
3. **Spring Boot 3.3.5 & Java 23**:
   - *Why*: Enterprise-grade stability, strongly typed compile-time safety, built-in dependency injection, and declarative transaction management (`@Transactional`).
4. **Spring Data JPA & Hibernate**:
   - *Why*: Eliminates boilerplate SQL strings, handles object-relational mapping, and automatically syncs entity lifecycle states with PostgreSQL tables.
5. **Spring Security & JJWT (0.12.6)**:
   - *Why*: Stateless, token-based authentication. Eliminates server-side session memory consumption and scales horizontally across multiple servers.
6. **PostgreSQL 16.3**:
   - *Why*: ACID-compliant relational database. Critical for resource booking systems where two students cannot simultaneously hold the same seat (`SELECT ... FOR UPDATE` & unique constraints).
7. **`qrcode.react` & `html5-qrcode`**:
   - *Why*: Allows real vector SVG QR code generation on student screens and live camera barcode scanning in the operator desk browser without needing proprietary external hardware.

---

# 3. End-to-End System Architecture

The following diagram illustrates how every layer in QueueSense communicates from the browser down to physical storage on disk:

```
[ CLIENT TIER ]
   │
   ├─► Student / Operator Web Browser
   │     │
   │     ▼
   ├─► React 19 UI Components (Pages, Modals, Badges, QR Views)
   │     │
   │     ▼
   └─► Axios API Gateway (frontend/src/services/api.js)
         - Injects Authorization: Bearer <token>
         - Intercepts 401 Unauthorized -> redirects to /login
         │
═════════════════════════════════════════════════════════════════════
         │ HTTP REST + JSON (Port 8080)
═════════════════════════════════════════════════════════════════════
         │
[ APPLICATION TIER: Spring Boot 3.3.5 ]
   │
   ├─► JwtAuthenticationFilter (backend/src/main/java/com/queuesense/security/)
   │     - Validates cryptographic signature
   │     - Extracts username & authorities
   │     - Populates SecurityContextHolder
   │     │
   │     ▼
   ├─► REST Controllers (backend/src/main/java/com/queuesense/controller/)
   │     - Maps URLs & HTTP verbs
   │     - Validates payload annotations (@Valid, @NotNull)
   │     - Maps DTOs
   │     │
   │     ▼
   ├─► Service Layer (backend/src/main/java/com/queuesense/service/)
   │     - Enforces state machines and university rules
   │     - Manages database transaction boundaries (@Transactional)
   │     - Records immutable audit trails (AuditLogService)
   │     │
   │     ▼
   ├─► Spring Data JPA Repositories (backend/src/main/java/com/queuesense/repository/)
   │     - Generates type-safe SQL queries
   │     - Provides entity caching and persistence contexts
   │     │
   │     ▼
   └─► Hibernate ORM & HikariCP Connection Pool
         │
═════════════════════════════════════════════════════════════════════
         │ JDBC Connection (Port 5432)
═════════════════════════════════════════════════════════════════════
         │
[ DATA PERSISTENCE TIER: PostgreSQL 16.3 ]
   │
   └─► 10 Relational Tables:
         - users, student_profiles, operator_profiles
         - facilities, resources, usage_requests
         - check_ins, usage_sessions, audit_logs, notifications
```

---

# 4. Complete Student Workflow Trace

This section traces every step a student takes through the application from sign-in to checking out and viewing history.

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student (Browser)
    participant UI as React UI (JSX)
    participant Axios as Axios Client (api.js)
    participant Sec as Spring Security Filter
    participant Ctrl as REST Controller
    participant Svc as Service Layer
    participant DB as PostgreSQL Database

    Note over Student, DB: 1. Student Sign In
    Student->>UI: Enter email & password
    UI->>Axios: POST /api/auth/login { email, password }
    Axios->>Ctrl: AuthController.login()
    Ctrl->>Svc: AuthService.login()
    Svc->>DB: Query User & verify BCrypt hash
    Svc->>DB: INSERT INTO audit_logs (LOGIN)
    Svc-->>UI: Return JWT Token + User profile
    UI->>UI: Save token in localStorage & set AuthContext

    Note over Student, DB: 2. Facility & Resource Browsing
    Student->>UI: Browse Facilities -> Select E-Library
    UI->>Axios: GET /api/facilities/1 & GET /api/resources/facility/1
    Axios->>Ctrl: FacilityController & ResourceController
    Ctrl->>DB: SELECT * FROM resources WHERE facility_id = 1
    DB-->>UI: Return list of Seats (AVAILABLE, RESERVED, OCCUPIED)

    Note over Student, DB: 3. Create Booking Request
    Student->>UI: Click "Request Seat 01" -> Submit BookingModal
    UI->>Axios: POST /api/requests { resourceId: 1, expectedArrival, duration }
    Axios->>Sec: Attach Authorization: Bearer <token>
    Sec->>Ctrl: RequestController.createRequest()
    Ctrl->>Svc: UsageRequestService.createRequest()
    Svc->>DB: Check active requests & resource status
    Svc->>DB: INSERT INTO usage_requests (status='PENDING', code='QS-2026-XXXXX')
    Svc->>DB: INSERT INTO audit_logs (REQUEST_CREATED)
    Svc-->>UI: Return UsageRequestResponseDto
    UI->>UI: Redirect to My Requests (Shows PENDING)

    Note over Student, DB: 4. Digital Confirmation Pass
    Student->>UI: Open Digital Pass (/student/confirmation/1)
    UI->>Axios: GET /api/requests/1
    Axios->>Ctrl: RequestController.getRequestById(1)
    Ctrl->>DB: SELECT * FROM usage_requests WHERE id = 1
    DB-->>UI: Return request details (status='APPROVED', code='QS-2026-XXXXX')
    UI->>UI: Render qrcode.react (SVG barcode)

    Note over Student, DB: 5. Checkout & History
    Student->>UI: Click "Check Out"
    UI->>Axios: POST /api/requests/1/check-out
    Axios->>Ctrl: RequestController.checkOut(1)
    Ctrl->>Svc: CheckOutService.checkOut()
    Svc->>DB: UPDATE usage_requests (status='COMPLETED')
    Svc->>DB: UPDATE resources (status='AVAILABLE')
    Svc->>DB: UPDATE usage_sessions (status='COMPLETED', duration=X mins)
    Svc->>DB: INSERT INTO audit_logs (CHECK_OUT)
    Svc-->>UI: Return updated status
    UI->>UI: Navigate to History (/student/history)
```

### Detailed Step-by-Step Technical Breakdown:

#### Step 1: Student Login
- **User Action**: Inputs `student@queuesense.demo` / `Student@123` on [`LoginPage.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/auth/LoginPage.jsx).
- **React Component**: `LoginPage.jsx` calls `login()` from [`AuthContext.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/context/AuthContext.jsx).
- **API Endpoint**: `POST /api/auth/login`.
- **Backend Controller**: [`AuthController.java` (Line 21)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/AuthController.java#L21).
- **Service**: [`AuthService.java` (Line 45)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/AuthService.java#L45).
- **Repository / DB Operation**: `userRepository.findByEmail()` followed by `BCryptPasswordEncoder.matches()`.
- **DB Records Changed**: Inserts 1 record into `audit_logs` (`action = "LOGIN"`).
- **Response Returned**: `200 OK` with [`AuthResponse`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/dto/AuthResponse.java) JSON containing JWT token, user name, role (`ROLE_STUDENT`), and `studentId` (`STU-2026-001`).
- **Frontend Action**: Stores token in `localStorage.getItem('queuesense_token')`, sets React state, and navigates to `/student/dashboard`.

#### Step 2: Facility & Resource Discovery
- **User Action**: Student clicks "Explore Libraries" &rarr; "E-Library".
- **React Component**: [`LibrariesList.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/LibrariesList.jsx) & [`FacilityDetails.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/FacilityDetails.jsx).
- **API Endpoint**: `GET /api/facilities/1` and `GET /api/resources/facility/1`.
- **Backend Controller**: [`FacilityController.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/FacilityController.java) & [`ResourceController.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/ResourceController.java).
- **Service**: `FacilityService.java` & `ResourceService.java`.
- **Repository / DB Operation**: `facilityRepository.findById(1)` and `resourceRepository.findByFacilityAndActiveTrue(facility)`.
- **DB Records Changed**: None (Read-only query).
- **Response Returned**: Array of resources with real-time status (`AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`).
- **Frontend Action**: Displays color-coded grid of seats/PCs with action buttons enabled only for `AVAILABLE` items.

#### Step 3: Create Usage Request
- **User Action**: Clicks "Request Seat 01", selects "Immediate (Next 10 mins)", "1 hour", "1 student", and clicks "Submit Usage Request".
- **React Component**: [`BookingModal.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/components/booking/BookingModal.jsx).
- **API Endpoint**: `POST /api/requests`.
- **Backend Controller**: [`RequestController.java` (Line 27)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L27).
- **Service**: [`UsageRequestService.java` (Line 50)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L50).
- **Repository / DB Operation**:
  1. Validates resource status is `AVAILABLE`.
  2. Runs `usageRequestRepository.existsByStudentAndStatusIn(student, [PENDING, APPROVED, CHECKED_IN])` to block duplicates.
  3. Generates unique request code (e.g. `QS-2026-50608`).
  4. Saves `UsageRequest`.
- **DB Records Changed**:
  - `INSERT INTO usage_requests` (`status = 'PENDING'`, `request_code = 'QS-2026-50608'`).
  - `INSERT INTO audit_logs` (`action = 'REQUEST_CREATED'`).
- **Response Returned**: `201 Created` with `UsageRequestResponseDto`.
- **Frontend Action**: Closes modal and navigates to `/student/requests`.

#### Step 4: Accessing Digital QR Pass
- **User Action**: After operator approval, student clicks "Digital Pass & QR" from [`MyRequests.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/MyRequests.jsx).
- **React Component**: [`DigitalConfirmation.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/DigitalConfirmation.jsx).
- **API Endpoint**: `GET /api/requests/{id}`.
- **Backend Controller**: [`RequestController.java` (Line 40)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L40).
- **Service**: `UsageRequestService.getRequestById(id)`.
- **Repository / DB Operation**: `usageRequestRepository.findById(id)`.
- **DB Records Changed**: None (Read-only).
- **Response Returned**: `200 OK` with request details including `status: "APPROVED"`, `requestCode: "QS-2026-50608"`, and `expiresAt`.
- **Frontend Action**: Renders `<QRCodeSVG value="QS-2026-50608" />`, displays a 10-minute countdown timer, and shows verification guidelines.

#### Step 5: Checkout
- **User Action**: Student clicks "Complete Session / Check Out" from dashboard or requests page.
- **React Component**: [`StudentDashboard.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/StudentDashboard.jsx) or [`MyRequests.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/MyRequests.jsx).
- **API Endpoint**: `POST /api/requests/{id}/check-out`.
- **Backend Controller**: [`RequestController.java` (Line 56)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L56).
- **Service**: [`CheckOutService.java` (Line 39)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/CheckOutService.java#L39).
- **Repository / DB Operation**:
  1. Verifies request status is `CHECKED_IN`.
  2. Updates `UsageRequest.status` to `COMPLETED`.
  3. Updates `Resource.status` to `AVAILABLE`.
  4. Finds linked `UsageSession`, sets `endedAt = NOW`, calculates `actualDurationMinutes`, and sets status to `COMPLETED`.
- **DB Records Changed**:
  - `UPDATE usage_requests SET status = 'COMPLETED', check_out_at = NOW() WHERE id = ?`.
  - `UPDATE resources SET status = 'AVAILABLE' WHERE id = ?`.
  - `UPDATE usage_sessions SET ended_at = NOW(), actual_duration_minutes = ?, status = 'COMPLETED' WHERE usage_request_id = ?`.
  - `INSERT INTO audit_logs` (`action = 'CHECK_OUT'`).
- **Response Returned**: `200 OK` with completed DTO.
- **Frontend Action**: Updates local UI state and shows a green success badge.

#### Step 6: History
- **User Action**: Clicks "History" on navbar.
- **React Component**: [`StudentHistory.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/student/StudentHistory.jsx).
- **API Endpoint**: `GET /api/history`.
- **Backend Controller**: [`HistoryController.java` (Line 27)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/HistoryController.java#L27).
- **Repository / DB Operation**: `usageSessionRepository.findByStudentOrderByStartedAtDesc(student)`.
- **DB Records Changed**: None (Read-only).
- **Response Returned**: Array of past study sessions with facility names, seat labels, start times, and duration in minutes.
- **Frontend Action**: Renders chronological session history cards with statistics.

---

# 5. Complete Operator Workflow Trace

This section traces the operator desk supervisory workflow:

```mermaid
sequenceDiagram
    autonumber
    actor Operator as Operator (Browser)
    participant UI as React UI (Operator Pages)
    participant Ctrl as REST Controller
    participant Svc as Service Layer
    participant DB as PostgreSQL Database

    Note over Operator, DB: 1. Operator Sign In & Dashboard
    Operator->>UI: Sign in (operator@queuesense.demo)
    UI->>Ctrl: GET /api/operator/dashboard
    Ctrl->>Svc: DashboardService.getDashboardStats()
    Svc->>DB: Run COUNT queries for Pending, Available, Occupied, CheckIns
    DB-->>UI: Return aggregate counts
    UI->>UI: Render live operational metric cards

    Note over Operator, DB: 2. Review & Approve Pending Queue
    Operator->>UI: Navigate to Pending Requests
    UI->>Ctrl: GET /api/operator/requests/pending
    Ctrl->>DB: SELECT * FROM usage_requests WHERE status = 'PENDING'
    DB-->>UI: List of student requests
    Operator->>UI: Click "Approve" on Request #14
    UI->>Ctrl: POST /api/operator/requests/14/approve
    Ctrl->>Svc: UsageRequestService.approveRequest(14)
    Svc->>DB: UPDATE usage_requests (status='APPROVED', expires_at=NOW+10m)
    Svc->>DB: UPDATE resources (status='RESERVED')
    Svc->>DB: INSERT INTO audit_logs (REQUEST_APPROVED)
    Svc-->>UI: Return approved request details
    UI->>UI: Move item from Pending to Approved tab

    Note over Operator, DB: 3. Verify Student QR Check-In
    Operator->>UI: Open QR Scanner Terminal -> Camera reads code 'QS-2026-50608'
    UI->>Ctrl: POST /api/check-in/verify { requestCode: 'QS-2026-50608', method: 'QR_SCAN' }
    Ctrl->>Svc: CheckInService.verifyAndCheckIn()
    Svc->>DB: Verify status is APPROVED and not expired
    Svc->>DB: UPDATE usage_requests (status='CHECKED_IN', check_in_at=NOW)
    Svc->>DB: UPDATE resources (status='OCCUPIED')
    Svc->>DB: INSERT INTO check_ins (request_id, operator_id, method)
    Svc->>DB: INSERT INTO usage_sessions (status='ACTIVE', started_at=NOW)
    Svc->>DB: INSERT INTO audit_logs (CHECK_IN)
    Svc-->>UI: Return verified session confirmation
    UI->>UI: Display verified banner with student name & seat assignment
```

### Detailed Operator Technical Breakdown:

#### Step 1: Operator Dashboard & Real-Time Stats
- **User Action**: Operator logs in and views `/operator/dashboard`.
- **React Component**: [`OperatorDashboard.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/operator/OperatorDashboard.jsx).
- **API Endpoint**: `GET /api/operator/dashboard`.
- **Backend Controller**: [`OperatorController.java` (Line 36)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/OperatorController.java#L36).
- **Service**: [`DashboardService.java` (Line 29)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/DashboardService.java#L29).
- **Repository / DB Operation**: Executes concurrent `countByStatus()` calls on `usage_requests` and `resources`, plus `countTodayCheckIns()` on `check_ins`.
- **DB Records Changed**: None.
- **Response Returned**: [`DashboardStatsDto`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/dto/DashboardStatsDto.java) (`pendingRequestsCount`, `availableResourcesCount`, `reservedResourcesCount`, `occupiedResourcesCount`, `maintenanceResourcesCount`, `totalResourcesCount`, `todayCheckInsCount`).
- **Frontend Action**: Renders 6 statistical summary cards with quick-action links.

#### Step 2: Managing Pending Requests
- **User Action**: Views list of student requests and clicks "Approve" or "Reject".
- **React Component**: [`OperatorRequests.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/operator/OperatorRequests.jsx).
- **API Endpoints**:
  - `GET /api/operator/requests/pending`
  - `POST /api/operator/requests/{id}/approve`
  - `POST /api/operator/requests/{id}/reject` (with JSON body `{"rejectionReason": "..."}`)
- **Backend Controller**: [`OperatorController.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/OperatorController.java).
- **Service**: [`UsageRequestService.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java).
- **DB Records Changed**:
  - On Approval: `usage_requests.status = 'APPROVED'`, `expires_at = NOW() + 10 mins`, `resources.status = 'RESERVED'`, `audit_logs` inserted.
  - On Rejection: `usage_requests.status = 'REJECTED'`, `rejection_reason = ?`, `audit_logs` inserted.
- **Frontend Action**: Refreshes queue state and updates tab badge counts.

#### Step 3: QR Code Verification & Physical Check-In
- **User Action**: Operator uses laptop camera to scan student's QR code or types code `QS-2026-50608` into manual entry field.
- **React Component**: [`OperatorCheckIn.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/operator/OperatorCheckIn.jsx) (integrating `html5-qrcode`).
- **API Endpoint**: `POST /api/check-in/verify`.
- **Backend Controller**: [`CheckInController.java` (Line 23)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/CheckInController.java#L23).
- **Service**: [`CheckInService.java` (Line 44)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/CheckInService.java#L44).
- **Repository / DB Operation**:
  1. Fetches request by `requestCode`.
  2. Validates status is `APPROVED`.
  3. Validates current time is before `expiresAt`.
  4. Changes request status to `CHECKED_IN`.
  5. Changes resource status to `OCCUPIED`.
  6. Creates `CheckIn` entity.
  7. Creates `UsageSession` entity (`status = 'ACTIVE'`).
- **DB Records Changed**:
  - `UPDATE usage_requests SET status = 'CHECKED_IN', check_in_at = NOW()`.
  - `UPDATE resources SET status = 'OCCUPIED'`.
  - `INSERT INTO check_ins (usage_request_id, operator_id, method, checked_in_at)`.
  - `INSERT INTO usage_sessions (usage_request_id, resource_id, student_id, started_at, status)`.
  - `INSERT INTO audit_logs (action = 'CHECK_IN')`.
- **Response Returned**: `200 OK` with updated `UsageRequestResponseDto`.
- **Frontend Action**: Displays large green verification card showing student name, department, seat label, and timestamp.

#### Step 4: Resource Status & Maintenance Overrides
- **User Action**: Operator toggles a broken PC or seat to `MAINTENANCE`.
- **React Component**: [`OperatorResources.jsx`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/pages/operator/OperatorResources.jsx).
- **API Endpoint**: `PATCH /api/operator/resources/{id}/status` with body `{"status": "MAINTENANCE"}`.
- **Backend Controller**: `OperatorController.java (Line 69)`.
- **Service**: `ResourceService.java (Line 36)`.
- **DB Records Changed**: `UPDATE resources SET status = 'MAINTENANCE' WHERE id = ?`, inserts `audit_logs`.
- **Frontend Action**: Resource card turns gray with a Maintenance badge.

---

# 6. Database Schema, Entities & Relational Design

The database runs on PostgreSQL 16.3 and contains **10 relational tables**:

```
                       ┌─────────────────────────┐
                       │          users          │
                       └───────────┬─────────────┘
                                   │1
                   ┌───────────────┼───────────────┐
                  1│              1│              1│
          ┌────────▼──────┐ ┌──────▼────────┐ ┌────▼──────────────┐
          │student_profiles│ │operator_      │ │  notifications    │
          └───────────────┘ │profiles       │ └───────────────────┘
                            └──────┬────────┘
                                   │*
                                   │1
                            ┌──────▼────────┐
                            │  facilities   │
                            └──────┬────────┘
                                   │1
                                   │*
                            ┌──────▼────────┐
                            │   resources   │
                            └──────┬────────┘
                                   │1
                                   │*
                            ┌──────▼────────┐
                            │ usage_requests│
                            └──────┬────────┘
                                   │1
                   ┌───────────────┴───────────────┐
                  1│                              1│
          ┌────────▼──────┐               ┌────────▼────────┐
          │   check_ins   │               │ usage_sessions  │
          └───────────────┘               └─────────────────┘

          ┌───────────────┐
          │  audit_logs   │ (Standalone immutable event log)
          └───────────────┘
```

### Table-by-Table Schema Specification

#### 1. `users` Table
- **Purpose**: Master table for authentication and user identities across campus.
- **Columns**: `id` (PK, BigInt), `email` (Unique, VarChar 100), `password_hash` (VarChar 255), `role` (VarChar 30: `ROLE_STUDENT`, `ROLE_OPERATOR`, `ROLE_ADMIN`), `name` (VarChar 100), `active` (Boolean), `created_at`, `updated_at`.
- **Relationships**:
  - Has one `student_profiles` record (if student).
  - Has one `operator_profiles` record (if operator).
  - Has many `usage_requests` (as student).
  - Has many `check_ins` (as operator).
  - Has many `notifications`.

#### 2. `student_profiles` Table
- **Purpose**: Stores student-specific academic information.
- **Columns**: `id` (PK), `user_id` (FK &rarr; `users.id`, Unique), `student_id` (Unique, VarChar 50 e.g. `STU-2026-001`), `department` (VarChar 100), `year` (Integer).
- **Why relationship exists**: Separates generic login credentials (`users`) from university academic records (`student_profiles`).

#### 3. `operator_profiles` Table
- **Purpose**: Stores campus staff assignments and facility supervision scopes.
- **Columns**: `id` (PK), `user_id` (FK &rarr; `users.id`, Unique), `employee_id` (Unique, VarChar 50 e.g. `EMP-2026-088`), `assigned_facility_id` (FK &rarr; `facilities.id`, Nullable).
- **Why relationship exists**: Associates staff members with the physical facility they are authorized to manage.

#### 4. `facilities` Table
- **Purpose**: Represents physical university venues (libraries, labs, reading rooms, sports courts).
- **Columns**: `id` (PK), `name` (VarChar 100 e.g. "E-Library"), `type` (VarChar 50: `LIBRARY`, `SPORTS`), `description` (VarChar 500), `active` (Boolean), `created_at`.
- **Relationships**: Has many `resources`.

#### 5. `resources` Table
- **Purpose**: Represents individual bookable units (seats, computers, desks, courts).
- **Columns**: `id` (PK), `facility_id` (FK &rarr; `facilities.id`), `name` (VarChar 100 e.g. "Seat 01"), `type` (VarChar 50: `SEAT`, `PC`, `DESK`, `COURT`), `status` (VarChar 30: `AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`), `active` (Boolean).
- **Why relationship exists**: Multiple resources belong to one facility.

#### 6. `usage_requests` Table
- **Purpose**: Central transactional entity capturing every booking request submitted by a student.
- **Columns**: `id` (PK), `request_code` (Unique, VarChar 50 e.g. `QS-2026-50608`), `student_id` (FK &rarr; `users.id`), `resource_id` (FK &rarr; `resources.id`), `facility_id` (FK &rarr; `facilities.id`), `expected_arrival` (VarChar 50), `expected_duration` (VarChar 50), `group_size` (Integer), `status` (VarChar 30: `PENDING`, `APPROVED`, `REJECTED`, `CHECKED_IN`, `COMPLETED`, `CANCELLED`, `EXPIRED`), `created_at`, `approved_at`, `rejected_at`, `rejection_reason`, `check_in_at`, `check_out_at`, `expires_at`.
- **Relationships**:
  - Links student, resource, and facility.
  - Has one `check_ins` record.
  - Has one `usage_sessions` record.

#### 7. `check_ins` Table
- **Purpose**: Immutable proof of physical presence logged when an operator verifies a student's QR code.
- **Columns**: `id` (PK), `usage_request_id` (FK &rarr; `usage_requests.id`, Unique), `operator_id` (FK &rarr; `users.id`), `method` (VarChar 30: `QR_SCAN`, `MANUAL_CODE`), `checked_in_at`.
- **Why relationship exists**: Proves who verified the student's entry, when, and via what method.

#### 8. `usage_sessions` Table
- **Purpose**: Tracks active and concluded study durations for analytics and student history.
- **Columns**: `id` (PK), `usage_request_id` (FK &rarr; `usage_requests.id`, Unique), `resource_id` (FK &rarr; `resources.id`), `student_id` (FK &rarr; `users.id`), `started_at`, `ended_at`, `actual_duration_minutes` (BigInt), `status` (VarChar 30: `ACTIVE`, `COMPLETED`).
- **Why relationship exists**: Separates the pre-arrival booking reservation request from the actual physical usage duration.

#### 9. `audit_logs` Table
- **Purpose**: System-wide security and compliance ledger recording every critical state transition.
- **Columns**: `id` (PK), `actor_email` (VarChar 100), `actor_role` (VarChar 50), `action` (VarChar 100 e.g. `LOGIN`, `REQUEST_APPROVED`), `entity_name` (VarChar 100), `entity_id` (VarChar 100), `details` (VarChar 1000), `timestamp`.
- **Why relationship exists**: Independent audit log table that does not cascade or delete when other records change.

#### 10. `notifications` Table
- **Purpose**: In-app student alerts (e.g. "Your seat request has been approved").
- **Columns**: `id` (PK), `user_id` (FK &rarr; `users.id`), `title` (VarChar 150), `message` (VarChar 1000), `is_read` (Boolean), `created_at`.

---

# 7. Authentication Mechanics (Deep Dive)

QueueSense implements stateless token-based authentication using **BCrypt** and **JSON Web Tokens (JWT)**:

```
[ BROWSER ]                                           [ SPRING BOOT BACKEND ]
    │                                                            │
    │ 1. POST /api/auth/login { email, password }               │
    ├───────────────────────────────────────────────────────────►│
    │                                                            │ 2. Find user by email
    │                                                            │ 3. BCrypt.checkpw(rawPassword, hash)
    │                                                            │ 4. If valid, generate JWT Token
    │ 5. Return JSON { token, email, name, role, studentId }    │    signed with HMAC-SHA256 Secret
    │◄───────────────────────────────────────────────────────────┤
    │                                                            │
    │ 6. Store token in localStorage ('queuesense_token')        │
    │                                                            │
    │ 7. Subsequent Requests:                                    │
    │    GET /api/requests/my                                    │
    │    Headers: { Authorization: "Bearer <token>" }           │
    │───────────────────────────────────────────────────────────►│
    │                                                            │ 8. JwtAuthenticationFilter:
    │                                                            │    - Parse and verify token signature
    │                                                            │    - Extract email and roles
    │                                                            │    - Populate SecurityContextHolder
    │                                                            │ 9. Execute controller method
```

### Password Hashing Details:
- When seed accounts are created in [`DataInitializer.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/config/DataInitializer.java), passwords are encrypted using `passwordEncoder.encode("Student@123")`.
- This produces a standard 60-character BCrypt hash starting with `$2a$10$...`.
- Raw passwords are never stored in the database.

### JWT Structure Details:
A QueueSense JWT consists of 3 base64-encoded segments separated by periods:
1. **Header**: `{"alg": "HS256", "typ": "JWT"}`
2. **Payload**:
   ```json
   {
     "sub": "student@queuesense.demo",
     "roles": "ROLE_STUDENT",
     "iat": 1758500000,
     "exp": 1758586400
   }
   ```
3. **Signature**: Cryptographic HMAC-SHA256 signature generated using `app.jwt.secret`.

---

# 8. Authorization & Role-Based Access Control (RBAC)

QueueSense uses Spring Security to enforce access boundaries across 3 distinct roles:
- `ROLE_STUDENT`
- `ROLE_OPERATOR`
- `ROLE_ADMIN`

### Endpoint Security Matrix:

| URL Endpoint | HTTP Method | Access Level | Permitted Roles | Handled By Controller |
| :--- | :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | **Public** | Anyone | `AuthController` |
| `/api/auth/me` | `GET` | **Authenticated** | All logged-in users | `AuthController` |
| `/api/facilities/**` | `GET` | **Public** | Anyone | `FacilityController` |
| `/api/resources/**` | `GET` | **Public** | Anyone | `ResourceController` |
| `/api/requests` | `POST` | **Student Only** | `ROLE_STUDENT` | `RequestController` |
| `/api/requests/my` | `GET` | **Student Only** | `ROLE_STUDENT` | `RequestController` |
| `/api/requests/{id}` | `GET` | **Authenticated** | All logged-in users *(See Bug H-01)* | `RequestController` |
| `/api/requests/{id}/cancel`| `POST` | **Owner Only** | Student who created request | `RequestController` |
| `/api/requests/{id}/check-out`| `POST` | **Authenticated**| Request owner or Operator | `RequestController` |
| `/api/operator/dashboard` | `GET` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/requests/pending` | `GET` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/requests/approved` | `GET` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/requests/{id}/approve` | `POST` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/requests/{id}/reject` | `POST` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/resources` | `GET` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/operator/resources/{id}/status` | `PATCH` | **Operator Only** | `ROLE_OPERATOR` | `OperatorController` |
| `/api/check-in/verify` | `POST` | **Operator Only** | `ROLE_OPERATOR` | `CheckInController` |
| `/api/history` | `GET` | **Authenticated** | All (Student filtered to self; Operator sees all) | `HistoryController` |

---

# 9. Finite State Machines & Lifecycle Invariants

QueueSense relies on two synchronized Finite State Machines (FSM):

### 1. Usage Request State Machine

```
                      [Student Submits Request]
                                  │
                                  ▼
                            +-----------+
              ┌─────────────┤  PENDING  ├─────────────┐
              │             +-----+-----+             │
     Operator │                   │          Student  │
      Rejects │          Operator │          Cancels  │
              ▼          Approves │                   ▼
       +--------------+           ▼            +-------------+
       |   REJECTED   |     +-----------+      |  CANCELLED  |
       +--------------+  ┌──┤ APPROVED  ├──┐   +-------------+
                         │  +-----+-----+  │          ▲
                Operator │        │        │          │
                Verifies │        │ 10-min │ Student  │
                Check-in │        │ Grace  │ Cancels  │
                         ▼        │ Passes │          │
                   +------------+ ▼        └──────────┘
                   | CHECKED_IN | +-----------+
                   +-----+------+ |  EXPIRED  |
                         │        +-----------+
                 Student │ or Operator
                Checks   │ Out
                         ▼
                   +-----------+
                   | COMPLETED |
                   +-----------+
```

### 2. Resource State Machine

```
              ┌──────────────────────────────────────────────┐
              │                                              │
              ▼ Operator sets                                │ Operator sets
        +-------------+  Available                     +-----┴-------+
        |  AVAILABLE  |◄───────────────────────────────┤ MAINTENANCE |
        +-----+-------+                                +-------------+
              │                                              ▲
              │ Operator Approves                            │ Operator flags
              │ Request                                      │ Maintenance
              ▼                                              │
        +-------------+                                      │
        |  RESERVED   ├──────────────────────────────────────┤
        +-----+-------+  Operator Cancels / Expired          │
              │                                              │
              │ Student Checks In (QR Scan)                  │
              ▼                                              │
        +-------------+                                      │
        |  OCCUPIED   ├──────────────────────────────────────┘
        +-----+-------+
              │
              │ Student / Operator Checks Out
              ▼
        (Returns to AVAILABLE)
```

### State Synchronization Invariants:
1. **When Request is `PENDING`**: Resource status remains `AVAILABLE`.
2. **When Request is `APPROVED`**: Resource status becomes `RESERVED` and a 10-minute hold countdown begins.
3. **When Request is `CHECKED_IN`**: Resource status becomes `OCCUPIED`.
4. **When Request is `COMPLETED`**: Resource status resets to `AVAILABLE`.
5. **When Request is `CANCELLED` or `EXPIRED`**: Resource status resets to `AVAILABLE`.

---

# 10. Anatomy of a Single Action: "Approve Request" Step-by-Step Trace

Here is exactly what happens under the hood when an operator clicks the green **"Approve"** button on request `#14`:

```
1. [BROWSER / REACT DOM]
   - Operator clicks <button onClick={() => handleApprove(14)}>Approve</button> inside OperatorRequests.jsx.
   - React sets actionId state to 14 (showing a loading spinner on that specific card).

2. [NETWORK / AXIOS]
   - Axios client (api.js) reads token from localStorage.
   - Dispatches HTTP request:
     POST /api/operator/requests/14/approve
     Host: localhost:8080
     Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

3. [VITE PROXY & SPRING SECURITY]
   - Vite dev server forwards request to Spring Boot on port 8080.
   - JwtAuthenticationFilter intercepts request, validates HMAC-SHA256 signature, extracts operator@queuesense.demo and role ROLE_OPERATOR.
   - SecurityContextHolder is populated with UsernamePasswordAuthenticationToken.
   - SecurityConfig verifies that /api/operator/** matches hasRole('OPERATOR').

4. [REST CONTROLLER LAYER]
   - OperatorController.approveRequest(14, authentication) receives call.
   - Extracts operatorEmail = authentication.getName().
   - Calls usageRequestService.approveRequest(14, operatorEmail).

5. [SERVICE & TRANSACTION BOUNDARY]
   - UsageRequestService.approveRequest() starts an ACID transaction (@Transactional).
   - Queries: SELECT * FROM usage_requests WHERE id = 14;
   - Verifies request.getStatus() == RequestStatus.PENDING.
   - Queries linked resource: SELECT * FROM resources WHERE id = resource_id;
   - Verifies resource.getStatus() == ResourceStatus.AVAILABLE.
   - Mutates in-memory Java objects:
       resource.setStatus(ResourceStatus.RESERVED);
       request.setStatus(RequestStatus.APPROVED);
       request.setApprovedAt(LocalDateTime.now());
       request.setExpiresAt(LocalDateTime.now().plusMinutes(10));
   - Calls resourceRepository.save(resource) and usageRequestRepository.save(request).
   - Calls auditLogService.log("operator@queuesense.demo", "ROLE_OPERATOR", "REQUEST_APPROVED", ...).

6. [HIBERNATE JPA & POSTGRESQL]
   - Hibernate flushes persistence context and emits SQL:
       UPDATE resources SET status = 'RESERVED' WHERE id = 2;
       UPDATE usage_requests SET status = 'APPROVED', approved_at = '2026-09-22 13:00:00', expires_at = '2026-09-22 13:10:00' WHERE id = 14;
       INSERT INTO audit_logs (actor_email, actor_role, action, entity_name, entity_id, details, timestamp)
       VALUES ('operator@queuesense.demo', 'ROLE_OPERATOR', 'REQUEST_APPROVED', 'UsageRequest', '14', 'Approved request QS-2026-50608 for Seat 02', NOW());
   - PostgreSQL commits transaction and writes data to disk.

7. [HTTP RESPONSE & FRONTEND UI UPDATE]
   - Controller returns 200 OK with UsageRequestResponseDto.
   - OperatorRequests.jsx receives response.
   - Calls fetchRequests(true) to refresh the lists.
   - Request #14 disappears from the "Pending Requests (0)" tab and appears in the "Approved Passes (1)" tab.
```

---

# 11. Audit Findings: Known Bugs, Security Vulnerabilities & Edge Cases

The following issues were discovered during the read-only inspection:

### 🔴 High Severity Issues

#### Finding H-01: Broken Object-Level Authorization (BOLA / IDOR) on Request Details
- **Location**: [`RequestController.java` (Lines 40–48)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/controller/RequestController.java#L40-L48)
- **Vulnerability**: `GET /api/requests/{id}` and `GET /api/requests/code/{code}` return request details to any authenticated caller without verifying if they own the request or supervise the facility.
- **Impact**: Any student can increment the request ID (`/api/requests/1`, `/api/requests/2`) to read other students' names, roll numbers, allocated seats, and active QR request codes.
- **Fix**: Check `request.getStudent().getEmail().equals(currentUserEmail)` or verify caller has `ROLE_OPERATOR`.

#### Finding H-02: Missing Facility-Scoped Authorization for Operators
- **Location**: [`UsageRequestService.java` (Lines 107–150)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L107-L150)
- **Vulnerability**: `getPendingRequests()` queries pending requests globally across all university facilities. An operator assigned to the E-Library can see, approve, or reject reservations for the Sports Complex or Reading Room.
- **Impact**: Cross-departmental operational interference.
- **Fix**: Filter queries by `operator.assignedFacilityId` and reject approval attempts for unassigned facilities.

---

### 🟡 Medium Severity Issues

#### Finding M-01: Predictable 5-Digit QR Request Code
- **Location**: [`UsageRequestService.java` (Line 207)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/UsageRequestService.java#L207)
- **Vulnerability**: Codes are generated as `QS-2026-` + random 5-digit number (`10000` to `99999`), giving only 90,000 possibilities with no cryptographic signature or hash.
- **Impact**: Acceptable for a supervised pilot with human desk attendants, but vulnerable to guessing if unmanned kiosks are ever used.
- **Fix**: Use UUIDv4 tokens or sign the payload with HMAC-SHA256.

#### Finding M-02: No Rate Limiting on Login & Verification Endpoints
- **Location**: [`SecurityConfig.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/config/SecurityConfig.java)
- **Vulnerability**: `/api/auth/login` and `/api/check-in/verify` have no request throttling.
- **Impact**: Vulnerable to brute-force credential stuffing or request code enumeration.
- **Fix**: Implement a Spring Security rate-limiting filter (e.g., Bucket4j).

---

### 🟢 Low Severity & Cosmetic Issues

#### Finding L-01: Lazy Reservation Hold Expiration (No Background Worker)
- **Location**: [`CheckInService.java` (Lines 58–67)](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/service/CheckInService.java#L58-L67)
- **Behavior**: An expired reservation is only marked `EXPIRED` when an operator attempts to verify that specific request code or when a user cancels.
- **Impact**: If a student is a no-show, their seat remains locked in `RESERVED` status indefinitely.
- **Fix**: Add a 60-second `@Scheduled` background worker in Spring Boot to sweep and auto-release expired holds.

#### Finding C-01: Print CSS Header Artifacts on Digital Confirmation Pass
- **Location**: [`App.css`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/frontend/src/App.css) missing `@media print` rules.
- **Impact**: When a student prints their confirmation pass, the website navbar, footer, and buttons appear on the paper sheet.
- **Fix**: Add `@media print { .navbar-root, .app-footer, .btn { display: none !important; } }`.

---

# 12. Feature Matrix: Implemented vs. Missing

| Feature / Capability Area | Current Implementation Status | Notes |
| :--- | :---: | :--- |
| **User Authentication (JWT + BCrypt)** | ✅ Fully Implemented | Login, role extraction, token refresh on load. |
| **Facility & Resource Browsing** | ✅ Fully Implemented | Live seating grid with status badges. |
| **Reservation Request Submission** | ✅ Fully Implemented | Arrival slot, duration, group size, duplicate request block. |
| **Operator Request Approval Queue** | ✅ Fully Implemented | Real-time pending/approved tabs, approve & reject actions. |
| **Digital Confirmation & QR Generation**| ✅ Fully Implemented | Vector SVG QR code rendering with hold countdown timer. |
| **Operator QR Code Camera Scanner** | ✅ Fully Implemented | Real-time camera barcode scanning via `html5-qrcode`. |
| **Physical Check-In Verification** | ✅ Fully Implemented | Transitions resource to `OCCUPIED` & starts `UsageSession`. |
| **Session Checkout** | ✅ Fully Implemented | Releases resource to `AVAILABLE` & logs session duration. |
| **Usage History Tracking** | ✅ Fully Implemented | Student view (personal history) & Operator view (all sessions). |
| **Audit Logging Ledger** | ✅ Fully Implemented | Immutable database ledger logging all 10 system actions. |
| **Automated Background Expiration** | ❌ Missing | Needs Spring `@Scheduled` cron job to release no-show seats. |
| **Facility-Scoped Operator Isolation** | ❌ Missing | Operators currently supervise all university facilities globally. |
| **BOLA Object Ownership Checks** | ❌ Missing | Students can inspect other students' requests via ID enumeration. |
| **University LDAP / SSO Integration** | ❌ Missing | Currently uses local PostgreSQL credentials. |
| **Student Daily Quota & Strike Penalties** | ❌ Missing | Needs business rules from college librarians/staff. |

---

# 13. Git, Secrets & Repository Readiness Assessment

### Current Git Status:
- **Git is NOT initialized** in this directory (there is no `.git` folder).
- **No remote GitHub repository exists**.

### ⚠️ Critical Pre-GitHub Checklist (Do NOT push without fixing these):
1. **Hardcoded JWT Secret in `application.yml`**:
   - `app.jwt.secret: QueueSenseSuperSecretKeyForJWTSigningThatIsLongEnough2026!` is currently hardcoded in source control.
   - Must be moved to an environment variable before public repository creation.
2. **PostgreSQL Database Password in `application.yml`**:
   - Currently set to blank (`password: ""`).
3. **Local Portable PostgreSQL & Maven Binaries**:
   - The workspace contains `pgsql/` (local database engine) and `tools/` (local Maven).
   - When Git is initialized, `.gitignore` **MUST** exclude `pgsql/`, `tools/`, `backend/target/`, and `frontend/node_modules/` to avoid committing hundreds of megabytes of binary files.

---

# 14. Actionable Next Steps & Student Learning Roadmap

### Priority Phase 1: Security & Boundary Fixes (Immediate)
1. **Fix BOLA Vulnerability**: Add ownership check in `RequestController.java` for `GET /api/requests/{id}`.
2. **Enforce Operator Facility Boundaries**: Filter pending requests in `UsageRequestService.java` by operator's assigned facility.
3. **Externalize Secrets**: Replace hardcoded JWT secret with `${JWT_SECRET}` in `application.yml`.

### Priority Phase 2: Booking Engine Automation (Short-Term)
4. **Automated Expiry Worker**: Add `@EnableScheduling` and a 60-second `@Scheduled` sweeper in Spring Boot to automatically release expired holds.
5. **Print Stylesheet**: Add `@media print` CSS rules so digital confirmation passes print cleanly without navbars.

### Priority Phase 3: College Stakeholder Alignment (Before Real Deployment)
6. **Consult College Staff** using [`docs/COLLEGE_REQUIREMENTS_QUESTIONS.md`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/docs/COLLEGE_REQUIREMENTS_QUESTIONS.md) to decide:
   - What should the hold grace period be? (e.g., 10 mins vs. 15 mins).
   - What are the daily seat booking quotas per student?
   - How should repeat no-shows be penalized (e.g., 3-day booking ban)?

---

### 🎓 What You Personally Need to Learn to Maintain QueueSense:
As a B.Tech CSE student managing this project, here is the exact recommended learning path:
1. **React State & Props**: Learn how `useState`, `useEffect`, and `useContext` pass data and re-render components.
2. **Axios & REST APIs**: Understand how frontend forms translate into HTTP `POST`/`GET` requests with JSON payloads and Bearer tokens.
3. **Spring Boot MVC Pattern**: Understand how `Controller` &rarr; `Service` &rarr; `Repository` separates responsibilities.
4. **Spring Security & JWT**: Understand how `JwtAuthenticationFilter` intercepts HTTP headers and populates user identities.
5. **JPA / Hibernate & SQL**: Learn how Java entity annotations (`@ManyToOne`, `@OneToOne`) map to SQL Foreign Keys and `JOIN` statements.
6. **Relational Database Design & ACID**: Understand how transactions prevent two students from booking the same seat simultaneously.

---
*Document compiled and verified by Read-Only Full-Stack Audit Agent. No code or database changes were made during this audit.*
