# QueueSense — System Architecture & Engineering Breakdown

## 1. Executive Summary & Architecture Overview

QueueSense is a full-stack campus facility availability, reservation, and real-time resource allocation management platform designed for university environments (libraries, digital labs, study rooms, sports facilities).

```
+-----------------------------------------------------------------------+
|                           CLIENT BROWSER                              |
|   +---------------------------------------------------------------+   |
|   |                  React 19 + Vite Frontend                     |   |
|   |   - React Router DOM v7 (Route Protection & Layouts)          |   |
|   |   - AuthContext (JWT Token & Session State)                   |   |
|   |   - QR Generation (qrcode.react) & Camera Scanning (html5-qr) |   |
|   +---------------------------------------------------------------+   |
+-----------------------------------┬-----------------------------------+
                                    │ HTTP REST + JSON (Bearer JWT)
                                    │ Proxied via Vite (/api -> :8080)
+-----------------------------------▼-----------------------------------+
|                        SPRING BOOT 3.3.5 BACKEND                      |
|                                                                       |
|   +---------------------------------------------------------------+   |
|   |                     Spring Security & JWT                     |   |
|   |   - JwtAuthenticationFilter (Header Extraction & Context Set) |   |
|   |   - BCryptPasswordEncoder (10 rounds password hashing)        |   |
|   |   - CustomUserDetailsService (DB-backed credential lookup)    |   |
|   +-------------------------------+-------------------------------+   |
|                                   │                                   |
|   +-------------------------------▼-------------------------------+   |
|   |                      REST Controller Layer                    |   |
|   |   - AuthController, FacilityController, ResourceController    |   |
|   |   - RequestController, OperatorController, CheckInController  |   |
|   |   - HistoryController                                         |   |
|   +-------------------------------+-------------------------------+   |
|                                   │                                   |
|   +-------------------------------▼-------------------------------+   |
|   |                       Service Layer (Logic)                   |   |
|   |   - AuthService, UsageRequestService, CheckInService          |   |
|   |   - CheckOutService, ResourceService, DashboardService        |   |
|   |   - AuditLogService                                           |   |
|   +-------------------------------+-------------------------------+   |
|                                   │                                   |
|   +-------------------------------▼-------------------------------+   |
|   |                   Data Access Layer (Spring Data JPA)         |   |
|   |   - Hibernate ORM + HikariCP Connection Pooling               |   |
|   |   - 10 Type-safe Repositories (UserRepository, etc.)          |   |
|   +-------------------------------+-------------------------------+   |
+-----------------------------------┬-----------------------------------+
                                    │ JDBC Connection (Port 5432)
+-----------------------------------▼-----------------------------------+
|                        POSTGRESQL 16.3 DATABASE                       |
|   - Tables: users, student_profiles, operator_profiles, facilities,   |
|     resources, usage_requests, check_ins, usage_sessions,             |
|     audit_logs, notifications                                         |
+-----------------------------------------------------------------------+
```

---

## 2. Beginner-Friendly Layer-by-Layer Explanation

### Layer 1: Client Browser & React / Vite Frontend
- **What it does**: This is what the student and operator see and interact with. It renders the UI buttons, forms, tables, and QR codes.
- **Key responsibility**: Captures user input (e.g., clicking "Request Seat 01"), sends HTTP requests to the backend with the user's JWT token, and updates the screen when the server responds.

### Layer 2: API & Network Gateway (Vite Proxy + Axios)
- **What it does**: Centralized communication bridge (`api.js`).
- **Key responsibility**: Attaches the `Authorization: Bearer <token>` header to outgoing requests and intercepts 401 Unauthorized errors to automatically redirect expired sessions back to `/login`.

### Layer 3: Spring Security Filter Chain
- **What it does**: The campus gatekeeper.
- **Key responsibility**: Before any request reaches a controller, `JwtAuthenticationFilter` reads the token, verifies the cryptographic signature, extracts the user's email and role (`ROLE_STUDENT`, `ROLE_OPERATOR`), and populates `SecurityContextHolder`.

### Layer 4: REST Controllers
- **What it does**: The front desk of the backend.
- **Key responsibility**: Maps URLs (e.g., `POST /api/requests`) to Java methods, validates incoming JSON payloads (`@Valid`), and calls the appropriate service.

