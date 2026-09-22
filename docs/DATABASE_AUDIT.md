# QueueSense — Database Schema & Data Mutation Audit

## 1. Relational Database Overview

QueueSense runs on **PostgreSQL 16.3**. The schema is managed via Hibernate JPA (`ddl-auto: update`). The schema contains **10 tables** capturing users, profiles, facilities, resources, reservation requests, check-ins, sessions, audit records, and notifications.

---

## 2. Table-by-Table Schema Specification

### 1. `users` Table
Stores authentication credentials and core identity for all campus actors.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Unique user ID |
| `email` | `VARCHAR(100)` | NO | UNIQUE | Login identifier |
| `password_hash` | `VARCHAR(255)` | NO | — | BCrypt hashed password |
| `role` | `VARCHAR(30)` | NO | — | `ROLE_STUDENT`, `ROLE_OPERATOR`, `ROLE_ADMIN` |
| `name` | `VARCHAR(100)` | NO | — | Full display name |
| `active` | `BOOLEAN` | NO | DEFAULT true | Account status flag |
| `created_at` | `TIMESTAMP` | NO | — | Account creation timestamp |
| `updated_at` | `TIMESTAMP` | NO | — | Last update timestamp |

### 2. `student_profiles` Table
Stores university-specific academic metadata for students.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Profile ID |
| `user_id` | `BIGINT` | NO | FK (`users.id`), UNIQUE | 1-to-1 link with user |
| `student_id` | `VARCHAR(50)` | NO | UNIQUE | Campus roll/reg number (`STU-2026-001`) |
| `department` | `VARCHAR(100)` | NO | — | e.g. "Computer Science & Engineering" |
| `year` | `INTEGER` | NO | — | Academic year (1, 2, 3, 4) |

### 3. `operator_profiles` Table
Stores facility assignment for library / facility staff.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Profile ID |
| `user_id` | `BIGINT` | NO | FK (`users.id`), UNIQUE | 1-to-1 link with user |
| `employee_id` | `VARCHAR(50)` | NO | UNIQUE | Campus employee ID (`EMP-2026-088`) |
| `assigned_facility_id` | `BIGINT` | YES | FK (`facilities.id`) | Facility under operator supervision |

### 4. `facilities` Table
Campus study and athletic spaces.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Facility ID |
| `name` | `VARCHAR(100)` | NO | — | Facility title (e.g. "E-Library") |
| `type` | `VARCHAR(50)` | NO | — | Category (e.g. "LIBRARY", "SPORTS") |
| `description` | `VARCHAR(500)` | YES | — | Overview of amenities |
| `active` | `BOOLEAN` | NO | DEFAULT true | Facility status flag |
| `created_at` | `TIMESTAMP` | NO | — | Creation timestamp |

### 5. `resources` Table
Individual bookable units (seats, computers, courts, desks).
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Resource ID |
| `facility_id` | `BIGINT` | NO | FK (`facilities.id`) | Parent facility |
| `name` | `VARCHAR(100)` | NO | — | Label (e.g. "Seat 01", "PC 04") |
| `type` | `VARCHAR(50)` | NO | — | "SEAT", "PC", "DESK", "COURT" |
| `status` | `VARCHAR(30)` | NO | — | `AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE` |
| `active` | `BOOLEAN` | NO | DEFAULT true | Unit active flag |

