# QueueSense
## Smart Campus Facility Availability, Booking & Resource Management System
IMPORTANT:
Before doing any implementation work, read PRD.md completely and understand
the requirements, workflow, architecture, UI requirements, security
requirements, database requirements, and final acceptance test.

PRD.md is the product source of truth.

After comprehensively reading PRD.md, follow all implementation,
testing, debugging, and completion instructions in this file.

**Project:** Idea Lab – RBU CSE  
**Version:** 2.0  
**Status:** Competition-quality functional prototype  
**Primary Goal:** Build a complete, connected full-stack prototype with a real backend, database, authentication, resource state management, approval workflow, QR-based check-in, checkout, and history.

---

# 1. PROJECT VISION

QueueSense is a smart campus resource management platform designed to reduce uncertainty, waiting, crowding, and inefficient utilization of university facilities.

The system connects:

Student
↓
Resource Availability
↓
Usage Request
↓
Facility Operator
↓
Approval
↓
Digital Confirmation
↓
QR Check-in
↓
Active Usage
↓
Checkout
↓
History

QueueSense must be implemented as a real connected application.

It must NOT be a collection of disconnected HTML pages.

The frontend must communicate with the backend through APIs.

The backend must communicate with the database.

The database must be the source of truth for request, user, resource, and usage states.

---

# 2. PRIMARY OBJECTIVE

The most important requirement is the following complete workflow:

Student Login
→ Student Dashboard
→ Libraries
→ E-Library
→ Available Resource
→ Create Request
→ Pending Request
→ Operator Login
→ Pending Request Appears
→ Approve
→ Student My Requests
→ Approved
→ Digital Confirmation
→ Real QR Code
→ Operator Check-in
→ Validation
→ Checked In
→ Resource Occupied
→ Checkout
→ Completed
→ Resource Available
→ History

Every step must work using the same backend and database state.

If one step is disconnected from another, the implementation is incomplete.

---

# 3. PROBLEM STATEMENT

Students frequently do not know whether a campus resource is available before physically reaching the facility.

This can result in:

- unnecessary walking
- waiting
- overcrowding
- inefficient resource usage
- resource conflicts
- poor visibility of facility availability

Facility operators also need a centralized way to:

- receive requests
- approve/reject requests
- monitor resource availability
- verify check-ins
- monitor active usage
- record checkouts
- view history

QueueSense provides one connected system for both students and facility operators.

---

# 4. TARGET USERS

## 4.1 Student

Students can:

- log in
- view facilities
- view resource availability
- select resources
- submit usage requests
- track request status
- view approved requests
- view digital confirmation
- display QR code
- check in
- check out
- view usage history

## 4.2 Facility Operator

Operators can:

- log in
- view assigned facility resources
- see pending requests
- open request details
- approve requests
- reject requests
- view approved requests
- scan/verify QR credentials
- check students in
- check students out
- monitor occupied resources
- view history
- view dashboard statistics

## 4.3 Admin

Admin architecture should exist even if the first demonstration focuses only on Student and Operator.

Admin may later:

- manage users
- manage facilities
- manage resources
- manage operators
- view audit logs

---

# 5. INITIAL FACILITY SCOPE

The initial demonstration focuses on:

1. Library
2. Sports

Snackify is intentionally outside the initial core demonstration scope.

The architecture should make adding additional facilities possible later.

---

# 6. LIBRARY SCOPE

The library area should support:

- E-Library
- Reading Room
- Library seating
- Computer resources where applicable

Prototype E-Library resources:

- Seat 01
- Seat 02
- Seat 03
- PC 04
- Seat 05
- Seat 06
- Seat 07
- PC 08
- Seat 09

These must come from database seed data.

Do NOT hard-code them into React components.

---

# 7. LIBRARY BOOKING RULE

Library resources do NOT require rigid fixed booking slots.

Students should be able to specify:

- expected arrival
- expected duration

The expected duration is informational and used for resource planning.

