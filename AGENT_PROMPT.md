# QueueSense Full-Stack Build Agent Instructions

You are the primary software engineering agent responsible for building the QueueSense full-stack application.

The complete project specification is in:

PRD.md

Read PRD.md completely before making implementation decisions.

Do not ignore requirements because the project is a prototype.

The objective is not to generate many source files.

The objective is to produce a working, tested, coherent application.

---

# 1. NON-NEGOTIABLE TECHNOLOGY STACK

Use exactly this architecture unless a technical limitation makes an equivalent implementation absolutely necessary:

Frontend:
- React
- Vite
- React Router
- Axios or Fetch
- reusable React components

Backend:
- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- Bean Validation

Database:
- PostgreSQL

Build:
- npm
- Maven

QR:
- real QR-code library

DO NOT use:

- Node.js backend
- Express
- Prisma
- SQLite
- static HTML-only architecture
- fake backend
- localStorage as the database

---

# 2. FIRST ACTION — INSPECT THE WORKSPACE

Before creating anything:

1. Inspect the entire workspace.
2. Check whether files already exist.
3. Check whether another QueueSense implementation exists.
4. Determine whether the folder is empty or partially initialized.
5. Do not blindly overwrite useful files.
6. If an old prototype exists, do not let its architecture contaminate the new implementation.

The previous QueueSense prototype used independent HTML pages and localStorage.

That architecture is NOT the target architecture.

The new application must be a proper React + Spring Boot + PostgreSQL application.

---

# 3. READ THE SPECIFICATION

Read:

PRD.md

before implementation.

Treat it as the primary product specification.

If there is a conflict between an old source file and PRD.md, prioritize PRD.md.

---

# 4. BUILD THE PROJECT AS ONE APPLICATION

Do not think of the application as:

login.html

dashboard.html

operator.html

request-status.html

confirmation.html

Instead think of it as:

One QueueSense application

with:

React frontend

+

Spring Boot backend

+

PostgreSQL database

+

REST API

All pages/routes must use the same application state.

---

# 5. CREATE PROJECT STRUCTURE

Create something similar to:

QueueSense-FullStack/

├── PRD.md
├── AGENT_PROMPT.md
├── README.md
├── .gitignore
│
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│
└── backend/
    ├── pom.xml
    └── src/

The exact structure may be improved if required.

---

# 6. BACKEND FIRST

Build the Spring Boot backend first.

Implement:

- entities
- enums
- repositories
- DTOs
- services
- controllers
- security
- JWT
- validation
- exception handling
- database configuration
- seed data

Do not place all business logic inside controllers.

---

# 7. DATABASE

Use PostgreSQL.

Create the required entities and relationships.

At minimum:

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

Use JPA/Hibernate.

Do not use browser storage as a database.

---

# 8. DATABASE SOURCE OF TRUTH

The following must come from PostgreSQL:

- users
- roles
- facilities
- resources
- resource states
- requests
- approvals
- rejections
- check-ins
- checkouts
- history
- statistics
- audit logs

The frontend must request current state from backend APIs.

---

# 9. AUTHENTICATION

Implement:

Student login

Operator login

Admin-ready architecture

Use:

Spring Security

JWT

Password hashing

The backend must determine the authenticated role.

Never trust:

localStorage.role

URL role

hidden UI

query parameters

for authorization.

---

# 10. DEMO ACCOUNTS

Seed:

Student:

student@queuesense.demo

Operator:

operator@queuesense.demo

Create a documented demo password.

Document the credentials in README.md.

Do not hard-code credentials inside React source code.

---

# 11. REQUEST WORKFLOW

Implement this first before polishing the UI:

Student login

↓

Dashboard

↓

Libraries

↓

E-Library

↓

Seat 03

↓

Create request

↓

PENDING

↓

Operator login

↓

Pending request appears

↓

Approve

↓

APPROVED

↓

Digital confirmation

↓

QR

↓

Check-in

↓

CHECKED_IN

↓

Checkout

↓

COMPLETED

↓

Resource AVAILABLE

↓

History

Do not move on to unnecessary features before this works.

---

# 12. REQUEST CREATION

When student creates a request:

Frontend:

POST /api/requests

Backend:

1. Authenticate student.
2. Validate fields.
3. Validate resource.
4. Validate facility.
5. Check resource state.
6. Check duplicate requests.
7. Generate request code.
8. Create request.
9. Save to database.
10. Return request.

Frontend:

Show PENDING request.

---

# 13. OPERATOR APPROVAL

When operator clicks Approve:

Frontend:

POST /api/operator/requests/{id}/approve

Backend:

1. Verify JWT.
2. Verify OPERATOR role.
3. Verify operator access.
4. Load request.
5. Verify request is PENDING.
6. Verify resource can be reserved.
7. Use transaction.
8. Change request to APPROVED.
9. Change resource to RESERVED.
10. Store approval timestamp.
11. Create audit log.
12. Return updated request.

Frontend:

Remove from pending.

Student:

Sees APPROVED.

---

# 14. IMPORTANT — NO LOCALSTORAGE SYNCHRONIZATION

Do not attempt to make approval work like:

