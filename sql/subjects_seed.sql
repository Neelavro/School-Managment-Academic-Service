-- ============================================================================
-- academic_service — Subjects + class-subject-group mapping
-- ============================================================================
-- Replaces the 10 placeholder subjects from seed_data.sql with a proper
-- Bangladeshi school subject catalog (Primary → Junior → SSC), then maps
-- which subjects each class teaches.
--
-- Class structure assumed (from seed_data.sql):
--   Class 1–5  → Primary    (no streaming)
--   Class 6–8  → Junior     (no streaming, optional 4th subject)
--   Class 9–10 → SSC        (Science / Commerce / Humanities streams)
--
-- student_group ids from seed_data.sql:
--   1 = Science     2 = Commerce     3 = Arts (Humanities)
--
-- Run AFTER seed_data.sql.
--
-- Idempotent:
--   - TRUNCATE wipes class_subject_group + subject cleanly.
--   - Subject IDs are explicit, so re-runs produce identical data.
-- ============================================================================

USE academic_service;

-- ── 1. Wipe existing subjects + mappings ──────────────────────────────────
-- class_subject_group depends on subject (CASCADE on subject side), but
-- subject FK from class_routine etc. is RESTRICT — so kill csg first then
-- subjects.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE class_subject_group;
TRUNCATE TABLE subject;
SET FOREIGN_KEY_CHECKS = 1;

-- ── 2. Subjects (Bangladeshi NCTB-style catalog) ───────────────────────────
INSERT INTO subject (id, name, code, is_active) VALUES
  -- Primary (Class 1–5)
  ( 1, 'Bangla',                                       'BAN',  b'1'),
  ( 2, 'English',                                      'ENG',  b'1'),
  ( 3, 'Mathematics',                                  'MATH', b'1'),
  ( 4, 'Bangladesh and Global Studies',                'BGS',  b'1'),
  ( 5, 'Primary Science',                              'PSCI', b'1'),
  ( 6, 'Religion and Moral Education',                 'REL',  b'1'),
  ( 7, 'Physical Education and Health',                'PE',   b'1'),

  -- Junior + SSC compulsory (Class 6–10)
  ( 8, 'Bangla 1st Paper',                             'BAN1', b'1'),
  ( 9, 'Bangla 2nd Paper',                             'BAN2', b'1'),
  (10, 'English 1st Paper',                            'ENG1', b'1'),
  (11, 'English 2nd Paper',                            'ENG2', b'1'),
  (12, 'General Science',                              'GSCI', b'1'),
  (13, 'Information and Communication Technology',     'ICT',  b'1'),

  -- Junior 4th-subject options (Class 6–8)
  (14, 'Agriculture Studies',                          'AGR',  b'1'),
  (15, 'Home Science',                                 'HSCI', b'1'),
  (16, 'Work and Life Oriented Education',             'WLOE', b'1'),

  -- SSC Science (Class 9–10)
  (17, 'Physics',                                      'PHY',  b'1'),
  (18, 'Chemistry',                                    'CHE',  b'1'),
  (19, 'Biology',                                      'BIO',  b'1'),
  (20, 'Higher Mathematics',                           'HMTH', b'1'),

  -- SSC Commerce (Class 9–10)
  (21, 'Accounting',                                   'ACC',  b'1'),
  (22, 'Finance, Banking and Insurance',               'FBI',  b'1'),
  (23, 'Business Entrepreneurship',                    'BENT', b'1'),
  (24, 'Economics',                                    'ECO',  b'1'),

  -- SSC Humanities (Class 9–10)
  (25, 'Civics and Citizenship',                       'CIV',  b'1'),
  (26, 'Geography and Environment',                    'GEO',  b'1'),
  (27, 'History of Bangladesh and World Civilization', 'HIST', b'1');

-- ── 3. class_subject_group mappings ───────────────────────────────────────
-- Convention: student_group_id = NULL means "all groups / no streaming".
--             is_fourth_subject = 1 marks the optional 4th subject.

