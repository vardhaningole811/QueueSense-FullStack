# QueueSense — Team Handoff & Development Guide

## 1. Project Overview

QueueSense is a smart campus facility availability, booking, approval and resource-management system.

The initial implementation focuses on:

- Library / E-Library resources
- Sports resources such as badminton
- Student booking
- Facility operator approval
- Digital confirmation pass
- QR-based check-in
- Check-out
- Booking history
- Resource availability
- Operator dashboard
- Audit logging

The system is intended to eventually become a realistic deployable campus application rather than only a static college prototype.

---

# 2. Current Architecture

QueueSense is a full-stack application.

Frontend:
React + Vite

Backend:
Java + Spring Boot

Database:
PostgreSQL

Authentication:
JWT + BCrypt

Persistence:
Spring Data JPA / Hibernate

Communication:
HTTP REST APIs

High-level flow:

Browser
↓
React frontend
↓
Axios / HTTP request
↓
Spring Boot REST API
↓
Spring Security / JWT
↓
Controller
↓
Service
↓
Repository
↓
JPA / Hibernate
↓
PostgreSQL

---

# 3. Current Working Features

## Student

- Login
- View facilities
- View resources
- View resource availability
- Submit usage request
- Prevent duplicate requests
- View request status
- Receive approved digital pass
- Display QR code
- Check-in
- Check-out
- View history

## Operator

- Login
- Dashboard
- View requests
- Approve request
- Reject request
- Reject with reason
- View resources
- Change resource status
- QR scanning
- Manual QR/code verification
- View sessions/history

## Backend

- REST APIs
- Authentication
- Authorization
- JWT authentication
- BCrypt password hashing
- Request lifecycle
- Resource lifecycle
- Database persistence
- Audit logs
- Exception handling

## Database

PostgreSQL stores the persistent application data.

There are currently approximately 10 relational tables covering:

- users
- student profiles
- operator profiles
- facilities
- resources
- usage requests
- usage sessions
- check-ins
- notifications
- audit logs

Exact schema should be checked in:

docs/DATABASE_AUDIT.md

---

# 4. Important Current State

The project has already been initialized as a Git repository.

Remote:

https://github.com/vardhaningole811/QueueSense-FullStack

Main branch:

main

Current baseline commit:

chore: initial QueueSense full-stack baseline

DO NOT rewrite or delete main history without discussing it with the team.

---

# 5. How To Run The Project

## Terminal 1 — Database

From project root:

.\start-db.ps1

This starts the local PostgreSQL instance used by QueueSense.

---

## Terminal 2 — Backend

From project root:

.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml spring-boot:run

Backend:

http://localhost:8080

---

## Terminal 3 — Frontend

From project root:

cd frontend

npm run dev

Frontend:

http://localhost:5173

---

# 6. Demo Accounts

Student:

student@queuesense.demo

Password:

Student@123

Operator:

operator@queuesense.demo

Password:

Operator@123

These are development/demo credentials only.

---

# 7. Important Known Problems

These must NOT be forgotten.

## H-01 — Request ownership vulnerability

A student must not be able to access another student's request/pass simply by changing an ID.

Example problem:

GET /api/requests/{id}

The backend must verify that the authenticated student owns the requested resource.

Never trust an ID coming from the frontend.

---

## H-02 — Operator facility isolation

Operators should only manage facilities/resources assigned to them.

Currently the implementation can expose broader facility/request access than intended.

Need proper authorization boundaries.

---

## L-01 — Reservation expiration

The current system has expiration logic, but expiration can behave lazily.

A reserved resource should automatically become AVAILABLE after its reservation expires.

A background scheduled job should eventually handle this automatically.

---

## M-02 — Security hardening

JWT/database configuration must use environment variables rather than hardcoded production secrets.

Rate limiting should eventually be added to authentication endpoints.

---

## C-01 — Digital pass printing

Print output needs improvement so navigation/buttons do not appear on the printed digital pass.