The prototype should allow reasonable duration values.

The system should not force every library booking into exactly one or two hours.

---

# 8. SPORTS SCOPE

Sports should exist in the architecture.

Initial prototype resources may include:

- Badminton Court
- Tennis Court
- Basketball Court

Sports may use fixed time slots.

The implementation should keep library and sports scheduling rules separate so that future scheduling rules can be added without rewriting the entire application.

---

# 9. AUTHENTICATION

The system must have separate portal experiences:

Student Portal

Operator Portal

Authentication must be implemented by the backend.

Do NOT trust frontend localStorage values to determine a user's role.

The backend must determine the authenticated user's identity and role.

Use:

- Spring Security
- JWT authentication
- password hashing

Passwords must never be stored in plaintext.

---

# 10. TECHNOLOGY STACK

## Frontend

- React
- Vite
- JavaScript or TypeScript
- React Router
- Axios or Fetch API
- reusable React components
- CSS modules or organized CSS

## Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- Bean Validation

## Database

- PostgreSQL

## QR

Use a proper QR-code generation library.

Do NOT create a fake QR using CSS.

## Build tools

Frontend:

npm

Backend:

Maven

---

# 11. ARCHITECTURE

Use a clean full-stack architecture:

React/Vite Frontend
        ↓
REST API
        ↓
Spring Boot Backend
        ↓
Service Layer
        ↓
Repository Layer
        ↓
PostgreSQL Database

Authentication:

React
↓
Spring Security
↓
JWT
↓
Authenticated API requests

The frontend must never directly access PostgreSQL.

---

# 12. BACKEND PACKAGE STRUCTURE

Use a clean structure similar to:

backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── queuesense/
                    ├── config/
                    ├── controller/
                    ├── dto/
                    ├── entity/
                    ├── enums/
                    ├── exception/
                    ├── repository/
                    ├── security/
                    ├── service/
                    └── QueueSenseApplication.java

The exact package name may be changed if required.

Avoid putting all logic into controllers.

Business logic belongs in services.

---

# 13. FRONTEND STRUCTURE

Use reusable components.

Suggested structure:

frontend/
└── src/
    ├── assets/
    ├── components/
    │   ├── layout/
    │   ├── common/
    │   ├── student/
    │   └── operator/
    ├── pages/
    │   ├── auth/
    │   ├── student/
    │   └── operator/
    ├── services/
    ├── context/
    ├── hooks/
    ├── utils/
    ├── App.jsx
    └── main.jsx

Exact names may be changed if a better structure is used.

---

# 14. DATABASE ENTITIES

Minimum entities:

User

StudentProfile

OperatorProfile

Facility

Resource

UsageRequest

CheckIn

UsageSession

AuditLog

Notification

---

# 15. USER ENTITY

User fields should approximately include:

- id
- email
- passwordHash
- role
- name
- active
- createdAt
- updatedAt

Roles:

STUDENT

OPERATOR

ADMIN

---

# 16. STUDENT PROFILE

StudentProfile should contain information such as:

- id
- userId
- studentId
- department
- year

The student ID should be unique.

---

# 17. OPERATOR PROFILE

OperatorProfile should contain:

- id
- userId
- employeeId
- assignedFacilityId

The exact relationship can be adjusted according to the implementation.

---

# 18. FACILITY ENTITY

Facility:

- id
- name
- type
- description
- active
- createdAt

Examples:

E-Library

Reading Room

Sports Complex

---

# 19. RESOURCE ENTITY

Resource:

- id
- facilityId
- name
- type
- status
- active

Resource statuses:

AVAILABLE

RESERVED

OCCUPIED

MAINTENANCE

---

# 20. USAGE REQUEST ENTITY

UsageRequest should contain approximately:

- id
- requestCode
- studentId
- resourceId
- facilityId
- expectedArrival
- expectedDuration
- groupSize
- status
- createdAt
- approvedAt
- rejectedAt
- rejectionReason
- checkInAt
- checkOutAt
- expiresAt