-- ─── Primary (Class 1–5) ─────────────────────────────────────────────────
-- 7 subjects × 5 classes = 35 rows
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 1
  (1, 1, NULL, b'1', b'0'), (1, 2, NULL, b'1', b'0'), (1, 3, NULL, b'1', b'0'),
  (1, 4, NULL, b'1', b'0'), (1, 5, NULL, b'1', b'0'), (1, 6, NULL, b'1', b'0'),
  (1, 7, NULL, b'1', b'0'),
  -- Class 2
  (2, 1, NULL, b'1', b'0'), (2, 2, NULL, b'1', b'0'), (2, 3, NULL, b'1', b'0'),
  (2, 4, NULL, b'1', b'0'), (2, 5, NULL, b'1', b'0'), (2, 6, NULL, b'1', b'0'),
  (2, 7, NULL, b'1', b'0'),
  -- Class 3
  (3, 1, NULL, b'1', b'0'), (3, 2, NULL, b'1', b'0'), (3, 3, NULL, b'1', b'0'),
  (3, 4, NULL, b'1', b'0'), (3, 5, NULL, b'1', b'0'), (3, 6, NULL, b'1', b'0'),
  (3, 7, NULL, b'1', b'0'),
  -- Class 4
  (4, 1, NULL, b'1', b'0'), (4, 2, NULL, b'1', b'0'), (4, 3, NULL, b'1', b'0'),
  (4, 4, NULL, b'1', b'0'), (4, 5, NULL, b'1', b'0'), (4, 6, NULL, b'1', b'0'),
  (4, 7, NULL, b'1', b'0'),
  -- Class 5
  (5, 1, NULL, b'1', b'0'), (5, 2, NULL, b'1', b'0'), (5, 3, NULL, b'1', b'0'),
  (5, 4, NULL, b'1', b'0'), (5, 5, NULL, b'1', b'0'), (5, 6, NULL, b'1', b'0'),
  (5, 7, NULL, b'1', b'0');

-- ─── Junior (Class 6–8) ──────────────────────────────────────────────────
-- 10 compulsory + 3 4th-subject options per class
-- Compulsory: Bangla 1/2, English 1/2, Math, Gen Science, BGS, Religion, ICT, PE
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 6 compulsory
  (6,  8, NULL, b'1', b'0'), (6,  9, NULL, b'1', b'0'),
  (6, 10, NULL, b'1', b'0'), (6, 11, NULL, b'1', b'0'),
  (6,  3, NULL, b'1', b'0'), (6, 12, NULL, b'1', b'0'),
  (6,  4, NULL, b'1', b'0'), (6,  6, NULL, b'1', b'0'),
  (6, 13, NULL, b'1', b'0'), (6,  7, NULL, b'1', b'0'),
  -- Class 6 4th-subject options
  (6, 14, NULL, b'1', b'1'), (6, 15, NULL, b'1', b'1'), (6, 16, NULL, b'1', b'1'),

  -- Class 7 compulsory
  (7,  8, NULL, b'1', b'0'), (7,  9, NULL, b'1', b'0'),
  (7, 10, NULL, b'1', b'0'), (7, 11, NULL, b'1', b'0'),
  (7,  3, NULL, b'1', b'0'), (7, 12, NULL, b'1', b'0'),
  (7,  4, NULL, b'1', b'0'), (7,  6, NULL, b'1', b'0'),
  (7, 13, NULL, b'1', b'0'), (7,  7, NULL, b'1', b'0'),
  -- Class 7 4th-subject options
  (7, 14, NULL, b'1', b'1'), (7, 15, NULL, b'1', b'1'), (7, 16, NULL, b'1', b'1'),

  -- Class 8 compulsory
  (8,  8, NULL, b'1', b'0'), (8,  9, NULL, b'1', b'0'),
  (8, 10, NULL, b'1', b'0'), (8, 11, NULL, b'1', b'0'),
  (8,  3, NULL, b'1', b'0'), (8, 12, NULL, b'1', b'0'),
  (8,  4, NULL, b'1', b'0'), (8,  6, NULL, b'1', b'0'),
  (8, 13, NULL, b'1', b'0'), (8,  7, NULL, b'1', b'0'),
  -- Class 8 4th-subject options
  (8, 14, NULL, b'1', b'1'), (8, 15, NULL, b'1', b'1'), (8, 16, NULL, b'1', b'1');