### Layer 5: Service Layer (Business Logic)
- **What it does**: The brain of the application.
- **Key responsibility**: Enforces rules (e.g., "A student cannot book a seat if they already have an active reservation", "A resource can only transition to OCCUPIED after an operator scans a valid QR").

### Layer 6: Data Access Layer (Spring Data JPA & Hibernate)
- **What it does**: The translator between Java objects and SQL tables.
- **Key responsibility**: Generates SQL queries (`SELECT`, `INSERT`, `UPDATE`) automatically from Java method names like `findByRequestCode()` and manages transactions (`@Transactional`).

### Layer 7: PostgreSQL Database
- **What it does**: Persistent storage on disk.
- **Key responsibility**: Guarantees ACID transactions, unique constraints (e.g., preventing duplicate emails or request codes), and foreign key integrity.

---

## 3. Detailed Component & Directory Structure

```
QueueSense-FullStack/
├── AGENT_PROMPT.md                  # Project engineering guidelines
├── PRD.md                           # Product requirements specification
├── start-db.ps1                     # PostgreSQL daemon launcher
├── stop-db.ps1                      # PostgreSQL shutdown script
│
├── backend/                         # Spring Boot 3.3.5 Application
│   ├── pom.xml                      # Maven dependencies and build plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/queuesense/
│       │   │   ├── QueueSenseApplication.java  # Main entry point
│       │   │   ├── config/
│       │   │   │   ├── DataInitializer.java     # CommandLineRunner seeding demo data
│       │   │   │   └── SecurityConfig.java      # Spring Security & CORS configuration
│       │   │   ├── controller/                  # REST Controllers
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── CheckInController.java
│       │   │   │   ├── FacilityController.java
│       │   │   │   ├── HistoryController.java
│       │   │   │   ├── OperatorController.java
│       │   │   │   ├── RequestController.java
│       │   │   │   └── ResourceController.java
│       │   │   ├── dto/                         # Data Transfer Objects
│       │   │   │   ├── AuthResponse.java
│       │   │   │   ├── CreateUsageRequestDto.java
│       │   │   │   ├── DashboardStatsDto.java
│       │   │   │   ├── LoginRequest.java
│       │   │   │   ├── RejectRequestDto.java
│       │   │   │   ├── UsageRequestResponseDto.java
│       │   │   │   └── VerifyCheckInDto.java
│       │   │   ├── entity/                      # JPA Entities (Database Tables)
│       │   │   │   ├── AuditLog.java
│       │   │   │   ├── CheckIn.java
│       │   │   │   ├── Facility.java
│       │   │   │   ├── Notification.java
│       │   │   │   ├── OperatorProfile.java
│       │   │   │   ├── Resource.java
│       │   │   │   ├── StudentProfile.java
│       │   │   │   ├── UsageRequest.java
│       │   │   │   ├── UsageSession.java
│       │   │   │   └── User.java
│       │   │   ├── enums/                       # Strongly typed domain constants
│       │   │   │   ├── CheckInMethod.java       # QR_SCAN, MANUAL_CODE
│       │   │   │   ├── RequestStatus.java       # PENDING, APPROVED, REJECTED, ...
│       │   │   │   ├── ResourceStatus.java      # AVAILABLE, RESERVED, OCCUPIED, ...
│       │   │   │   └── Role.java                # ROLE_STUDENT, ROLE_OPERATOR, ROLE_ADMIN
│       │   │   ├── exception/                   # Global exception handling
│       │   │   │   ├── DuplicateRequestException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── InvalidStateTransitionException.java
│       │   │   │   ├── ResourceNotFoundException.java
│       │   │   │   └── UnauthorizedException.java
│       │   │   ├── repository/                  # Spring Data JPA Repositories
│       │   │   │   ├── AuditLogRepository.java
│       │   │   │   ├── CheckInRepository.java
│       │   │   │   ├── FacilityRepository.java
│       │   │   │   ├── NotificationRepository.java
│       │   │   │   ├── OperatorProfileRepository.java
│       │   │   │   ├── ResourceRepository.java
│       │   │   │   ├── StudentProfileRepository.java
│       │   │   │   ├── UsageRequestRepository.java
│       │   │   │   ├── UsageSessionRepository.java
│       │   │   │   └── UserRepository.java
│       │   │   ├── security/                    # Security components
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   └── JwtTokenProvider.java
│       │   │   └── service/                     # Business Logic Services
│       │   │       ├── AuditLogService.java
│       │   │       ├── AuthService.java
│       │   │       ├── CheckInService.java
│       │   │       ├── CheckOutService.java
│       │   │       ├── DashboardService.java
│       │   │       ├── FacilityService.java
│       │   │       ├── ResourceService.java
│       │   │       └── UsageRequestService.java
│       │   └── resources/
│       │       └── application.yml              # Database, JWT, & Grace period properties
│       └── test/java/com/queuesense/
│           └── QueueSenseWorkflowTest.java      # 5 end-to-end JUnit workflow tests
│
├── frontend/                        # React 19 + Vite Web Application
│   ├── package.json
│   ├── vite.config.js               # Dev server & reverse proxy (/api -> localhost:8080)
│   ├── index.html                   # HTML root & SEO tags
│   └── src/
│       ├── main.jsx                 # React root mount
│       ├── App.jsx                  # Main router hierarchy
│       ├── App.css                  # UI component & layout styling
│       ├── index.css                # University design tokens & typography
│       ├── services/
│       │   └── api.js               # Axios instance with JWT interceptors
│       ├── context/
│       │   └── AuthContext.jsx       # Auth state, login/logout, user info
│       ├── components/
│       │   ├── common/              # StatusBadge, Modal, ErrorMessage, EmptyState, ...
│       │   ├── layout/              # AppLayout, Navbar (with quick demo switch)
│       │   └── booking/             # BookingModal
│       └── pages/
│           ├── auth/                # LoginPage
│           ├── student/             # StudentDashboard, LibrariesList, FacilityDetails,
│           │                        # MyRequests, DigitalConfirmation, StudentHistory
│           └── operator/            # OperatorDashboard, OperatorRequests, OperatorResources,
│                                    # OperatorCheckIn, OperatorHistory
│
├── pgsql/                           # Portable PostgreSQL 16.3 Binaries & Data
└── tools/                           # Portable Apache Maven 3.9.9
```