### 6. `usage_requests` Table
Core transactional entity representing student reservation requests.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Request ID |
| `request_code` | `VARCHAR(50)` | NO | UNIQUE | Human/QR code (`QS-2026-XXXXX`) |
| `student_id` | `BIGINT` | NO | FK (`users.id`) | Requesting student |
| `resource_id` | `BIGINT` | NO | FK (`resources.id`) | Target resource |
| `facility_id` | `BIGINT` | NO | FK (`facilities.id`) | Target facility |
| `expected_arrival`| `VARCHAR(50)` | NO | — | Arrival slot (e.g. "Immediate", "10:30 AM") |
| `expected_duration`| `VARCHAR(50)`| NO | — | Duration (e.g. "1 hour", "2 hours") |
| `group_size` | `INTEGER` | NO | DEFAULT 1 | 1 to 3 students |
| `status` | `VARCHAR(30)` | NO | — | `PENDING`, `APPROVED`, `REJECTED`, `CHECKED_IN`, `COMPLETED`, `CANCELLED`, `EXPIRED` |
| `created_at` | `TIMESTAMP` | NO | — | Request creation timestamp |
| `approved_at` | `TIMESTAMP` | YES | — | Timestamp of operator approval |
| `rejected_at` | `TIMESTAMP` | YES | — | Timestamp of operator rejection |
| `rejection_reason`| `VARCHAR(255)`| YES | — | Reason text if rejected |
| `check_in_at` | `TIMESTAMP` | YES | — | Physical check-in timestamp |
| `check_out_at`| `TIMESTAMP` | YES | — | Session conclusion timestamp |
| `expires_at` | `TIMESTAMP` | YES | — | Approval hold expiration timestamp |

### 7. `check_ins` Table
Verification proof logged upon physical check-in.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Check-in ID |
| `usage_request_id`| `BIGINT` | NO | FK (`usage_requests.id`), UNIQUE | Exactly 1 check-in per request |
| `operator_id` | `BIGINT` | NO | FK (`users.id`) | Operator who verified check-in |
| `method` | `VARCHAR(30)` | NO | — | `QR_SCAN`, `MANUAL_CODE` |
| `checked_in_at` | `TIMESTAMP` | NO | — | Verification timestamp |

### 8. `usage_sessions` Table
Active and historical usage duration tracking.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Session ID |
| `usage_request_id`| `BIGINT` | NO | FK (`usage_requests.id`), UNIQUE | Linked usage request |
| `resource_id` | `BIGINT` | NO | FK (`resources.id`) | Resource used |
| `student_id` | `BIGINT` | NO | FK (`users.id`) | Student who utilized space |
| `started_at` | `TIMESTAMP` | NO | — | Check-in timestamp |
| `ended_at` | `TIMESTAMP` | YES | — | Checkout timestamp (null while active) |
| `actual_duration_minutes` | `BIGINT` | YES | — | Computed elapsed minutes |
| `status` | `VARCHAR(30)` | NO | — | `ACTIVE`, `COMPLETED` |

### 9. `audit_logs` Table
Immutable operational audit trail.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Log entry ID |
| `actor_email` | `VARCHAR(100)` | NO | — | Email of actor performing action |
| `actor_role` | `VARCHAR(50)` | NO | — | Role of actor at time of action |
| `action` | `VARCHAR(100)` | NO | — | `LOGIN`, `REQUEST_CREATED`, `REQUEST_APPROVED`, `CHECK_IN`, `CHECK_OUT`, ... |
| `entity_name` | `VARCHAR(100)` | YES | — | Target entity (e.g. "UsageRequest") |
| `entity_id` | `VARCHAR(100)` | YES | — | Target entity ID |
| `details` | `VARCHAR(1000)`| YES | — | Human-readable log details |
| `timestamp` | `TIMESTAMP` | NO | — | Log event timestamp |

### 10. `notifications` Table
In-app student alert table.
| Column | Type | Nullable | Constraints | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | NO | PRIMARY KEY, AUTO_INCREMENT | Notification ID |
| `user_id` | `BIGINT` | NO | FK (`users.id`) | Recipient student |
| `title` | `VARCHAR(150)` | NO | — | Alert title |
| `message` | `VARCHAR(1000)`| NO | — | Alert body |
| `is_read` | `BOOLEAN` | NO | DEFAULT false | Read status |
| `created_at` | `TIMESTAMP` | NO | — | Timestamp |

---