---

# 8. Features Still Required

Priority order:

## P0 — Must fix before calling the implementation production-ready

1. Student request ownership authorization
2. Operator facility authorization
3. Reservation expiration worker
4. Proper cancellation workflow
5. Correct resource/request state transitions
6. Security configuration cleanup
7. Strong backend validation
8. Integration testing of complete workflows

---

## P1 — Important campus features

1. Daily booking limits
2. No-show tracking
3. Warning/strike system
4. Temporary suspension
5. Early checkout
6. Automatic resource release
7. Notifications
8. Better operator assignment
9. Better audit trail
10. Booking conflict prevention

---

## P2 — Advanced features

1. University SSO / LDAP
2. ID-card integration
3. Analytics dashboard
4. Usage prediction
5. Facility utilization reports
6. Admin dashboard
7. Multi-facility management
8. Deployment to cloud infrastructure

---

# 9. Git Rules For The Team

DO NOT directly work on main.

Each developer should create their own branch.

Example:

git checkout main

git pull origin main

git checkout -b feature/student-cancellation

Then work.

After completing the feature:

git status

git add .

git commit -m "feat: add student request cancellation"

git push -u origin feature/student-cancellation

Then create a Pull Request on GitHub.

Another teammate reviews it.

Only then should it be merged into main.

---

# 10. Important Git Rule

Before starting work every day:

git checkout main

git pull origin main

git checkout your-branch

git merge main

This reduces the chance of working against an outdated codebase.

---

# 11. Do Not Commit

Never commit:

- .env
- passwords
- API keys
- JWT production secrets
- database dumps
- node_modules
- target/
- pgsql/
- tools/
- personal IDE configuration

The repository already contains .gitignore rules for most of these.

---

# 12. Team Roles

Suggested initial ownership:

Vardhan:
- Overall integration
- Architecture understanding
- Git/GitHub
- Backend coordination
- Final integration/testing

Uday:
- Backend / API work
- Request lifecycle
- Security authorization

Mansi:
- Frontend/UI
- Student workflow
- UX improvements

Aaryan:
- Database
- Testing
- Documentation
- Reports/analytics

These roles can change. Nobody should modify another person's feature branch without coordination.

---

# 13. How The System Actually Works

Example:

Student clicks "Book".

React handles the click.

↓

React sends an HTTP POST request.

↓

Axios sends the request to Spring Boot.

↓

Spring Security checks the JWT.

↓

Controller receives the request.

↓

Controller calls the Service.

↓

Service validates:

- student identity
- resource existence
- availability
- duplicate requests
- booking rules

↓

Repository communicates with PostgreSQL through JPA/Hibernate.

↓

Database stores the request.

↓

Backend returns JSON.

↓

React receives the response.

↓

React updates the UI.

The important concept:

The frontend should NEVER be responsible for enforcing security.

The backend/database are authoritative.

---

# 14. What New Team Members Must Learn

Before modifying code, understand:

1. HTTP
2. REST API
3. JSON
4. React components
5. React state
6. Props
7. Axios
8. Java Spring Boot
9. Controllers
10. Services
11. Repositories
12. JPA
13. Hibernate
14. PostgreSQL
15. SQL
16. Foreign keys
17. Transactions
18. JWT
19. BCrypt
20. RBAC
21. Git branches
22. Pull Requests

The detailed beginner explanation is in:

docs/CURRENT_SYSTEM_MAP.md

and

docs/QUEUE_SENSE_STUDENT_GUIDE.md

---

# 15. Golden Rule

Do not add features blindly.

For every feature answer:

1. What problem does it solve?
2. What user performs the action?
3. Which frontend page triggers it?
4. Which API endpoint receives it?
5. Which controller handles it?
6. Which service contains the business logic?
7. Which repository accesses the database?
8. Which database tables change?
9. What happens if the operation fails?
10. How is authorization enforced?

If these questions cannot be answered, the feature is not understood well enough yet.