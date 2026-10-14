# Rural Community Support Platform
> **Connecting rural communities with the right help.**  
> A modular Java and MySQL platform that turns local complaints into verified, trackable NGO action.

---

## 📌 1. Project Overview

In many rural regions, critical infrastructure problems—such as drinking water contamination, broken handpumps, sanitation failures, primary health clinic shortages, school damages, and agricultural waste burning—often go unresolved because:
1. Complaints are reported through disconnected, unverified channels.
2. Communities struggle to identify which specialized NGO operates in their area.
3. NGOs struggle to find vetted problems matched to their specific capacity.
4. Without systematic verification and tracking, urgent requests disappear between reporting and response.

The **Rural Community Support Platform** bridges this divide by providing an end-to-end transparent workflow:

```
[Community Member] ───(1. Report Issue)───► [District Admin] ───(2. Verification)
                                                                       │
                                                            (3. Matching Engine:
                                                             Need + Place + Capacity)
                                                                       ▼
[Community Member] ◄───(4. Status Tracking)─── [Specialized NGO] ◄─── (Allocation)
```

---

## 👥 2. Stakeholder Roles & OOP Domain Model

The platform implements pure Object-Oriented Programming (OOP) principles with inheritance, encapsulation, and polymorphism across three primary roles:

| Role | Domain Class | Responsibilities | Default Demo Login |
| :--- | :--- | :--- | :--- |
| **District Admin** | `Admin` (extends `User`) | Verifies reported issues, executes the matching & allocation engine, manages NGO registries, and monitors analytical dashboards. | configured administrator credentials |
| **NGO Organization** | `NGO` (extends `User`) | Manages specializations, service villages, and caseload capacity; receives allocations, and transitions work progress from `IN_PROGRESS` to `RESOLVED`. | credentials configured for the NGO account |
| **Community Member** | `CommunityMember` (extends `User`) | Reports local problems with category, village, urgency, and description; tracks the real-time status of submitted complaints. | `ramesh_k` / `user123` |

### Core Domain Entities
- **`User`**: Base class containing user credentials, contact details, timestamps, and role-based guards.
- **`Issue`**: Encapsulates complaint details, category, urgency, calculated rule-based priority score, and current lifecycle state.
- **`Assignment`**: Connects verified issues with allocated NGOs and tracks transactional state.
- **`StatusHistory`**: Complete audit trail recording who made each status transition, timestamps, and progress notes.
- **`MatchRecommendation`**: DTO containing algorithmic scoring, breakdown factors, and administrative justification.
- **`Report`**: Aggregated analytics object containing category breakdowns, area metrics, NGO performance rates, and hotspot priority alerts.

---

## 🧩 3. Six Modular Areas

1. **User Authentication & Role-Based Access Control (RBAC)**: Secure SHA-256 password hashing, session context, and role-segregated UI panels.
2. **Issue Reporting**: Categorized input (`CLEAN_WATER`, `SANITATION`, `HEALTHCARE`, `EDUCATION`, `WASTE_MANAGEMENT`), village sector, urgency selector, photo attachment link, and real-time priority scoring.
3. **NGO Management**: Capacity controls (`active_cases` vs `max_capacity`), multi-specialization tagging, and regional village coverage maps.
4. **Intelligent Matching & Allocation Engine**: Automated scoring algorithm recommending best-fit NGOs based on Need, Place, and Bandwidth while keeping the Administrator in control.
5. **Work-Status Tracking**: Transparent lifecycle transitions:  
   `PENDING_VERIFICATION` ➔ `VERIFIED` ➔ `ASSIGNED` ➔ `IN_PROGRESS` ➔ `RESOLVED` ➔ `CLOSED`.
6. **Dashboards, Reports & Rule-Based Priority Prediction**: SQL analytical aggregations (`JOIN`, `COUNT`, `GROUP BY`, `ORDER BY`), NGO scorecard, and hotspot recurrence alerts.

---

## ⚙️ 4. Rule-Based Matching & Priority Prediction Engine

### A. NGO Matching Formula (Slide 6)
For any verified issue, registered NGOs are evaluated and scored (0–100%):
- **Need Match (+45 pts)**: NGO holds an active specialization in the issue category.
- **Place Match (+35 pts)**: NGO covers the reported village/area.
- **Capacity Availability (+20 pts)**: NGO has at least 1 free slot (`active_cases < max_capacity`).
- **Capacity Buffer Bonus (+10 pts)**: NGO has $\ge 3$ available slots for fast turnaround.
- **Overload Penalty (-30 pts)**: Penalizes allocating critical emergency complaints to full NGOs.

### B. Rule-Based Priority Prediction (Slide 9 & 10)
- **Base Urgency Weight**: `LOW` = 20 pts, `MEDIUM` = 40 pts, `HIGH` = 70 pts, `CRITICAL` = 90 pts.
- **Recurrence Cluster Detection**: Queries active unresolved complaints in the same village and category. Each recurring unresolved problem adds +10 points to the priority score.
- **Priority Alerts**: The analytics engine automatically surfaces villages with $\ge 2$ active issues or any `CRITICAL` issue as emergency hotspots.

