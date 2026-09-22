# QueueSense — College Reality Gap & Stakeholder Interview Framework

## 1. System Maturity Classification

| Capability Area | A. Prototype / Demo State | B. College Pilot Viable | C. Missing for Full Production |
| :--- | :--- | :--- | :--- |
| **Authentication** | Demo credentials with 1-click login buttons. | BCrypt + JWT authentication with unique student email. | University LDAP / SAML 2.0 / Microsoft Entra SSO integration. |
| **Facility Scope** | 3 Hardcoded facilities (E-Library, Reading Room, Sports Complex). | Real DB facilities and resource inventories. | Dynamic department, block, and floor mapping. |
| **Operator Scope** | Operator views and approves all facility queues globally. | Operator assigned to facility in profile. | Strict multi-facility authorization boundaries & shift rotations. |
| **Resource Allocation** | Immediate booking with 1–4 hour duration. | 10-minute hold grace period. | Scheduled timeslots, calendar recurrence, peak-hour quotas. |
| **Check-in Verification**| HTML5 camera scanner + manual code input fallback. | Real-time QR validation against database. | Turnstile / barcode hardware integration, NFC RFID card readers. |
| **No-Show Handling** | On-demand expiration when scanned after deadline. | Configurable grace period property in YAML. | Automated background expiration sweeper & student penalty strikes. |
| **Notifications** | Static DB notification table. | In-app notification UI polling. | Push notifications, SMS gateway, or University WhatsApp/Email alerts. |

---

## 2. University Stakeholder Discovery Questionnaire

To bridge the gap between our engineering prototype and the operational reality of the university campus, the following questions must be reviewed with campus authorities:

### 1. Questions for the Chief Librarian
1. **Seat & Desk Inventory**: Are library seats assigned strict numbers on desks, or are they unnumbered open tables?
2. **PC Station Management**: Do computer lab workstations require automatic OS lock/unlock integration upon QR check-in?
3. **Session Duration & Extensions**: Can a student extend their 2-hour booking if no one is waiting in the queue?
4. **No-Show Policy**: What happens if a student reserves a desk during exam week and doesn't show up? Should their booking privileges be suspended after 3 strikes?
5. **Quiet Zone vs Discussion Pods**: Are there distinct noise zones with different maximum group size limits?

### 2. Questions for the Reading Room Manager
1. **Operating Hours & Night Access**: Is the reading room open 24/7 or does it close at specific hours? How should the system handle overnight study sessions?
2. **Belongings & Short Breaks**: Can a student leave their desk for lunch (e.g. a 20-minute "Step Out" hold) without triggering an automatic checkout?
3. **Walk-in Handling**: If a student arrives without a smartphone, how does the staff member assign an available seat?

### 3. Questions for the Sports Facility & Complex In-Charge
1. **Slot-Based Scheduling**: Unlike library seats, are sports courts (Badminton, Tennis, Basketball) booked by fixed 45-minute or 1-hour slots?
2. **Equipment Lending**: Does booking a badminton court also require tracking racket and shuttlecock issuance?
3. **Team/Group Verification**: Does every team member need to scan their student ID card, or is only the team captain responsible?
4. **Weather & Maintenance Closures**: How should operators broadcast rain/maintenance emergency closures to all active bookings?

### 4. Questions for College Administration & Dean of Student Affairs
1. **Booking Quotas**: What is the maximum daily or weekly usage allowance per student across campus facilities?
2. **Department Priority**: Do final-year students or postgraduate researchers have priority reservation windows for digital labs?
3. **Disciplinary Actions**: How are student penalties, noise warnings, or property damage tied to the booking audit log?

### 5. Questions for Campus IT & Network Administrator
1. **Identity Provider**: Does the university use Microsoft 365, Google Workspace for Education, or an on-premise LDAP server for student logins?
2. **Network Topology**: Will the operator check-in tablets run on the campus internal intranet or require public internet access?
3. **Database Hosting**: Will PostgreSQL run on a dedicated campus VM server or a cloud instance?
4. **Data Retention**: How long must usage session logs and student history records be retained for campus compliance audits?
