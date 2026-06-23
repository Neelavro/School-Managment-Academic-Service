-- ============================================================================
-- academic_service — Seed data
-- ============================================================================
-- Populates bootstrap tables + academic structure + 1500 students + 1500
-- enrollments for the 2026 academic year.
--
-- Distribution:
--   10 classes (Class 1..10), 3 sections each (A/B/C) = 30 sections
--   150 students per class, 50 per section
--   Sections A & C are Boys, Section B is Girls
--   Classes 9 & 10 get a student_group (Science/Commerce/Arts round-robin)
--
-- Run AFTER:
--   bootstrap.sql  →  academic_structure.sql  →  students.sql
--
-- Idempotent for bootstrap rows (INSERT IGNORE on fixed PKs).
-- NOT idempotent for the 1500 students — re-running will hit the UNIQUE on
-- student_system_id and abort the batch (leaving the first run intact).
-- To re-seed students:
--   DELETE FROM enrollment;
--   DELETE FROM student_image;
--   DELETE FROM student;
-- then re-run this file.
-- ============================================================================

USE academic_service;

-- Recursive CTE needs > default 1000 to reach 1500.
SET SESSION cte_max_recursion_depth = 2000;

-- ─── system_settings ────────────────────────────────────────────────────────
INSERT IGNORE INTO system_settings (id, institution_name, address, heading) VALUES
  (1, 'Demo School', 'Dhaka, Bangladesh', 'Demo School Management System');

-- ─── academic_year ──────────────────────────────────────────────────────────
INSERT IGNORE INTO academic_year (id, year_name, is_active) VALUES
  (1, '2026', b'1');

-- ─── shift ──────────────────────────────────────────────────────────────────
INSERT IGNORE INTO shift (id, name, is_active) VALUES
  (1, 'Day',     b'1'),
  (2, 'Morning', b'1');

-- ─── gender (student.gender_id) ─────────────────────────────────────────────
INSERT IGNORE INTO gender (id, gender, is_active) VALUES
  (1, 'Male',   b'1'),
  (2, 'Female', b'1');

-- ─── student_status ─────────────────────────────────────────────────────────
INSERT IGNORE INTO student_status (id, status_name) VALUES
  (1, 'ACTIVE'),
  (2, 'TRANSFERRED'),
  (3, 'ARCHIVED');

-- ─── gender_section (section.gender_id, enrollment.gender_section_id) ──────
INSERT IGNORE INTO gender_section (id, gender_name) VALUES
  (1, 'Boys'),
  (2, 'Girls');

-- ─── student_group ──────────────────────────────────────────────────────────
INSERT IGNORE INTO student_group (id, group_name, is_active) VALUES
  (1, 'Science',  b'1'),
  (2, 'Commerce', b'1'),
  (3, 'Arts',     b'1');

-- ─── subject ────────────────────────────────────────────────────────────────
INSERT IGNORE INTO subject (id, name, code, is_active) VALUES
  ( 1, 'Bangla',         'BAN', b'1'),
  ( 2, 'English',        'ENG', b'1'),
  ( 3, 'Mathematics',    'MTH', b'1'),
  ( 4, 'General Science','SCI', b'1'),
  ( 5, 'Social Science', 'SOC', b'1'),
  ( 6, 'Religion',       'REL', b'1'),
  ( 7, 'Physics',        'PHY', b'1'),
  ( 8, 'Chemistry',      'CHM', b'1'),
  ( 9, 'Biology',        'BIO', b'1'),
  (10, 'ICT',            'ICT', b'1');

-- ─── room ───────────────────────────────────────────────────────────────────
INSERT IGNORE INTO room (id, name, capacity, is_active) VALUES
  (1, 'Room 101', 50, b'1'),
  (2, 'Room 102', 50, b'1'),
  (3, 'Room 103', 50, b'1'),
  (4, 'Room 201', 50, b'1'),
  (5, 'Room 202', 50, b'1');

-- ─── grading_policies + grades ─────────────────────────────────────────────
INSERT IGNORE INTO grading_policies (id, name, is_active) VALUES
  (1, 'Default GPA-5', b'1');

INSERT IGNORE INTO grades (id, grading_policy_id, name, gpa_value, min_mark, max_mark) VALUES
  (1, 1, 'A+', 5.00, 80.00, 100.00),
  (2, 1, 'A',  4.00, 70.00,  79.99),
  (3, 1, 'A-', 3.50, 60.00,  69.99),
  (4, 1, 'B',  3.00, 50.00,  59.99),
  (5, 1, 'C',  2.00, 40.00,  49.99),
  (6, 1, 'D',  1.00, 33.00,  39.99),
  (7, 1, 'F',  0.00,  0.00,  32.99);