---

# 21. REQUEST STATES

Allowed states:

PENDING

APPROVED

REJECTED

CHECKED_IN

COMPLETED

CANCELLED

EXPIRED

Valid transitions:

PENDING → APPROVED

PENDING → REJECTED

PENDING → CANCELLED

APPROVED → CHECKED_IN

APPROVED → EXPIRED

CHECKED_IN → COMPLETED

Invalid transitions must be rejected by the backend.

Examples:

REJECTED → CHECKED_IN

COMPLETED → APPROVED

EXPIRED → CHECKED_IN

must never be allowed.

---

# 22. RESOURCE STATE MACHINE

Resources can be:

AVAILABLE

RESERVED

OCCUPIED

MAINTENANCE

Typical transitions:

AVAILABLE → RESERVED

RESERVED → OCCUPIED

OCCUPIED → AVAILABLE

AVAILABLE → MAINTENANCE

MAINTENANCE → AVAILABLE

The backend must prevent invalid resource states.

---

# 23. REQUEST CREATION

When a student submits a request:

1. Authenticate student.
2. Validate resource.
3. Validate facility.
4. Validate expected arrival.
5. Validate expected duration.
6. Validate group size.
7. Check resource availability.
8. Check duplicate active requests.
9. Create UsageRequest.
10. Generate request code.
11. Save request to database.
12. Return request details.
13. Frontend displays PENDING state.

The request must NOT be stored only in browser localStorage.

---

# 24. DUPLICATE REQUEST PROTECTION

A student should not be able to create unlimited active requests for the same resource.

Prevent obvious duplicate active requests.

The backend must be responsible for this validation.

---

# 25. CONCURRENCY

QueueSense is a resource allocation system.

Two students must not be able to successfully reserve the same resource simultaneously.

Use appropriate database transaction handling and locking/constraints.

Approval and resource reservation should be performed atomically where necessary.

---

# 26. OPERATOR APPROVAL

Operator pending request view should contain:

- Student ID
- Request ID
- Facility
- Resource
- Expected arrival
- Expected duration
- Group size
- Request time

Actions:

Approve

Reject

When Approve is clicked:

1. Frontend sends API request.
2. Backend authenticates operator.
3. Backend checks operator authorization.
4. Backend checks request is PENDING.
5. Backend checks resource availability.
6. Backend changes request to APPROVED.
7. Backend changes resource to RESERVED.
8. Approval timestamp is recorded.
9. Audit log is created.
10. Response is returned.
11. Operator UI refreshes.
12. Request disappears from pending list.

---

# 27. REJECTION

When operator rejects:

- status becomes REJECTED
- rejection timestamp is stored
- optional rejection reason is stored
- audit log is created
- student can see rejection status

---

# 28. STUDENT MY REQUESTS

My Requests must be database-driven.

Categories may include:

- Active
- Pending
- Approved
- Rejected
- Completed

At minimum the demonstration must clearly show:

PENDING

APPROVED

CHECKED_IN

COMPLETED

---

# 29. APPROVED REQUEST UI

An approved request should visually stand out.

Example structure:

E-Library

Seat 03

APPROVED

Request ID: QS-2026-00124

Expected arrival: 10:30 AM

Expected duration: 2 hours

[View Digital Confirmation]

The APPROVED state should use a green status indicator.

---

# 30. DIGITAL CONFIRMATION

The confirmation page must show:

QueueSense branding

Request Approved

Request ID

Student ID

Facility

Resource

Expected arrival

Expected duration

QR code

Check-in instructions

Proceed to Check-in

The page must only be accessible when the request is APPROVED or otherwise in a valid check-in state.

If the request is not approved:

redirect to the request status page

and explain the reason.

Do not simply show a blank page.

---

# 31. QR CODE

Use a real QR-code library.

Do NOT create a fake CSS QR pattern.

The QR should contain only a safe identifier.

Example:

requestCode

