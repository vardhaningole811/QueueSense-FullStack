# QueueSense — Finite State Machine & Lifecycle Transition Audit

## 1. Usage Request State Machine

```
                      [START: User Submits Request]
                                   │
                                   ▼
                             +-----------+
              ┌──────────────┤  PENDING  ├──────────────┐
              │              +-----+-----+              │
              │ Operator           │                    │ Student
              │ Rejects            │ Operator           │ Cancels
              ▼                    │ Approves           ▼
       +--------------+            ▼             +-------------+
       |   REJECTED   |      +-----------+       |  CANCELLED  |
       +--------------+   ┌──┤ APPROVED  ├──┐    +-------------+
                          │  +-----+-----+  │           ▲
                          │        │        │           │
                 Operator │        │ 10-min │ Student   │
                 Verifies │        │ Grace  │ Cancels   │
                 Check-in │        │ Passes │           │
                          ▼        ▼        └───────────┘
                    +------------+ +-----------+
                    | CHECKED_IN | |  EXPIRED  |
                    +-----+------+ +-----------+
                          │
                  Student │ or Operator
                 Checks   │ Out
                          ▼
                    +-----------+
                    | COMPLETED |
                    +-----------+
```

### Transition Validation Table: Usage Requests
| Initial State | Target State | Triggering Event | Valid? | Backend Enforcement |
| :--- | :--- | :--- | :--- | :--- |
| `—` | `PENDING` | Student submits `POST /api/requests` | **YES** | Verified resource is `AVAILABLE` & student has no active request. |
| `PENDING` | `APPROVED` | Operator clicks Approve | **YES** | `UsageRequestService.approveRequest()` |
| `PENDING` | `REJECTED` | Operator clicks Reject | **YES** | `UsageRequestService.rejectRequest()` |
| `PENDING` | `CANCELLED` | Student clicks Cancel | **YES** | `UsageRequestService.cancelRequest()` |
| `APPROVED` | `CHECKED_IN`| Operator scans QR / verifies code | **YES** | `CheckInService.verifyAndCheckIn()` |
| `APPROVED` | `CANCELLED` | Student cancels approved reservation | **YES** | `UsageRequestService.cancelRequest()` (releases `RESERVED` back to `AVAILABLE`) |
| `APPROVED` | `EXPIRED` | Time elapsed > `expiresAt` (10m) | **YES** | Enforced on check-in verification attempt. *(See Gap 1 below)* |
| `CHECKED_IN`| `COMPLETED` | Student or Operator checks out | **YES** | `CheckOutService.checkOut()` (releases `OCCUPIED` back to `AVAILABLE`) |
| `PENDING` | `CHECKED_IN`| Operator scans non-approved request | **NO** | Blocked: `Status must be APPROVED` |
| `REJECTED` | `APPROVED` | Approval after rejection | **NO** | Blocked: `Status must be PENDING` |
| `COMPLETED` | `CHECKED_IN`| Re-checking in completed request | **NO** | Blocked: `Status must be APPROVED` |
| `EXPIRED` | `CHECKED_IN`| Checking in after grace period | **NO** | Blocked: Auto-releases resource to `AVAILABLE` and throws exception |
| `CHECKED_IN`| `CANCELLED` | Cancelling while actively occupied | **NO** | Blocked: Must use checkout endpoint |

---

## 2. Resource State Machine

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

### Transition Validation Table: Resources
| Initial State | Target State | Triggering Event | Valid? | Backend Enforcement |
| :--- | :--- | :--- | :--- | :--- |
| `AVAILABLE` | `RESERVED` | Usage request approved | **YES** | Set in `UsageRequestService.approveRequest()` |
| `RESERVED` | `OCCUPIED` | Check-in verified via QR/Code | **YES** | Set in `CheckInService.verifyAndCheckIn()` |
| `OCCUPIED` | `AVAILABLE` | Session checkout concluded | **YES** | Set in `CheckOutService.checkOut()` |
| `RESERVED` | `AVAILABLE` | Request cancelled or expired | **YES** | Released in `cancelRequest()` and `verifyAndCheckIn()` |
| `AVAILABLE` | `MAINTENANCE`| Operator toggles maintenance mode | **YES** | `ResourceService.updateResourceStatus()` |
| `MAINTENANCE`| `AVAILABLE` | Operator releases maintenance mode | **YES** | `ResourceService.updateResourceStatus()` |

---

## 3. Critical State Machine Gaps & Edge Cases

### Gap 1: Lazy Expiration Evaluation (No Active Background Scheduler)
- **Current Behavior**: If an approved request passes its 10-minute grace period (`expiresAt`), the database record remains `APPROVED` and the resource remains `RESERVED` until the operator explicitly tries to verify that specific request or until the student cancels.
- **Why it matters**: If a student is a complete "no-show" and never shows up, the seat remains permanently locked in `RESERVED` state, preventing other students from booking it.
- **Recommended Solution**: Add a scheduled Spring task (`@EnableScheduling` + `@Scheduled(fixedRate = 60000)`) in `UsageRequestService` that sweeps all `APPROVED` requests where `expiresAt < NOW()`, marks them `EXPIRED`, and releases their resources back to `AVAILABLE`.

### Gap 2: Operator Maintenance Override during Active Occupancy
- **Current Behavior**: An operator calling `PATCH /api/operator/resources/{id}/status` can force a resource to `MAINTENANCE` even if a student is currently `CHECKED_IN` and actively occupying it.
- **Why it matters**: Leaves the student with an orphan active session while the resource displays maintenance.
- **Recommended Solution**: Add a check in `ResourceService.updateResourceStatus`: only allow status updates if `resource.getStatus() == AVAILABLE` or `resource.getStatus() == MAINTENANCE`. If `OCCUPIED` or `RESERVED`, require checkout or reservation cancellation first.