## 3. Database Lifecycle: State Transitions & Record Changes

```
[Available Resource]
        │
        ▼ Student submits request (POST /api/requests)
+------------------------------------------------------------------------------------+
| 1. INSERT INTO usage_requests (requestCode='QS-2026-XXXXX', status='PENDING', ...) |
| 2. INSERT INTO audit_logs (action='REQUEST_CREATED', ...)                          |
| Resource remains: status='AVAILABLE'                                               |
+------------------------------------------------------------------------------------+
        │
        ▼ Operator approves request (POST /api/operator/requests/{id}/approve)
+------------------------------------------------------------------------------------+
| 1. UPDATE usage_requests SET status='APPROVED', approved_at=NOW, expires_at=NOW+10m|
| 2. UPDATE resources SET status='RESERVED' WHERE id=resourceId                      |
| 3. INSERT INTO audit_logs (action='REQUEST_APPROVED', ...)                         |
+------------------------------------------------------------------------------------+
        │
        ▼ Operator scans QR / verifies code (POST /api/check-in/verify)
+------------------------------------------------------------------------------------+
| 1. UPDATE usage_requests SET status='CHECKED_IN', check_in_at=NOW                  |
| 2. UPDATE resources SET status='OCCUPIED' WHERE id=resourceId                      |
| 3. INSERT INTO check_ins (usage_request_id, operator_id, method='QR_SCAN', ...)    |
| 4. INSERT INTO usage_sessions (usage_request_id, started_at=NOW, status='ACTIVE')  |
| 5. INSERT INTO audit_logs (action='CHECK_IN', ...)                                 |
+------------------------------------------------------------------------------------+
        │
        ▼ Student or Operator checks out (POST /api/requests/{id}/check-out)
+------------------------------------------------------------------------------------+
| 1. UPDATE usage_requests SET status='COMPLETED', check_out_at=NOW                  |
| 2. UPDATE resources SET status='AVAILABLE' WHERE id=resourceId                     |
| 3. UPDATE usage_sessions SET ended_at=NOW, actual_duration_minutes=X,              |
|    status='COMPLETED' WHERE usage_request_id=requestId                             |
| 4. INSERT INTO audit_logs (action='CHECK_OUT', ...)                                |
+------------------------------------------------------------------------------------+
```

---

## 4. Foreign Key Relationships & Cascade Behavior

```
               ┌───────────────┐
               │     users     │
               └───┬───────┬───┘
                   │1      │1
                   │1      │1
         ┌─────────▼─┐   ┌─▼─────────────┐
         │ student_  │   │ operator_     ├────────┐
         │ profiles  │   │ profiles      │        │
         └───────────┘   └───────────────┘        │
                   │1                             │
                   │*                             │
               ┌───▼───────────┐         ┌────────▼──────┐
               │ usage_requests│◄────────┤  facilities   │
               └───┬───┬───┬───┘         └───┬───────────┘
                   │1  │1  │*                │1
                   │1  │1  └────────┐        │*
        ┌──────────▼┐ ┌▼──────────┐ │    ┌───▼───────────┐
        │ check_ins │ │usage_     │ └───►│   resources   │
        └───────────┘ │sessions   │      └───────────────┘
                      └───────────┘
```

- **Cascade Rules**: Currently, all `@ManyToOne` and `@OneToOne` associations use default JPA fetch types without `CascadeType.REMOVE` or `CascadeType.ALL`. This prevents accidental catastrophic deletions (e.g. deleting a student does not erase audit logs or usage history).
- **Constraints & Indexes**:
  - `users(email)`: Unique Index.
  - `usage_requests(request_code)`: Unique Index.
  - `check_ins(usage_request_id)`: Unique Index.
  - `usage_sessions(usage_request_id)`: Unique Index.
  - `student_profiles(student_id)`: Unique Index.
  - `operator_profiles(employee_id)`: Unique Index.