or a short-lived signed check-in token.

Do not place passwords or unnecessary student personal information inside the QR.

---

# 32. QR CHECK-IN

The operator should have a Check-in interface.

Preferred workflow:

Operator opens Check-in.

↓

Scan QR

↓

System extracts request identifier.

↓

Frontend sends identifier to backend.

↓

Backend validates request.

Validation must include:

1. request exists
2. request belongs to a valid resource
3. request status is APPROVED
4. request has not expired
5. request has not already been checked in
6. request has not been cancelled
7. operator is authorized

If valid:

request → CHECKED_IN

resource → OCCUPIED

check-in timestamp recorded

usage session created

audit log created

---

# 33. QR SCANNING IMPLEMENTATION

The application should support browser camera scanning if practical.

Also provide a manual request-code fallback for the prototype.

This is important for demonstrations because camera permissions can fail on laptops.

Example:

[Scan QR]

or

[Enter Request Code]

Both must ultimately call the same backend validation.

---

# 34. CHECKOUT

Checkout can be initiated by the student or operator according to the UI design.

Backend must verify:

request status == CHECKED_IN

Then:

request → COMPLETED

resource → AVAILABLE

check-out timestamp recorded

usage session closed

audit log created

---

# 35. EARLY CHECKOUT

Students may check out earlier than their expected duration.

Actual usage duration should be calculated from:

check-in timestamp

to

check-out timestamp

The resource becomes AVAILABLE immediately after successful checkout.

---

# 36. NO-SHOW RULE

Prototype grace period:

10 minutes

The value should be configurable.

If the student does not check in within the allowed grace period:

APPROVED → EXPIRED

Resource:

RESERVED → AVAILABLE

The system should not leave the resource permanently reserved.

---

# 37. GROUP REQUESTS

Allow group size:

1–3

The main requester remains responsible for the request.

Future versions can support individual group-member accounts.

---

# 38. OPERATOR DASHBOARD

Operator dashboard should prioritize operational information.

Statistics:

- Pending Requests
- Available Resources
- Reserved Resources
- Currently Occupied
- Today's Check-ins

Statistics must be calculated from actual database state.

Do NOT display fake hard-coded numbers.

---

# 39. OPERATOR PENDING REQUESTS

Pending requests should be easy to process.

The operator should not need to navigate through many pages.

Each request card should display:

Student

Request ID

Resource

Arrival

Duration

Group size

Requested time

Actions:

Approve

Reject

---

# 40. OPERATOR APPROVED/ACTIVE REQUESTS

After approval, the request should leave Pending.

It should appear in:

Approved Requests

or

Active/Reserved Requests

depending on the UI architecture.

After check-in:

it should appear as active/occupied.

After checkout:

it moves to history.

---

# 41. STUDENT DASHBOARD

Student dashboard should prioritize:

1. Current active request
2. Resource availability
3. Quick actions
4. Recent activity

The most important current request should be visible without excessive scrolling.

---

# 42. ACTIVE REQUEST CARD

If a student has an active request, show:

Facility

Resource

Status

Request ID

Expected arrival

Expected duration

Relevant action

For example:

APPROVED

[View Digital Confirmation]

or

CHECKED_IN

[Check Out]

---

# 43. RESOURCE AVAILABILITY

Resource cards must be database-driven.

Statuses:

AVAILABLE

RESERVED

OCCUPIED

MAINTENANCE

Visual meanings:

AVAILABLE = green

RESERVED = amber

OCCUPIED = red

MAINTENANCE = gray

Do not rely only on color.

Display text labels as well.

---

# 44. HISTORY

History should show completed usage.

Information:

Request ID

Facility

Resource

Check-in

Check-out

Duration

Status

History must come from backend data.

---

# 45. NOTIFICATIONS

For prototype use in-app notifications/toasts.

Examples:

Request submitted.

Request approved.

Request rejected.

Check-in successful.

Check-out successful.

Future versions can support email/push notifications.