-- ─── class (10 classes split across both shifts) ───────────────────────────
-- Primary (1–5) → Morning shift (id 2); Secondary (6–10) → Day shift (id 1).
-- Common pattern for Bangladeshi schools that double up the building.
INSERT IGNORE INTO class (id, name, shift_id, grading_policy_id, order_index, is_active) VALUES
  ( 1, 'Class 1',  2, 1,  1, b'1'),
  ( 2, 'Class 2',  2, 1,  2, b'1'),
  ( 3, 'Class 3',  2, 1,  3, b'1'),
  ( 4, 'Class 4',  2, 1,  4, b'1'),
  ( 5, 'Class 5',  2, 1,  5, b'1'),
  ( 6, 'Class 6',  1, 1,  6, b'1'),
  ( 7, 'Class 7',  1, 1,  7, b'1'),
  ( 8, 'Class 8',  1, 1,  8, b'1'),
  ( 9, 'Class 9',  1, 1,  9, b'1'),
  (10, 'Class 10', 1, 1, 10, b'1');

-- ─── class_student_group (Class 9, 10 only) ────────────────────────────────
INSERT IGNORE INTO class_student_group (class_id, student_group_id) VALUES
  ( 9, 1), ( 9, 2), ( 9, 3),
  (10, 1), (10, 2), (10, 3);

-- ─── section (3 per class × 10 = 30) ───────────────────────────────────────
-- Section gender pattern: A=Boys, B=Girls, C=Boys
INSERT IGNORE INTO section (id, section_name, class_id, gender_id, is_active) VALUES
  ( 1, 'A',  1, 1, b'1'), ( 2, 'B',  1, 2, b'1'), ( 3, 'C',  1, 1, b'1'),
  ( 4, 'A',  2, 1, b'1'), ( 5, 'B',  2, 2, b'1'), ( 6, 'C',  2, 1, b'1'),
  ( 7, 'A',  3, 1, b'1'), ( 8, 'B',  3, 2, b'1'), ( 9, 'C',  3, 1, b'1'),
  (10, 'A',  4, 1, b'1'), (11, 'B',  4, 2, b'1'), (12, 'C',  4, 1, b'1'),
  (13, 'A',  5, 1, b'1'), (14, 'B',  5, 2, b'1'), (15, 'C',  5, 1, b'1'),
  (16, 'A',  6, 1, b'1'), (17, 'B',  6, 2, b'1'), (18, 'C',  6, 1, b'1'),
  (19, 'A',  7, 1, b'1'), (20, 'B',  7, 2, b'1'), (21, 'C',  7, 1, b'1'),
  (22, 'A',  8, 1, b'1'), (23, 'B',  8, 2, b'1'), (24, 'C',  8, 1, b'1'),
  (25, 'A',  9, 1, b'1'), (26, 'B',  9, 2, b'1'), (27, 'C',  9, 1, b'1'),
  (28, 'A', 10, 1, b'1'), (29, 'B', 10, 2, b'1'), (30, 'C', 10, 1, b'1');

-- ============================================================================
-- 1500 STUDENTS
-- ============================================================================
-- student_system_id pattern: 6-digit zero-padded (000001..001500)
-- gender_id: alternates Male/Female
-- All marked ACTIVE.
-- ============================================================================