-- ─── SSC Compulsory (Class 9–10, NULL group = all streams) ───────────────
-- Bangla 1/2, English 1/2, Math, Religion, ICT, PE
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 9
  (9,  8, NULL, b'1', b'0'), (9,  9, NULL, b'1', b'0'),
  (9, 10, NULL, b'1', b'0'), (9, 11, NULL, b'1', b'0'),
  (9,  3, NULL, b'1', b'0'), (9,  6, NULL, b'1', b'0'),
  (9, 13, NULL, b'1', b'0'), (9,  7, NULL, b'1', b'0'),
  -- Class 10
  (10,  8, NULL, b'1', b'0'), (10,  9, NULL, b'1', b'0'),
  (10, 10, NULL, b'1', b'0'), (10, 11, NULL, b'1', b'0'),
  (10,  3, NULL, b'1', b'0'), (10,  6, NULL, b'1', b'0'),
  (10, 13, NULL, b'1', b'0'), (10,  7, NULL, b'1', b'0');

-- ─── SSC Science group (Class 9–10, group_id = 1) ───────────────────────
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 9 Science
  (9, 17, 1, b'1', b'0'),  -- Physics
  (9, 18, 1, b'1', b'0'),  -- Chemistry
  (9, 19, 1, b'1', b'0'),  -- Biology
  (9, 20, 1, b'1', b'1'),  -- Higher Math (4th)
  -- Class 10 Science
  (10, 17, 1, b'1', b'0'),
  (10, 18, 1, b'1', b'0'),
  (10, 19, 1, b'1', b'0'),
  (10, 20, 1, b'1', b'1');

-- ─── SSC Commerce group (Class 9–10, group_id = 2) ──────────────────────
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 9 Commerce
  (9, 21, 2, b'1', b'0'),  -- Accounting
  (9, 22, 2, b'1', b'0'),  -- Finance, Banking and Insurance
  (9, 23, 2, b'1', b'0'),  -- Business Entrepreneurship
  (9, 24, 2, b'1', b'1'),  -- Economics (4th)
  -- Class 10 Commerce
  (10, 21, 2, b'1', b'0'),
  (10, 22, 2, b'1', b'0'),
  (10, 23, 2, b'1', b'0'),
  (10, 24, 2, b'1', b'1');

-- ─── SSC Humanities/Arts group (Class 9–10, group_id = 3) ───────────────
INSERT INTO class_subject_group (class_id, subject_id, student_group_id, is_active, is_fourth_subject) VALUES
  -- Class 9 Humanities
  (9, 25, 3, b'1', b'0'),  -- Civics and Citizenship
  (9, 26, 3, b'1', b'0'),  -- Geography and Environment
  (9, 27, 3, b'1', b'0'),  -- History
  (9, 24, 3, b'1', b'1'),  -- Economics (4th)
  -- Class 10 Humanities
  (10, 25, 3, b'1', b'0'),
  (10, 26, 3, b'1', b'0'),
  (10, 27, 3, b'1', b'0'),
  (10, 24, 3, b'1', b'1');

-- ============================================================================
-- Verification
-- ============================================================================
SELECT 'subject'             AS tbl, COUNT(*) AS rows_ FROM subject UNION ALL
SELECT 'class_subject_group', COUNT(*) FROM class_subject_group;

-- Per-class subject count (compulsory + group-specific):
SELECT
  c.id           AS class_id,
  c.name         AS class_name,
  COUNT(csg.id)  AS subject_assignments
FROM class c
LEFT JOIN class_subject_group csg ON csg.class_id = c.id
GROUP BY c.id, c.name
ORDER BY c.id;

-- Per-group breakdown for SSC classes:
SELECT
  c.name                       AS class_name,
  COALESCE(sg.group_name, '—') AS stream,
  COUNT(*)                     AS subjects,
  SUM(CASE WHEN csg.is_fourth_subject = b'1' THEN 1 ELSE 0 END) AS fourth_subjects
FROM class_subject_group csg
JOIN class c ON c.id = csg.class_id
LEFT JOIN student_group sg ON sg.id = csg.student_group_id
WHERE c.id IN (9, 10)
GROUP BY c.id, c.name, csg.student_group_id, sg.group_name
ORDER BY c.id, csg.student_group_id;
