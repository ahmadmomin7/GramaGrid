-- ============================================================================
-- Rural Community Support Platform - Database Schema
-- MySQL 8.0+ Compatible
-- ============================================================================

DROP DATABASE IF EXISTS rural_support_db;
CREATE DATABASE rural_support_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rural_support_db;

-- ----------------------------------------------------------------------------
-- 1. Table: users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    role ENUM('ADMIN', 'NGO', 'COMMUNITY_MEMBER') NOT NULL,
    village_area VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. Table: ngos
-- ----------------------------------------------------------------------------
CREATE TABLE ngos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    organization_name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    max_capacity INT NOT NULL DEFAULT 5, -- max concurrent assigned issues
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ngo_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 3. Table: ngo_specializations
-- Categories: CLEAN_WATER, SANITATION, HEALTHCARE, EDUCATION, WASTE_MANAGEMENT
-- ----------------------------------------------------------------------------
CREATE TABLE ngo_specializations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ngo_id INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    CONSTRAINT fk_spec_ngo FOREIGN KEY (ngo_id) REFERENCES ngos(id) ON DELETE CASCADE,
    CONSTRAINT uq_ngo_category UNIQUE (ngo_id, category)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. Table: ngo_service_areas
-- Villages or regional sectors covered
-- ----------------------------------------------------------------------------
CREATE TABLE ngo_service_areas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ngo_id INT NOT NULL,
    village_area VARCHAR(100) NOT NULL,
    CONSTRAINT fk_area_ngo FOREIGN KEY (ngo_id) REFERENCES ngos(id) ON DELETE CASCADE,
    CONSTRAINT uq_ngo_area UNIQUE (ngo_id, village_area)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 5. Table: issues
-- ----------------------------------------------------------------------------
CREATE TABLE issues (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reporter_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    village_area VARCHAR(100) NOT NULL,
    urgency ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    description TEXT NOT NULL,
    photo_path VARCHAR(255),
    status ENUM('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'PENDING_VERIFICATION',
    priority_score INT NOT NULL DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. Table: assignments
-- ----------------------------------------------------------------------------
CREATE TABLE assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    issue_id INT NOT NULL,
    ngo_id INT NOT NULL,
    assigned_by INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    CONSTRAINT fk_assign_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_assign_ngo FOREIGN KEY (ngo_id) REFERENCES ngos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assign_admin FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 7. Table: status_history
-- ----------------------------------------------------------------------------
CREATE TABLE status_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    issue_id INT NOT NULL,
    changed_by INT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    comments TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_hist_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 8. Indexes for performance
-- ----------------------------------------------------------------------------
CREATE INDEX idx_issues_status ON issues(status);
CREATE INDEX idx_issues_category ON issues(category);
CREATE INDEX idx_issues_village ON issues(village_area);
CREATE INDEX idx_issues_urgency ON issues(urgency);
CREATE INDEX idx_assignments_ngo ON assignments(ngo_id, status);

-- ----------------------------------------------------------------------------
-- 9. Views for reporting and dashboards
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ngo_caseload AS
SELECT 
    n.id AS ngo_id,
    n.organization_name,
    n.max_capacity,
    COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) AS active_cases,
    COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_cases,
    (n.max_capacity - COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END)) AS remaining_capacity
FROM ngos n
LEFT JOIN assignments a ON n.id = a.ngo_id
GROUP BY n.id, n.organization_name, n.max_capacity;

-- ----------------------------------------------------------------------------
-- 10. Initial Seed Data
-- Passwords are hashed with SHA-256:
-- 'admin123' -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- 'ngo123'   -> 060fe28178c35c127d8382fc7f8d9a30946251ec8b6d11793833f3d1c081c625
-- 'user123'  -> e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446
-- ----------------------------------------------------------------------------