INSERT INTO student (
    student_system_id, name_bangla, name_english,
    father_name_english, father_phone,
    mother_name_english, mother_phone,
    guardian_name_english, guardian_phone, guardian_relation,
    current_district, current_thana,
    permanent_district, permanent_thana,
    dob, nationality, is_active,
    gender_id, student_status_id
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1500
)
SELECT
    LPAD(n, 6, '0')                                                  AS student_system_id,
    CONCAT('শিক্ষার্থী ', LPAD(n, 4, '0'))                                AS name_bangla,
    CONCAT('Student ', LPAD(n, 4, '0'))                              AS name_english,
    CONCAT('Father of ', LPAD(n, 4, '0'))                            AS father_name_english,
    CONCAT('017', LPAD(10000000 + n, 8, '0'))                        AS father_phone,
    CONCAT('Mother of ', LPAD(n, 4, '0'))                            AS mother_name_english,
    CONCAT('018', LPAD(10000000 + n, 8, '0'))                        AS mother_phone,
    CONCAT('Father of ', LPAD(n, 4, '0'))                            AS guardian_name_english,
    CONCAT('017', LPAD(10000000 + n, 8, '0'))                        AS guardian_phone,
    'Father'                                                         AS guardian_relation,
    'Dhaka'                                                          AS current_district,
    'Dhanmondi'                                                      AS current_thana,
    'Dhaka'                                                          AS permanent_district,
    'Dhanmondi'                                                      AS permanent_thana,
    DATE_ADD('2010-01-01', INTERVAL FLOOR(RAND(n) * 365 * 8) DAY)    AS dob,
    'Bangladeshi'                                                    AS nationality,
    b'1'                                                             AS is_active,
    CASE WHEN n % 2 = 0 THEN 1 ELSE 2 END                            AS gender_id,
    1                                                                AS student_status_id
FROM seq;

-- ============================================================================
-- 1500 ENROLLMENTS (one per student in academic_year 2026)
-- ============================================================================
-- Maths:
--   class_id      = ((n-1) DIV 150) + 1                  -- 1..10
--   section_pos   = ((n-1) MOD 150) DIV 50 + 1           -- 1..3 (A/B/C)
--   section_id    = (class_id - 1) * 3 + section_pos     -- 1..30
--   class_roll    = ((n-1) MOD 50) + 1                   -- 1..50 per section
--   gender_section_id: A,C → Boys (1); B → Girls (2)
--   student_group_id: classes 9, 10 only — round-robin Science/Commerce/Arts
-- ============================================================================

INSERT INTO enrollment (
    student_system_id, academic_year_id, class_id, section_id, shift_id,
    gender_section_id, student_group_id, class_roll, is_active
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1500
),
calc AS (
    SELECT
        n,
        ((n - 1) DIV 150) + 1                AS class_id_calc,
        ((n - 1) MOD 150) DIV 50 + 1         AS section_pos
    FROM seq
)
SELECT
    LPAD(n, 6, '0')                              AS student_system_id,
    1                                            AS academic_year_id,
    class_id_calc                                AS class_id,
    (class_id_calc - 1) * 3 + section_pos        AS section_id,
    -- shift derives from class: 1–5 Morning (2), 6–10 Day (1)
    CASE WHEN class_id_calc <= 5 THEN 2 ELSE 1 END      AS shift_id,
    CASE WHEN section_pos IN (1, 3) THEN 1 ELSE 2 END   AS gender_section_id,
    CASE
        WHEN class_id_calc >= 9 THEN ((n - 1) MOD 3) + 1
        ELSE NULL
    END                                          AS student_group_id,
    ((n - 1) MOD 50) + 1                         AS class_roll,
    b'1'                                         AS is_active
FROM calc;

-- ============================================================================
-- Verification
-- ============================================================================
SELECT 'system_settings' AS tbl, COUNT(*) AS rows_ FROM system_settings UNION ALL
SELECT 'academic_year',  COUNT(*) FROM academic_year UNION ALL
SELECT 'shift',          COUNT(*) FROM shift UNION ALL
SELECT 'gender',         COUNT(*) FROM gender UNION ALL
SELECT 'student_status', COUNT(*) FROM student_status UNION ALL
SELECT 'gender_section', COUNT(*) FROM gender_section UNION ALL
SELECT 'student_group',  COUNT(*) FROM student_group UNION ALL
SELECT 'subject',        COUNT(*) FROM subject UNION ALL
SELECT 'room',           COUNT(*) FROM room UNION ALL
SELECT 'grading_policies', COUNT(*) FROM grading_policies UNION ALL
SELECT 'grades',         COUNT(*) FROM grades UNION ALL
SELECT 'class',          COUNT(*) FROM class UNION ALL
SELECT 'class_student_group', COUNT(*) FROM class_student_group UNION ALL
SELECT 'section',        COUNT(*) FROM section UNION ALL
SELECT 'student',        COUNT(*) FROM student UNION ALL
SELECT 'enrollment',     COUNT(*) FROM enrollment;

-- Per-class enrollment spread (should be 150 each):
SELECT class_id, COUNT(*) AS students FROM enrollment GROUP BY class_id ORDER BY class_id;

-- Per-section spread (should be 50 each, 30 sections):
SELECT section_id, COUNT(*) AS students FROM enrollment GROUP BY section_id ORDER BY section_id;