---

## 🗄️ 5. Database Schema (MySQL)

The database schema (`sql/schema.sql`) enforces relational integrity with foreign keys, cascading constraints, and views:

- `users`: ID, username, SHA-256 hash, full name, phone, email, role, village, created_at.
- `ngos`: ID, user_id (FK), organization_name, contact_person, phone, email, max_capacity, description.
- `ngo_specializations`: ngo_id (FK), category (`CLEAN_WATER`, `SANITATION`, `HEALTHCARE`, etc.).
- `ngo_service_areas`: ngo_id (FK), village_area (e.g., 'Rampur', 'Shivpur', 'Belgaon').
- `issues`: ID, reporter_id (FK), title, category, village_area, urgency, description, photo_path, status, priority_score, created_at, updated_at.
- `assignments`: ID, issue_id (FK), ngo_id (FK), assigned_by (FK), assigned_at, status (`ACTIVE`, `COMPLETED`), notes.
- `status_history`: ID, issue_id (FK), changed_by (FK), previous_status, new_status, comments, changed_at.
- `v_ngo_caseload`: SQL View computing live active and completed caseloads per NGO.

---

## 🚀 6. Quick Start & Setup

### Separate login portals
When the website opens, the homepage offers two separate choices:
- **User / NGO Login** — sign in or register as a community member or NGO.
- **Admin Login** — sign in to the administrator dashboard using an ADMIN account.

Admin sign-in uses a separate API endpoint and creates a server-side session token. Admin dashboard APIs (verification, rejection, closure, matching, allocation, NGO management, analytics, and the administrator issue list) reject requests without a valid admin token. Logging out invalidates that token. Admin accounts cannot sign in through the regular User / NGO login form.

### Step 1: Initialize Database
Run the automated batch script or execute through MySQL CLI:
```bat
setup-db.bat
```
*Or manually via MySQL Command Line:*
```sql
mysql -u root -p < sql/schema.sql
```

### Step 2: Configure Database Credentials
Edit `db.properties` in the project root if your MySQL password differs:
```properties
db.url=jdbc:mysql://localhost:3306/rural_support_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_PASSWORD
```
*(You can also configure and test this directly inside the GUI using the "DB Settings" button!)*

### Step 3: Compile the Project
Double-click `compile.bat` or run:
```bat
compile.bat
```

### Step 4: Run the Application
- **To launch the Web Portal (Browser UI):**
  ```bat
  run-web.bat
  ```
  *Opens automatically at `http://localhost:8080`!*
- **To launch the Graphical Desktop UI (Swing):**
  ```bat
  run-gui.bat
  ```
- **To launch the interactive Command-Line Interface (CLI):**
  ```bat
  run-cli.bat
  ```
- **To build or import with Maven:**
  Open `pom.xml` in IntelliJ IDEA, Eclipse, or VS Code, or run:
  ```bash
  mvn clean package
  ```

---

## 🧪 7. Walkthrough & Test Demonstration

1. **Sign In as Admin**:
   - On the homepage, select **Admin Login**.
   - Sign in with an administrator account configured in your database. No admin credentials are displayed on the website.
   - Open **Verification Queue** tab: Review pending issues reported by villagers.
   - Click **Verify Issue** on issue #1 ("Main village handpump non-functional for 5 days").
2. **Run Intelligent NGO Matching**:
   - Open **Matching & Allocation** tab: Select the verified handpump issue.
   - Notice the system automatically ranks **Jal Seva Mission** at **100% Score** (Matches Clean Water category, covers Rampur, and has free capacity).
   - Click **Allocate Issue to Recommended NGO**.
3. **Switch to NGO Role**:
   - Click **Log Out** in the top bar to invalidate the admin session and return to the portal selector.
   - Log in with an authorized NGO account.
   - Notice the newly assigned handpump issue appears in your field tasks.
   - Click **Start Work (In Progress)** and then **Mark Resolved** with completion remarks.
4. **Switch to Community Member**:
   - Select **User / NGO Login**, then log in as **Ramesh Kumar** (`ramesh_k` / `user123`).
   - View **My Submitted Issues & Progress Tracker** and click **View Timeline / Status** to see the complete audit trail.
5. **View Analytical Dashboards**:
   - Return to Admin: Open **Dashboards & Analytics** tab to inspect live category distributions, NGO completion performance rates, and priority alerts.

---

## 🔮 8. Future Roadmap

The platform architecture leaves a clean path for future extensions (Slide 10):
- **Machine Learning Priority Prediction**: Training a predictive model on historical resolution turnaround times, seasonal water level variances, and geographic clustering.
- **SMS / USSD Gateway**: Enabling feature phone users in remote areas without internet to report issues via offline SMS gateways.

