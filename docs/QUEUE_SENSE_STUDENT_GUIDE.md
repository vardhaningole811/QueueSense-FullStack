# QueueSense — The BTech CSE Student's Guide to Full-Stack Engineering

Welcome to the comprehensive engineering guide for QueueSense. This guide explains how every piece of this full-stack application works together using real code from this project.

---

## 1. What React Is Doing

In traditional web development, clicking a button requested a brand new HTML page from the server, causing a full page reload and white screen flash.

**React is a Component-Based Single Page Application (SPA) Library.**
- In QueueSense, the browser loads `index.html` **once**.
- When you click between **Libraries** (`/student/libraries`) and **My Requests** (`/student/requests`), **the browser never reloads**. Instead, React's router dynamically swaps out JavaScript components on the DOM.
- **State (`useState`)**: When `loading` changes from `true` to `false`, React automatically re-renders the screen to show the table of seats.
- **Side Effects (`useEffect`)**: When `FacilityDetails.jsx` loads, `useEffect` triggers an asynchronous network call (`api.get('/resources/facility/1')`) to fetch real data from the backend.

```jsx
// Real code from FacilityDetails.jsx:
useEffect(() => {
  fetchFacilityData(); // Runs once when the component appears on screen!
}, [fetchFacilityData]);
```

---

## 2. What Spring Boot Is Doing

**Spring Boot is an Enterprise Java Web Framework.**
- It acts as the backend server listening for HTTP traffic on port `8080`.
- **Dependency Injection (`@Autowired` / Constructor Injection)**: You don't write `new UsageRequestService()`. Spring automatically instantiates services, repositories, and controllers and connects them together.
- **Inversion of Control (IoC)**: Spring manages the lifecycle of your code, handling multithreading, connection pooling, and request routing behind the scenes.

---

## 3. What PostgreSQL Is Doing

**PostgreSQL is an ACID-compliant Relational Database Management System (RDBMS).**
- While Java objects in memory disappear when the server restarts, PostgreSQL writes all records to disk (`pgsql/data/`).
- **ACID Guarantees**:
  - **Atomicity**: When an operator approves a request, both the request status change (`APPROVED`) and the resource status change (`RESERVED`) succeed together, or both fail. No partial corrupt state.
  - **Consistency**: Unique constraints prevent two students from having the same email address.
  - **Isolation**: Multiple students booking seats at the exact same millisecond are handled safely.
  - **Durability**: Once committed, data survives power loss.

---

## 4. What An API & REST Endpoints Are

An **API (Application Programming Interface)** is a structured contract that allows two programs (the React frontend and the Spring Boot backend) to talk to each other.

**REST (Representational State Transfer)** uses standard HTTP verbs to perform actions:
- `GET /api/facilities` &rarr; Retrieve list of facilities (Read-only, idempotent).
- `POST /api/requests` &rarr; Create a new usage request (Creates data).
- `PATCH /api/operator/resources/1/status` &rarr; Modify a specific field (status) of a resource.
- `DELETE` (or Cancel) &rarr; Invalidate a resource.

---

## 5. What JPA & Hibernate Are Doing

In pure SQL, you have to write:
```sql
SELECT * FROM usage_requests WHERE student_id = 4 ORDER BY created_at DESC;
```
and manually extract columns from a `ResultSet`.

**Hibernate is an Object-Relational Mapper (ORM), and Spring Data JPA is its abstraction layer.**
- You define a Java class (`UsageRequest.java`) with annotations like `@Entity`, `@Table`, `@ManyToOne`.
- In your interface (`UsageRequestRepository.java`), you declare:
```java
List<UsageRequest> findByStudentOrderByCreatedAtDesc(User student);
```
- Spring Data JPA reads that method name and **generates the SQL query automatically at runtime**!

---

## 6. How Authentication & Security Work

