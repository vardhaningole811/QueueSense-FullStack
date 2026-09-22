# QueueSense — Backend REST API Specification & Endpoint Audit

## 1. Summary of API Endpoints

The QueueSense backend exposes **18 REST API endpoints** across 7 controllers.

### Access Control Matrix Summary
- **Public (No Auth required)**: 5 endpoints
- **Authenticated (Any Valid Role)**: 4 endpoints
- **Student-Only Workflow**: 2 endpoints
- **Operator-Only Workflow**: 7 endpoints

---

## 2. Endpoint-by-Endpoint Audit

### 1. Authentication Controller (`/api/auth`)

#### `POST /api/auth/login`
- **Access**: Public
- **Role Required**: None
- **Input (JSON Body)**:
  ```json
  {
    "email": "student@queuesense.demo",
    "password": "Student@123"
  }
  ```
- **Output (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "id": 1,
    "email": "student@queuesense.demo",
    "name": "Aarav Sharma",
    "role": "ROLE_STUDENT",
    "studentId": "STU-2026-001",
    "assignedFacilityId": null,
    "assignedFacilityName": null
  }
  ```
- **Database Effect**: Inserts record into `audit_logs` (`action = "LOGIN"`).
- **Error Cases**:
  - `401 Unauthorized` (Bad credentials).
  - `400 Bad Request` (Missing email or password).

#### `GET /api/auth/me`
- **Access**: Authenticated
- **Role Required**: Any (`ROLE_STUDENT`, `ROLE_OPERATOR`, `ROLE_ADMIN`)
- **Input**: `Authorization: Bearer <token>` header
- **Output (200 OK)**: User profile matching current JWT subject.
- **Database Effect**: None (Read-only).
- **Error Cases**: `401 Unauthorized` (Missing or invalid token).

---

### 2. Facilities Controller (`/api/facilities`)

#### `GET /api/facilities`
- **Access**: Public
- **Role Required**: None
- **Input**: None
- **Output (200 OK)**: Array of all active facilities:
  ```json
  [
    {
      "id": 1,
      "name": "E-Library",
      "type": "LIBRARY",
      "description": "Central digital library with high-speed workstations and quiet study pods",
      "active": true,
      "createdAt": "2026-09-20T03:17:16"
    }
  ]
  ```
- **Database Effect**: `SELECT * FROM facilities WHERE active = true`.
- **Error Cases**: None.

#### `GET /api/facilities/{id}`
- **Access**: Public
- **Role Required**: None
- **Input**: Path variable `id` (e.g. `1`)
- **Output (200 OK)**: Facility object.
- **Database Effect**: `SELECT * FROM facilities WHERE id = ?`.
- **Error Cases**: `404 Not Found` if ID does not exist.

---

### 3. Resources Controller (`/api/resources`)

#### `GET /api/resources`
- **Access**: Public
- **Role Required**: None
- **Input**: None
- **Output (200 OK)**: Array of all resources across all facilities.
- **Database Effect**: `SELECT * FROM resources`.

#### `GET /api/resources/{id}`
- **Access**: Public
- **Role Required**: None
- **Input**: Path variable `id`
- **Output (200 OK)**: Resource entity with current status (`AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`).
- **Database Effect**: Read-only.
- **Error Cases**: `404 Not Found`.

#### `GET /api/resources/facility/{facilityId}`
- **Access**: Public
- **Role Required**: None
- **Input**: Path variable `facilityId` (e.g. `1`)
- **Output (200 OK)**: Array of active resources in specified facility.
- **Database Effect**: `SELECT * FROM resources WHERE facility_id = ? AND active = true`.
- **Error Cases**: `404 Not Found` if facility does not exist.

---

### 4. Usage Request Controller (`/api/requests`)

#### `POST /api/requests`
- **Access**: Authenticated
- **Role Required**: Student (`ROLE_STUDENT`)
- **Input (JSON Body)**:
  ```json
  {
    "resourceId": 2,
    "expectedArrival": "Immediate (Next 10 mins)",
    "expectedDuration": "1 hour",
    "groupSize": 1
  }
  ```
- **Output (201 Created)**:
  ```json
  {
    "id": 14,
    "requestCode": "QS-2026-50608",
    "studentName": "Aarav Sharma",
    "studentId": "STU-2026-001",
    "facilityId": 1,
    "facilityName": "E-Library",
    "resourceId": 2,
    "resourceName": "Seat 02",
    "expectedArrival": "Immediate (Next 10 mins)",
    "expectedDuration": "1 hour",
    "groupSize": 1,
    "status": "PENDING",
    "createdAt": "2026-09-22T01:23:22"
  }
  ```
- **Database Effect**:
  - `INSERT INTO usage_requests (..., status='PENDING')`
  - `INSERT INTO audit_logs (action='REQUEST_CREATED')`
- **Error Cases**:
  - `400 Bad Request` / `InvalidStateTransitionException`: Resource is not `AVAILABLE`.
  - `400 Bad Request` / `DuplicateRequestException`: Student already has an active or pending request (`existsByStudentAndStatusIn`).
  - `404 Not Found`: Resource ID not found.

#### `GET /api/requests/my`
- **Access**: Authenticated
- **Role Required**: Student
- **Input**: `Authorization: Bearer <token>`
- **Output (200 OK)**: Array of `UsageRequestResponseDto` belonging to calling student, ordered by `createdAt DESC`.
- **Database Effect**: Read-only.

#### `GET /api/requests/{id}`
- **Access**: Authenticated
- **Role Required**: Any authenticated user
- **Input**: Path variable `id`
- **Output (200 OK)**: Usage request details.
- **Database Effect**: Read-only.
- **Error Cases**: `404 Not Found`.

#### `GET /api/requests/code/{code}`
- **Access**: Authenticated
- **Role Required**: Any authenticated user
- **Input**: Path variable `code` (e.g. `QS-2026-50608`)
- **Output (200 OK)**: Usage request details.
- **Database Effect**: Read-only.

#### `POST /api/requests/{id}/cancel`
- **Access**: Authenticated
- **Role Required**: Request owner
- **Input**: Path variable `id`
- **Output (200 OK)**: Updated request with status `CANCELLED`.
- **Database Effect**:
  - `UPDATE usage_requests SET status = 'CANCELLED' WHERE id = ?`
  - If previously `APPROVED`, `UPDATE resources SET status = 'AVAILABLE'`
  - `INSERT INTO audit_logs (action='REQUEST_CANCELLED')`
- **Error Cases**:
  - `400 Bad Request`: User does not own request.
  - `400 Bad Request`: Request status is not `PENDING` or `APPROVED`.

#### `POST /api/requests/{id}/check-out`
- **Access**: Authenticated
- **Role Required**: Student (owner) or Operator
- **Input**: Path variable `id`
- **Output (200 OK)**: Updated request with status `COMPLETED`.
- **Database Effect**:
  - `UPDATE usage_requests SET status = 'COMPLETED', check_out_at = NOW`
  - `UPDATE resources SET status = 'AVAILABLE'`
  - `UPDATE usage_sessions SET ended_at = NOW, status = 'COMPLETED', actual_duration_minutes = ?`
  - `INSERT INTO audit_logs (action='CHECK_OUT')`
- **Error Cases**:
  - `400 Bad Request`: Request status is not `CHECKED_IN`.

---

### 5. Operator Controller (`/api/operator`)

#### `GET /api/operator/dashboard`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Output (200 OK)**:
  ```json
  {
    "pendingRequestsCount": 0,
    "availableResourcesCount": 11,
    "reservedResourcesCount": 0,
    "occupiedResourcesCount": 3,
    "maintenanceResourcesCount": 1,
    "totalResourcesCount": 15,
    "todayCheckInsCount": 4
  }
  ```
- **Database Effect**: Aggregated `COUNT` queries across `usage_requests`, `resources`, and `check_ins`.

#### `GET /api/operator/requests/pending`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Output (200 OK)**: Array of requests with status `PENDING`.
- **Database Effect**: `SELECT * FROM usage_requests WHERE status = 'PENDING' ORDER BY created_at DESC`.

#### `GET /api/operator/requests/approved`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Output (200 OK)**: Array of requests with status `APPROVED`.

#### `POST /api/operator/requests/{id}/approve`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Input**: Path variable `id`
- **Output (200 OK)**: Approved request with `expiresAt` set.
- **Database Effect**:
  - `UPDATE usage_requests SET status = 'APPROVED', approved_at = NOW, expires_at = NOW + 10m`
  - `UPDATE resources SET status = 'RESERVED'`
  - `INSERT INTO audit_logs (action='REQUEST_APPROVED')`
- **Error Cases**:
  - `400 Bad Request`: Request status is not `PENDING`.
  - `400 Bad Request`: Resource is no longer `AVAILABLE`.

#### `POST /api/operator/requests/{id}/reject`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Input (JSON Body)**:
  ```json
  {
    "reason": "Quota reached or facility maintenance"
  }
  ```
- **Output (200 OK)**: Rejected request.
- **Database Effect**:
  - `UPDATE usage_requests SET status = 'REJECTED', rejected_at = NOW, rejection_reason = ?`
  - `INSERT INTO audit_logs (action='REQUEST_REJECTED')`

#### `GET /api/operator/resources`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Output (200 OK)**: All resources across facilities.

#### `PATCH /api/operator/resources/{id}/status`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Input (JSON Body)**:
  ```json
  {
    "status": "MAINTENANCE"
  }
  ```
- **Output (200 OK)**: Updated resource.
- **Database Effect**:
  - `UPDATE resources SET status = ? WHERE id = ?`
  - `INSERT INTO audit_logs (action='RESOURCE_STATUS_CHANGED')`

---

### 6. Check-In Controller (`/api/check-in`)

#### `POST /api/check-in/verify`
- **Access**: Authenticated
- **Role Required**: `ROLE_OPERATOR`
- **Input (JSON Body)**:
  ```json
  {
    "requestCode": "QS-2026-50608",
    "method": "QR_SCAN"
  }
  ```
- **Output (200 OK)**:
  ```json
  {
    "id": 14,
    "requestCode": "QS-2026-50608",
    "status": "CHECKED_IN",
    "checkInAt": "2026-09-22T01:23:25",
    ...
  }
  ```
- **Database Effect**:
  - `UPDATE usage_requests SET status = 'CHECKED_IN', check_in_at = NOW`
  - `UPDATE resources SET status = 'OCCUPIED'`
  - `INSERT INTO check_ins (usage_request_id, operator_id, method, checked_in_at)`
  - `INSERT INTO usage_sessions (usage_request_id, resource_id, student_id, started_at, status='ACTIVE')`
  - `INSERT INTO audit_logs (action='CHECK_IN')`
- **Error Cases**:
  - `400 Bad Request`: Request is not `APPROVED` (e.g. already checked in or cancelled).
  - `400 Bad Request`: Request hold window expired (`LocalDateTime.now().isAfter(expiresAt)`).

---

### 7. History Controller (`/api/history`)

#### `GET /api/history`
- **Access**: Authenticated
- **Role Required**: Any authenticated user
- **Behavior**:
  - If caller is `ROLE_STUDENT`: returns only sessions where `student_id = caller.id`.
  - If caller is `ROLE_OPERATOR`: returns all facility sessions across the university.
- **Database Effect**:
  - `SELECT * FROM usage_sessions WHERE student_id = ? ORDER BY started_at DESC` (Student)
  - `SELECT * FROM usage_sessions ORDER BY started_at DESC` (Operator)