Operator changes localStorage

then

Student reads localStorage.

That was part of the old prototype problem.

Instead:

Operator

→ Backend API

→ PostgreSQL

→ Student API request

→ Updated state

This is mandatory.

---

# 15. REQUEST STATE MACHINE

Implement backend protection.

Allowed:

PENDING → APPROVED

PENDING → REJECTED

PENDING → CANCELLED

APPROVED → CHECKED_IN

APPROVED → EXPIRED

CHECKED_IN → COMPLETED

Reject invalid transitions.

Do not allow frontend to directly set statuses.

---

# 16. RESOURCE STATE

Implement:

AVAILABLE

RESERVED

OCCUPIED

MAINTENANCE

When request approved:

resource → RESERVED

When check-in succeeds:

resource → OCCUPIED

When checkout succeeds:

resource → AVAILABLE

These changes must happen through backend logic.

---

# 17. DIGITAL CONFIRMATION

Only display confirmation for valid approved requests.

Page should include:

QueueSense

Request Approved

Request ID

Student ID

Facility

Resource

Expected arrival

Expected duration

Real QR

Proceed to Check-in

If a request is not approved, show an appropriate state instead of a blank page.

---

# 18. REAL QR CODE

Use an actual QR library.

Do not make a CSS fake QR.

The QR should encode:

requestCode

or

short-lived check-in token.

Do not encode:

password

JWT

unnecessary student personal data

---

# 19. QR CHECK-IN

Operator check-in page should support:

Scan QR

OR

Enter Request Code

The two methods must use the same backend validation logic.

Backend must verify:

- request exists
- request is APPROVED
- request has not expired
- request is not cancelled
- request has not already been checked in
- operator is authorized

Then:

request → CHECKED_IN

resource → OCCUPIED

create check-in

create usage session

create audit log

---

# 20. CHECKOUT

Implement:

POST /api/requests/{id}/check-out

Backend verifies:

status == CHECKED_IN

Then:

request → COMPLETED

resource → AVAILABLE

check-out timestamp stored

usage session closed

audit log created

Frontend refreshes state.

---

# 21. HISTORY

History must come from backend.

Do not fake history data in React.

After checkout, the completed usage must appear automatically.

---

# 22. OPERATOR DASHBOARD

Build dashboard statistics from database.

At minimum:

Pending Requests

Available Resources

Reserved Resources

Currently Occupied

Today's Check-ins

Do not use:

const pending = 3;

const available = 12;

etc.

Statistics must be calculated using real backend queries.

---

# 23. FRONTEND ARCHITECTURE

Create reusable components.

Examples:

AppLayout

Sidebar

Topbar

Button

StatusBadge

StatCard

ResourceCard

RequestCard

RequestDetails

EmptyState

LoadingState

Modal

QRCodeDisplay

Toast

Do not duplicate entire page layouts.

---

# 24. CONSISTENT UI

The old QueueSense prototype had different visual styles on different pages.

Do NOT repeat that.

Create one design system.

Every page must use:

same sidebar

same typography

same buttons

same spacing

same cards

same colors

same status badges

same navigation

same QueueSense branding

---

# 25. VISUAL STYLE

The UI must NOT look like a generic AI-generated SaaS dashboard.

Avoid:

- excessive gradients
- glassmorphism
- huge hero sections
- giant illustrations
- unnecessary charts
- fake analytics
- excessive rounded cards
- random icon collections
- excessive animations
- marketing-heavy text

Aim for:

professional university software

clean

credible

practical

modern

campus-oriented

The application should look like something a university IT team could realistically deploy.

---

# 26. DESIGN SYSTEM

Use:

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

Use accent sparingly.

---

# 27. UI PRIORITY

Prioritize:

1. usability
2. information hierarchy
3. consistency
4. readability
5. workflow clarity
6. visual polish

Do not sacrifice functionality for visual effects.

---

# 28. STUDENT EXPERIENCE

Student should be able to understand immediately:

Where am I?

What resources are available?

Do I have an active request?

What is its status?

What should I do next?

For example:

PENDING

"Waiting for facility operator approval."

APPROVED

"Your request is approved."

[View Digital Confirmation]

CHECKED_IN

"Your session is active."

[Check Out]

COMPLETED

"Usage completed."

---

# 29. OPERATOR EXPERIENCE

Operator should immediately see:

Pending requests

Available resources

Reserved resources

Occupied resources

Today's activity

The operator should not need to navigate through unnecessary screens to approve a request.

---

# 30. ERROR HANDLING

Never leave blank screens.

Handle:

loading

empty state

network error

unauthorized

forbidden

invalid request

resource unavailable

expired request

already checked-in

already completed

server error

Show useful messages.

---

# 31. RESPONSIVE DESIGN

Support:

desktop

laptop

tablet

mobile

Desktop is primary for the demonstration.

---

# 32. API CLIENT

Create one centralized frontend API client.

Do not scatter raw fetch calls throughout every component.

Centralize:

base URL

JWT handling

error handling

request helpers

---

# 33. ENVIRONMENT VARIABLES

Use environment variables for:

database connection

JWT secret

backend URL

frontend API URL