```
1. Student enters email & password -> POST /api/auth/login
2. Spring matches password against BCrypt hash in DB:
   BCrypt.checkpw("Student@123", "$2a$10$...") -> TRUE
3. JwtTokenProvider signs a JSON Web Token (JWT) with the secret key:
   Header: {"alg": "HS256"}
   Payload: {"sub": "student@queuesense.demo", "roles": "ROLE_STUDENT", "exp": 1726978800}
   Signature: HMACSHA256(Header + Payload, SecretKey)
4. Frontend saves token in browser localStorage.
5. Every subsequent request sends:
   Authorization: Bearer eyJhbGciOi...
6. JwtAuthenticationFilter validates the cryptographic signature.
```

---

## 7. How Authorization Works

- **Authentication** answers: *"Who are you?"* (e.g. Aarav Sharma).
- **Authorization** answers: *"What are you allowed to do?"* (e.g. Can Aarav access the operator approval button?).
- In `SecurityConfig.java`:
```java
.requestMatchers("/api/operator/**").hasRole("OPERATOR")
.requestMatchers("/api/check-in/**").hasRole("OPERATOR")
```
If a student token calls `GET /api/operator/requests/pending`, Spring Security immediately halts the request and returns `403 Forbidden`.

---

## 8. Complete Journey of a Booking Request

```
Step 1: Student clicks "Request Seat 01" in BookingModal.jsx.
Step 2: Axios sends HTTP POST /api/requests with Bearer JWT.
Step 3: JwtAuthenticationFilter verifies JWT and sets SecurityContext.
Step 4: RequestController receives CreateUsageRequestDto and calls UsageRequestService.
Step 5: Service checks if Seat 01 is AVAILABLE and ensures student has no duplicate active request.
Step 6: Service creates UsageRequest entity, assigns unique code "QS-2026-50608", sets status PENDING.
Step 7: JPA inserts row into usage_requests and audit_logs tables.
Step 8: Controller returns 201 Created JSON to React.
Step 9: React navigates to MyRequests.jsx, rendering the PENDING badge.
```

---

## 9. How QR Verification Works

1. When a request is `APPROVED`, the student opens `DigitalConfirmation.jsx`.
2. The `<QRCodeSVG value={request.requestCode} />` component encodes the text string `"QS-2026-50608"` into a matrix of black and white 2D squares.
3. At the facility desk, the operator uses `OperatorCheckIn.jsx` which activates their webcam via `Html5QrcodeScanner`.
4. The scanner reads the QR pattern, extracts `"QS-2026-50608"`, and sends `POST /api/check-in/verify`.
5. `CheckInService` finds the request, verifies `status == APPROVED`, verifies `now < expiresAt`, updates status to `CHECKED_IN`, changes seat status to `OCCUPIED`, and starts an active `UsageSession`.

---

## 10. How to Debug Bugs Like a Senior Engineer

### Scenario 1: A button click does nothing or shows an error
- **Step 1**: Press `F12` in Chrome &rarr; Open the **Console** tab. Look for red JavaScript errors.
- **Step 2**: Open the **Network** tab &rarr; Click the button again &rarr; Look for the failed red HTTP request (e.g. `400`, `401`, `403`, `500`).
- **Step 3**: Click the failed network request &rarr; Look at the **Payload** (what React sent) and **Response** (what Spring Boot answered).

### Scenario 2: Backend error in terminal
- **Look for the root cause**: Scroll down past Spring's long stack trace to the first line that says `Caused by:`.
- Examples:
  - `JDBCConnectionException: Connection refused` &rarr; PostgreSQL is not running (`start-db.ps1`).
  - `DuplicateRequestException` &rarr; Student already has an active reservation.
  - `AccessDeniedException` &rarr; Student tried calling an operator endpoint.

### Scenario 3: Database inspection
- Run `psql` or PowerShell script to query the database directly:
```powershell
& 'c:\My_career_Projects_Ideas_lab\QueueSense-FullStack\pgsql\bin\psql.exe' -U postgres -d queuesense -c "SELECT id, request_code, status, resource_id FROM usage_requests ORDER BY id DESC LIMIT 5;"
```
This lets you check the true database state independent of what the frontend UI is displaying!