---

# 46. API DESIGN

Suggested API endpoints:

POST /api/auth/login

POST /api/auth/logout

GET /api/auth/me

GET /api/facilities

GET /api/facilities/{id}

GET /api/resources

GET /api/resources/{id}

POST /api/requests

GET /api/requests/my

GET /api/requests/{id}

POST /api/requests/{id}/cancel

GET /api/operator/dashboard

GET /api/operator/requests/pending

GET /api/operator/requests/approved

POST /api/operator/requests/{id}/approve

POST /api/operator/requests/{id}/reject

POST /api/requests/{id}/check-in

POST /api/requests/{id}/check-out

POST /api/check-in/verify

GET /api/history

GET /api/operator/resources

---

# 47. API SECURITY

Student endpoints must require STUDENT authorization where appropriate.

Operator endpoints must require OPERATOR authorization.

Admin endpoints must require ADMIN authorization.

Never rely on:

localStorage role

frontend route

hidden button

query parameter

for authorization.

The backend must enforce permissions.

---

# 48. VALIDATION

Validate:

- missing fields
- invalid resource
- unavailable resource
- invalid duration
- invalid group size
- unauthorized access
- invalid request state
- duplicate active request
- expired request
- invalid check-in
- invalid checkout

Return useful HTTP status codes and clear error messages.

---

# 49. ERROR HANDLING

Important pages must handle:

- loading
- empty state
- server error
- unauthorized
- forbidden
- expired request
- rejected request
- already checked in
- already completed
- unavailable resource
- missing request

Never show a blank white screen.

---

# 50. EMPTY STATES

Examples:

No pending requests

No active requests

No usage history yet

No available resources

Each empty state should explain what the user can do next.

---

# 51. FRONTEND ROUTES

Recommended:

/login

/student/dashboard

/student/libraries

/student/libraries/:facilityId

/student/resources/:resourceId

/student/requests

/student/requests/:requestId

/student/confirmation/:requestId

/student/check-in/:requestId

/student/history

/operator/dashboard

/operator/requests

/operator/resources

/operator/check-ins

/operator/history

Routes may be adjusted if a cleaner architecture is used.

---

# 52. FRONTEND STATE

React state should be used for UI state.

Backend/database should be the source of truth for persistent workflow state.

Do NOT recreate the old prototype's approach where localStorage contains the entire request.

LocalStorage may only be used for appropriate client-side information such as:

- JWT/token storage if the selected security architecture requires it
- UI preferences

The request state must always come from backend API responses.

---

# 53. DESIGN SYSTEM

QueueSense should look like one product.

Primary:

#101828

Accent:

#14B8A6

Background:

#F5F7FA

Card:

#FFFFFF

Border:

#E5E7EB

Text:

#17202A

Muted:

#667085

Success:

#16A34A

Warning:

#D97706

Error:

#DC2626

Use accent colors carefully.

---

# 54. TYPOGRAPHY

Use:

Inter

or a clean system-safe equivalent.

Do not mix unrelated fonts.

Hierarchy:

Page title: 28–32px

Section title: 18–22px

Body: 14–16px

Secondary: 12–13px

---

# 55. VISUAL DIRECTION

The UI should be:

- professional
- clean
- modern
- campus-oriented
- credible
- practical
- easy to understand

It should feel like a serious university technology product.

It should NOT look like:

- generic AI-generated SaaS
- a landing page
- a template copied from a startup website
- an excessive glassmorphism dashboard
- an over-designed Dribbble concept

---

# 56. AVOID AI-GENERATED VISUAL STYLE

Avoid:

- excessive gradients
- excessive glassmorphism
- huge hero sections
- oversized decorative illustrations
- unnecessary charts
- fake statistics
- random icons
- excessive rounded cards
- excessive animations
- marketing-heavy copy

Prefer:

- structured layouts
- restrained colors
- consistent spacing
- practical information hierarchy
- real operational data
- clear buttons
- readable tables/cards
- meaningful status indicators