-- Users: Admin, NGOs, Community Members
INSERT INTO users (id, username, password_hash, full_name, phone, email, role, village_area) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'District Coordinator Admin', '+91 98765 00001', 'admin@ruralhelp.org', 'ADMIN', 'District HQ'),
(2, 'jalseva_ngo', '060fe28178c35c127d8382fc7f8d9a30946251ec8b6d11793833f3d1c081c625', 'Jal Seva Mission', '+91 98765 11111', 'contact@jalseva.org', 'NGO', 'Rampur Sector'),
(3, 'aarogya_ngo', '060fe28178c35c127d8382fc7f8d9a30946251ec8b6d11793833f3d1c081c625', 'Aarogya Rural Trust', '+91 98765 22222', 'help@aarogyatrust.org', 'NGO', 'Belgaon Sector'),
(4, 'vidya_ngo', '060fe28178c35c127d8382fc7f8d9a30946251ec8b6d11793833f3d1c081c625', 'Vidya Vikas Foundation', '+91 98765 33333', 'info@vidyavikas.org', 'NGO', 'Shivpur Sector'),
(5, 'ramesh_k', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'Ramesh Kumar', '+91 91234 56780', 'ramesh@village.in', 'COMMUNITY_MEMBER', 'Rampur'),
(6, 'priya_sharma', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'Priya Sharma', '+91 91234 56781', 'priya@village.in', 'COMMUNITY_MEMBER', 'Shivpur'),
(7, 'anita_devi', 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'Anita Devi', '+91 91234 56782', 'anita@village.in', 'COMMUNITY_MEMBER', 'Belgaon');

-- NGO Profiles
INSERT INTO ngos (id, user_id, organization_name, contact_person, phone, email, max_capacity, description) VALUES
(1, 2, 'Jal Seva Mission', 'Suresh Patel', '+91 98765 11111', 'contact@jalseva.org', 6, 'Specialized in rural water purification, handpump repairs, and rainwater conservation.'),
(2, 3, 'Aarogya Rural Trust', 'Dr. Meera Iyer', '+91 98765 22222', 'help@aarogyatrust.org', 5, 'Provides mobile health clinics, sanitation drives, and primary medical support.'),
(3, 4, 'Vidya Vikas Foundation', 'Kavita Sen', '+91 98765 33333', 'info@vidyavikas.org', 4, 'Focuses on village school infrastructure, sanitation in schools, and education programs.');

-- NGO Specializations
INSERT INTO ngo_specializations (ngo_id, category) VALUES
(1, 'CLEAN_WATER'),
(1, 'WASTE_MANAGEMENT'),
(2, 'HEALTHCARE'),
(2, 'SANITATION'),
(3, 'EDUCATION'),
(3, 'SANITATION');

-- NGO Service Areas
INSERT INTO ngo_service_areas (ngo_id, village_area) VALUES
(1, 'Rampur'),
(1, 'Shivpur'),
(2, 'Rampur'),
(2, 'Belgaon'),
(3, 'Shivpur'),
(3, 'Belgaon');

-- Sample Issues
INSERT INTO issues (id, reporter_id, title, category, village_area, urgency, description, photo_path, status, priority_score, created_at) VALUES
(1, 5, 'Main village handpump non-functional for 5 days', 'CLEAN_WATER', 'Rampur', 'HIGH', 'Drinking water crisis in East Rampur. Over 80 households dependent on this pump.', '', 'PENDING_VERIFICATION', 75, NOW() - INTERVAL 4 DAY),
(2, 5, 'Drainage overflow near local health sub-center', 'SANITATION', 'Rampur', 'CRITICAL', 'Open drain overflowing onto the main street, creating severe disease outbreak risk.', '', 'VERIFIED', 90, NOW() - INTERVAL 3 DAY),
(3, 6, 'Shortage of essential medicines at primary clinic', 'HEALTHCARE', 'Shivpur', 'HIGH', 'Vaccine refrigeration failure and lack of basic fever/ORS packets.', '', 'ASSIGNED', 80, NOW() - INTERVAL 2 DAY),
(4, 6, 'Damaged roof and broken desks at Primary School', 'EDUCATION', 'Shivpur', 'MEDIUM', 'Monsoon rain leaking into grade 3 and 4 classrooms. Requires urgent repair.', '', 'IN_PROGRESS', 60, NOW() - INTERVAL 5 DAY),
(5, 7, 'Plastic and agricultural waste burning near farms', 'WASTE_MANAGEMENT', 'Belgaon', 'LOW', 'Improper dumping and open burning causing air contamination.', '', 'RESOLVED', 35, NOW() - INTERVAL 7 DAY),
(6, 7, 'Contaminated well water causing gastro cases', 'CLEAN_WATER', 'Belgaon', 'CRITICAL', 'Several children reported sick with diarrhea after consuming water from the community well.', '', 'VERIFIED', 95, NOW() - INTERVAL 1 DAY);