Do not hard-code secrets.

Create appropriate example environment files.

---

# 34. CORS

Configure backend CORS correctly for the local Vite development server.

Do not disable security broadly just to make the frontend work.

---

# 35. VALIDATION

Use backend validation.

Validate:

expected arrival

expected duration

group size

resource ID

request state

authentication

authorization

Do not rely only on frontend validation.

---

# 36. TRANSACTIONS

Use @Transactional where operations modify multiple related records.

Especially:

approval

check-in

checkout

resource state transitions

The request and resource state should not become inconsistent.

---

# 37. CONCURRENCY

Protect against:

Student A reserves Seat 03

Student B simultaneously reserves Seat 03

Only one request should succeed.

Use appropriate transactional logic/database constraints.

---

# 38. SEED DATA

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

Use different initial statuses.

---

# 39. TESTING

Write backend tests for important services.

At minimum test:

login

request creation

approval

rejection

invalid state transition

check-in

checkout

duplicate request

unauthorized access

resource conflict

---

# 40. BROWSER TESTING

After implementation, actually run:

backend

frontend

database

Then test the browser workflow.

Do not simply inspect source code and assume it works.

---

# 41. ACCEPTANCE TEST

Run this exact test:

1. Student login.
2. Open dashboard.
3. Open Libraries.
4. Open E-Library.
5. Select available Seat 03.
6. Submit request.
7. Confirm PENDING.
8. Open operator portal.
9. Confirm request appears.
10. Approve.
11. Confirm request disappears from pending.
12. Confirm resource becomes RESERVED.
13. Open student My Requests.
14. Confirm APPROVED.
15. Click View Digital Confirmation.
16. Confirm QR exists.
17. Open operator check-in.
18. Scan QR or enter request code.
19. Confirm validation succeeds.
20. Confirm CHECKED_IN.
21. Confirm resource becomes OCCUPIED.
22. Checkout.
23. Confirm COMPLETED.
24. Confirm resource becomes AVAILABLE.
25. Open history.
26. Confirm completed usage exists.

If any step fails:

DO NOT claim the project is complete.

---

# 42. DEBUGGING LOOP

When something fails:

1. Reproduce.
2. Identify whether failure is frontend, backend, database, authentication, or API integration.
3. Inspect console.
4. Inspect backend logs.
5. Inspect network request.
6. Inspect database state.
7. Fix root cause.
8. Restart if required.
9. Re-test.
10. Continue until the workflow works.

Do not patch symptoms with random alerts or hard-coded values.

---

# 43. IMPORTANT RULE ABOUT OLD FILES

If the workspace contains old HTML files such as:

index.html

student.html

operator.html

request-status.html

confirmation.html

libraries.html

etc.

do not attempt to keep them as the primary architecture.

The new React application should replace the old static-page workflow.

You may use the old files only as reference for requirements or visual ideas.

---

# 44. DO NOT OVERENGINEER

This is a competition-quality academic prototype.

Do not spend excessive time building:

- microservices
- Kubernetes
- complicated cloud infrastructure
- production-scale distributed systems
- unnecessary AI
- unnecessary analytics

Focus on:

working workflow

security basics

database correctness

clean architecture

good UI

reliable demonstration

---

# 45. DO NOT UNDERENGINEER

At the same time, do not create:

- static mock data
- fake approval
- fake QR
- fake operator dashboard
- fake history
- disconnected pages

The application must be genuinely functional.

---

# 46. PRIORITY ORDER

If time becomes limited:

Priority 1:
Authentication

Priority 2:
Database

Priority 3:
Resource availability

Priority 4:
Request creation

Priority 5:
Operator approval

Priority 6:
Digital confirmation

Priority 7:
QR

Priority 8:
Check-in

Priority 9:
Checkout

Priority 10:
History

Priority 11:
UI polish

Do not spend hours polishing a page that is not connected to the backend.

---

# 47. README

Create a complete README.md containing:

Project overview

Features

Architecture

Technology stack

Requirements

PostgreSQL setup

Environment variables

Backend setup

Frontend setup

Database setup

Seed data

Demo credentials

How to run

API overview

Demo workflow

Testing

Troubleshooting

---

# 48. FINAL QUALITY CHECK

Before saying "done", verify:

Frontend runs.

Backend runs.

Database connects.

Student login works.

Operator login works.

JWT works.

Role authorization works.

Facilities load from DB.

Resources load from DB.

Request creation works.

Request persists.

Operator sees request.

Approval works.

Student sees approval.

Digital confirmation works.

Real QR exists.

QR validation works.

Check-in works.

Resource becomes occupied.

Checkout works.

Resource becomes available.

History works.

Invalid states are blocked.

Duplicate requests are blocked.

Unauthorized requests are blocked.

No major console errors exist.

No broken routes exist.

No fake workflow exists.

---

# 49. FINAL RESPONSE TO USER

When implementation is actually complete, report:

1. What was built.
2. Frontend URL.
3. Backend URL.
4. Database status.
5. Demo accounts.
6. Main workflow tested.
7. Any remaining limitations.

Do NOT say "complete" if the acceptance workflow has not been tested.