---

# 57. SHARED UI

All pages must share:

- QueueSense logo
- navigation
- typography
- buttons
- cards
- status badges
- spacing
- page structure
- sidebar/topbar behavior

Do not independently design every page.

Create reusable components.

---

# 58. RESPONSIVE DESIGN

Support:

- desktop
- laptop
- tablet
- mobile

Desktop is the primary live demonstration environment.

---

# 59. ACCESSIBILITY

Use:

- readable contrast
- keyboard-accessible buttons
- meaningful labels
- form labels
- semantic HTML
- visible status text
- focus states

Do not communicate state through color alone.

---

# 60. DEMO ACCOUNTS

Seed:

Student:

student@queuesense.demo

Operator:

operator@queuesense.demo

Use a documented local demo password.

Credentials must not be embedded into frontend source code.

---

# 61. SEED DATA

Seed:

Facilities:

E-Library

Reading Room

Sports Complex

Resources:

Seat 01

Seat 02

Seat 03

PC 04

Seat 05

Seat 06

Seat 07

PC 08

Seat 09

Badminton Court

Tennis Court

Basketball Court

Create different initial resource states.

The database should contain enough realistic data to demonstrate availability.

---

# 62. AUDIT LOGGING

Important actions should create audit records.

Examples:

LOGIN

REQUEST_CREATED

REQUEST_APPROVED

REQUEST_REJECTED

CHECK_IN

CHECK_OUT

REQUEST_CANCELLED

RESOURCE_STATUS_CHANGED

Audit logs should contain:

actor

action

entity

timestamp

---

# 63. QR SECURITY

QR should not contain:

- password
- JWT
- unnecessary student information
- sensitive data

Prefer:

requestCode

or

short-lived signed token

Backend must perform final validation.

---

# 64. DEMO WORKFLOW

The exact demonstration scenario:

STEP 1

Open QueueSense.

STEP 2

Student logs in.

STEP 3

Student reaches Student Dashboard.

STEP 4

Student opens Libraries.

STEP 5

Student selects E-Library.

STEP 6

Student selects Seat 03.

STEP 7

Student enters expected arrival and duration.

STEP 8

Student submits request.

STEP 9

My Requests shows:

PENDING

STEP 10

Operator logs in.

STEP 11

Operator sees the request under Pending Requests.

STEP 12

Operator opens request details.

STEP 13

Operator approves.

STEP 14

Backend updates database.

STEP 15

Resource becomes RESERVED.

STEP 16

Student opens My Requests.

STEP 17

Request displays:

APPROVED

green status

STEP 18

Student clicks:

View Digital Confirmation

STEP 19

Confirmation page displays:

Request ID

Student ID

Facility

Resource

Arrival

Duration

QR

STEP 20

Operator opens Check-in.

STEP 21

Operator scans QR or enters request code.

STEP 22

Backend validates request.

STEP 23

Operator confirms check-in.

STEP 24

Request becomes CHECKED_IN.

STEP 25

Resource becomes OCCUPIED.

STEP 26

Student sees active usage.

STEP 27

Student checks out.

STEP 28

Request becomes COMPLETED.

STEP 29

Resource becomes AVAILABLE.

STEP 30

History displays completed usage.

This exact flow must work before declaring the project complete.

---

# 65. DEVELOPMENT REQUIREMENTS

The coding agent must:

1. Inspect workspace.
2. Create project structure.
3. Set up frontend.
4. Set up Spring Boot backend.
5. Configure PostgreSQL.
6. Configure JPA/Hibernate.
7. Configure Spring Security/JWT.
8. Create entities.
9. Create repositories.
10. Create services.
11. Create controllers.
12. Create validation.
13. Create exception handling.
14. Create database schema.
15. Create seed data.
16. Create authentication.
17. Build React routes.
18. Build reusable UI components.
19. Connect frontend to backend.
20. Implement request workflow.
21. Implement operator workflow.
22. Implement QR generation.
23. Implement QR verification.
24. Implement check-in.
25. Implement checkout.
26. Implement history.
27. Implement dashboard statistics.
28. Run backend.
29. Run frontend.
30. Test APIs.
31. Test browser workflow.
32. Fix errors.
33. Retest.