---

## 4. Configuration & Environment Variables

| Property | Default Value | Location | Purpose |
| :--- | :--- | :--- | :--- |
| `server.port` | `8080` | `application.yml` | Spring Boot HTTP listening port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/queuesense` | `application.yml` | JDBC connection string |
| `spring.datasource.username` | `postgres` | `application.yml` | Database user |
| `spring.datasource.password` | `""` (empty) | `application.yml` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | `application.yml` | Automatically creates/updates DB tables from entities |
| `app.jwt.secret` | `QueueSenseSuperSecretKey...` | `application.yml` | HMAC-SHA secret for signing JWTs |
| `app.jwt.expiration-milliseconds` | `86400000` (24h) | `application.yml` | JWT validity duration |
| `app.booking.grace-period-minutes`| `10` | `application.yml` | No-show expiration window |

---

## 5. Seed Data Mechanism

When the backend starts, [`DataInitializer.java`](file:///c:/My_career_Projects_Ideas_lab/QueueSense-FullStack/backend/src/main/java/com/queuesense/config/DataInitializer.java) runs automatically (`CommandLineRunner`). If the `users` table is empty (`userRepository.count() == 0`), it populates:
1. **Facilities**: E-Library (`LIBRARY`), Reading Room (`LIBRARY`), Sports Complex (`SPORTS`).
2. **Users**:
   - Student: `student@queuesense.demo` / `Student@123` (ID: `STU-2026-001`, CSE 3rd Year)
   - Operator: `operator@queuesense.demo` / `Operator@123` (Assigned: E-Library)
   - Admin: `admin@queuesense.demo` / `Admin@123`
3. **Resources**: 9 Seats/PCs in E-Library, 3 Desks in Reading Room, 3 Athletic Courts in Sports Complex with mixed statuses (`AVAILABLE`, `OCCUPIED`, `MAINTENANCE`).