-- Assignments for issues 3 and 4
INSERT INTO assignments (id, issue_id, ngo_id, assigned_by, assigned_at, status, notes) VALUES
(1, 3, 2, 1, NOW() - INTERVAL 1 DAY, 'ACTIVE', 'Allocated to Aarogya Trust due to emergency medical supplies required in Shivpur.'),
(2, 4, 3, 1, NOW() - INTERVAL 4 DAY, 'ACTIVE', 'Assigned to Vidya Vikas Foundation for classroom roof repair and desk replacements.');

-- Status History
INSERT INTO status_history (issue_id, changed_by, previous_status, new_status, comments, changed_at) VALUES
(1, 5, NULL, 'PENDING_VERIFICATION', 'Issue reported by community member Ramesh Kumar.', NOW() - INTERVAL 4 DAY),
(2, 5, NULL, 'PENDING_VERIFICATION', 'Issue reported by community member Ramesh Kumar.', NOW() - INTERVAL 3 DAY),
(2, 1, 'PENDING_VERIFICATION', 'VERIFIED', 'Verified by Admin after cross-check with village panchayat head.', NOW() - INTERVAL 2 DAY),
(3, 6, NULL, 'PENDING_VERIFICATION', 'Issue reported by Priya Sharma.', NOW() - INTERVAL 2 DAY),
(3, 1, 'PENDING_VERIFICATION', 'VERIFIED', 'Verified by Admin.', NOW() - INTERVAL 1 DAY),
(3, 1, 'VERIFIED', 'ASSIGNED', 'Matched and assigned to Aarogya Rural Trust.', NOW() - INTERVAL 1 DAY),
(4, 6, NULL, 'PENDING_VERIFICATION', 'Issue reported by Priya Sharma.', NOW() - INTERVAL 5 DAY),
(4, 1, 'PENDING_VERIFICATION', 'VERIFIED', 'Verified by Admin.', NOW() - INTERVAL 4 DAY),
(4, 1, 'VERIFIED', 'ASSIGNED', 'Assigned to Vidya Vikas Foundation.', NOW() - INTERVAL 4 DAY),
(4, 4, 'ASSIGNED', 'IN_PROGRESS', 'NGO dispatched contractors to evaluate roof replacement.', NOW() - INTERVAL 3 DAY),
(5, 7, NULL, 'PENDING_VERIFICATION', 'Issue reported by Anita Devi.', NOW() - INTERVAL 7 DAY),
(5, 1, 'PENDING_VERIFICATION', 'VERIFIED', 'Verified by Admin.', NOW() - INTERVAL 6 DAY),
(5, 1, 'VERIFIED', 'ASSIGNED', 'Assigned to Jal Seva Mission for waste collection.', NOW() - INTERVAL 5 DAY),
(5, 2, 'ASSIGNED', 'IN_PROGRESS', 'Clean-up drive organized.', NOW() - INTERVAL 3 DAY),
(5, 2, 'IN_PROGRESS', 'RESOLVED', 'Waste cleared and designated compost pits created with villagers.', NOW() - INTERVAL 1 DAY);