Do not stop after generating source files.

---

# 66. TESTING REQUIREMENTS

Test:

- Student login
- Operator login
- Invalid login
- Facility listing
- Resource listing
- Resource availability
- Request creation
- Pending request
- Operator pending request
- Approval
- Rejection
- Student approved status
- Digital confirmation
- QR generation
- QR verification
- Check-in
- Resource occupation
- Checkout
- Resource availability after checkout
- History
- Logout
- Unauthorized access
- Invalid state transition
- Duplicate request protection

---

# 67. ACCEPTANCE TEST

The following must pass:

Student Login
↓
Dashboard
↓
Libraries
↓
E-Library
↓
Available Resource
↓
Create Request
↓
PENDING
↓
Operator Login
↓
Pending Request Appears
↓
Approve
↓
Database Updated
↓
Student My Requests
↓
APPROVED
↓
View Digital Confirmation
↓
Real QR
↓
Operator Check-in
↓
Backend Validation
↓
CHECKED_IN
↓
Resource OCCUPIED
↓
Checkout
↓
COMPLETED
↓
Resource AVAILABLE
↓
History

If any step fails, QueueSense is NOT complete.

---

# 68. DOCUMENTATION

Generate README.md containing:

- project overview
- problem statement
- architecture
- technology stack
- folder structure
- PostgreSQL setup
- environment variables
- backend setup
- frontend setup
- database setup
- seed instructions
- demo accounts
- API overview
- workflow
- troubleshooting
- testing instructions

---

# 69. CODE QUALITY

Prefer:

- reusable components
- modular files
- DTOs
- service layer
- repository layer
- centralized API client
- centralized authentication
- reusable UI components
- environment variables
- proper exception handling
- validation

Avoid:

- giant files
- duplicated UI
- inline CSS everywhere
- hard-coded requests
- hard-coded statistics
- fake QR
- localStorage as database
- frontend-only authorization
- duplicated business logic

---

# 70. NO STATIC MOCK WORKFLOW

These are NOT acceptable:

- button only showing alert
- fake approval
- fake request stored only in localStorage
- fake QR made from CSS
- hard-coded pending request
- hard-coded approved request
- static resource availability
- frontend-only role authorization
- disconnected pages

The main workflow must use the real backend and database.

---

# 71. IMPLEMENTATION FREEDOM

The coding agent may modify:

- exact folder structure
- package names
- component names
- API naming
- database implementation details
- CSS organization

if doing so improves reliability.

However, these must remain mandatory:

- React/Vite frontend
- Spring Boot backend
- PostgreSQL database
- JPA/Hibernate
- Spring Security/JWT
- real database-backed workflow
- Student → Operator → Approval → QR → Check-in → Checkout
- consistent UI
- working acceptance test

---

# 72. IMPORTANT AGENT RULE

Do not ask the user to manually create dozens of files.

The agent should create and modify files directly.

The agent should run the application.

The agent should inspect errors.

The agent should fix errors.

The agent should retest.

Do not declare success simply because source code was generated.

---

# 73. DEFINITION OF DONE

QueueSense is complete only when:

- frontend runs
- backend runs
- PostgreSQL runs
- authentication works
- student login works
- operator login works
- resources load from database
- requests persist in database
- operator approval works
- student status updates correctly
- digital confirmation works
- real QR code is generated
- QR verification works
- check-in works
- resource becomes occupied
- checkout works
- resource becomes available
- history works
- authorization works
- invalid state transitions are rejected
- UI is consistent
- no major console errors remain
- no broken navigation remains
- no fake workflow remains
- complete demo scenario passes

Do not claim completion before testing the full workflow.