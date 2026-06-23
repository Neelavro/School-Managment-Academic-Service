-- ============================================================================
-- academic_service — Class routine + exam routine/session seed (shift-aware)
-- ============================================================================
-- Generates:
--   1. exam_type        — Half-Yearly, Annual
--   2. exam_routine     — one per type for the 2026 academic year (2 rows)
--   3. class_routine    — full weekly timetable for every class × section
--                         (SUN–THU, 6 periods/day)
--   4. exam_session     — one per (routine × class × compulsory subject)
--   5. exam_class_room_assignment — one room per (routine × class)
--
-- SHIFT-AWARENESS:
--   Both class_routine.start/end_time AND exam_session.start/end_time are
--   computed from each class's shift_id. Two shifts are supported:
--     shift_id = 1 (Day)     → school day starts 12:30
--     shift_id = 2 (Morning) → school day starts 07:00
--   Exam sessions:
--     Day     → 13:00–16:00
--     Morning → 08:00–11:00
--
--   seed_data.sql splits classes across both shifts by default:
--     Classes 1–5  → Morning (shift_id = 2)
--     Classes 6–10 → Day     (shift_id = 1)
--   So Morning and Day timings both appear in the resulting routine + sessions.
--   To override the split, edit the OPTIONAL REBALANCE block below.
--
-- Subject selection:
--   Pulls compulsory subjects from class_subject_group where student_group_id
--   IS NULL. Group-specific subjects (Physics, Accounting, Civics …) need a
--   per-section group pick that doesn't exist in section yet — add via UI.
--
-- Run AFTER:
--   bootstrap.sql → academic_structure.sql → students.sql → exams.sql →
--   seed_data.sql → subjects_seed.sql
--
-- Idempotent:
--   exam_type uses INSERT IGNORE; the rest TRUNCATE first.
-- ============================================================================

USE academic_service;
SET SESSION cte_max_recursion_depth = 2000;

-- ============================================================================
-- OPTIONAL — REBALANCE classes across both shifts
-- ============================================================================
-- Uncomment to flip Classes 1–5 to Morning shift, keep Classes 6–10 on Day.
-- (Class 1–5 = primary in the morning is a common pattern in BD schools.)
--
-- UPDATE class SET shift_id = 2 WHERE id BETWEEN 1 AND 5;
-- UPDATE class SET shift_id = 1 WHERE id BETWEEN 6 AND 10;
-- ============================================================================

-- ── 1. exam_type ──────────────────────────────────────────────────────────
INSERT IGNORE INTO exam_type (id, name, order_index, is_active) VALUES
  (1, 'Half-Yearly Examination', 1, b'1'),
  (2, 'Annual Examination',      2, b'1');

-- ── 2. Wipe existing routine + exam seeded data ───────────────────────────
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE exam_class_room_assignment;
TRUNCATE TABLE exam_session;
TRUNCATE TABLE exam_routine;
TRUNCATE TABLE class_routine;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 3. exam_routine
-- ============================================================================
INSERT INTO exam_routine (
    id, title, exam_type_id, academic_year_id, status, published_at,
    created_at, last_modified_at, is_active,
    routine_start_date, routine_end_date, consider_for_annual_result
) VALUES
  (1, 'Half-Yearly Examination 2026', 1, 1, 'PUBLISHED', '2026-06-01 09:00:00',
      '2026-05-15 10:00:00', '2026-06-01 09:00:00', b'1',
      '2026-06-15', '2026-06-30', b'1'),
  (2, 'Annual Examination 2026',      2, 1, 'PUBLISHED', '2026-11-15 09:00:00',
      '2026-10-15 10:00:00', '2026-11-15 09:00:00', b'1',
      '2026-12-01', '2026-12-20', b'1');

-- ============================================================================
-- 4. class_routine — 10 classes × 3 sections × 5 days × 6 periods = 900 rows
-- ============================================================================
-- Shift-aware time math (offsets are MINUTES from the shift's first-bell):
--   Period 1:   0  → 45        (45 min)
--   Period 2:  50  → 95
--   Period 3: 100  → 145
--   --- break (20 min) ---
--   Period 4: 165  → 210
--   Period 5: 215  → 260
--   Period 6: 265  → 310
-- ============================================================================

INSERT INTO class_routine (
    class_id, section_id, subject_id, room_id,
    day_of_week, start_time, end_time, routine_type, is_active
)
WITH RECURSIVE
  -- Pulls every active class from the table — works for any number of
  -- classes / shifts, not just the original 10.
  classes AS (
      SELECT id AS class_id FROM class WHERE is_active = b'1'
  ),
  sections_pos AS (
      SELECT 1 AS section_pos
      UNION ALL SELECT section_pos + 1 FROM sections_pos WHERE section_pos < 3
  ),
  days AS (
      SELECT 1 AS day_num, 'SUNDAY'    AS day_name UNION ALL
      SELECT 2,            'MONDAY'                UNION ALL
      SELECT 3,            'TUESDAY'               UNION ALL
      SELECT 4,            'WEDNESDAY'             UNION ALL
      SELECT 5,            'THURSDAY'
  ),
  -- Per-period offsets from the shift's first-bell.
  periods AS (
      SELECT 1 AS period_num,   0 AS start_off,  45 AS end_off UNION ALL
      SELECT 2,                 50,              95             UNION ALL
      SELECT 3,                100,             145             UNION ALL
      SELECT 4,                165,             210             UNION ALL
      SELECT 5,                215,             260             UNION ALL
      SELECT 6,                265,             310
  ),
  -- shift_id → first-bell. Morning starts early; Day starts after noon.
  shift_times AS (
      SELECT 1 AS shift_id, '12:30:00' AS shift_start UNION ALL
      SELECT 2,             '07:00:00'
  ),
  -- Rank each class's compulsory subjects deterministically.
  ranked_subjects AS (
      SELECT
          csg.class_id,
          csg.subject_id,
          ROW_NUMBER() OVER (PARTITION BY csg.class_id ORDER BY csg.subject_id) AS rn,
          COUNT(*)    OVER (PARTITION BY csg.class_id)                          AS total_subjects
      FROM class_subject_group csg
      WHERE csg.student_group_id IS NULL
        AND csg.is_active = b'1'
  ),
  grid AS (
      SELECT
          c.class_id,
          cl.shift_id,
          s.section_pos,
          d.day_num,
          d.day_name,
          p.period_num,
          p.start_off,
          p.end_off,
          (d.day_num - 1) * 6 + (p.period_num - 1) AS slot
      FROM classes c
      JOIN class cl ON cl.id = c.class_id
      CROSS JOIN sections_pos s
      CROSS JOIN days d
      CROSS JOIN periods p
  )
SELECT
    g.class_id,
    (g.class_id - 1) * 3 + g.section_pos AS section_id,
    rs.subject_id,
    ((g.slot + g.class_id + g.section_pos) % 5) + 1 AS room_id,
    g.day_name AS day_of_week,
    ADDTIME(st.shift_start, SEC_TO_TIME(g.start_off * 60)) AS start_time,
    ADDTIME(st.shift_start, SEC_TO_TIME(g.end_off   * 60)) AS end_time,
    'DEFAULT'  AS routine_type,
    b'1'       AS is_active
FROM grid g
JOIN shift_times st  ON st.shift_id = COALESCE(g.shift_id, 1)
JOIN ranked_subjects rs
  ON rs.class_id = g.class_id
 AND rs.rn       = (g.slot % rs.total_subjects) + 1;

-- ============================================================================
-- 5. exam_session — shift-aware times
-- ============================================================================
-- Day shift     → 13:00 – 16:00
-- Morning shift → 08:00 – 11:00
-- ============================================================================

INSERT INTO exam_session (
    exam_routine_id, class_id, subject_id, date, group_id,
    start_time, end_time, show_on_admit_card, last_modified_at, is_active
)
WITH ranked_class_subjects AS (
    SELECT
        csg.class_id,
        csg.subject_id,
        ROW_NUMBER() OVER (PARTITION BY csg.class_id ORDER BY csg.subject_id) AS rn
    FROM class_subject_group csg
    WHERE csg.student_group_id IS NULL
      AND csg.is_active = b'1'
),
exam_times AS (
    SELECT 1 AS shift_id, '13:00:00' AS exam_start, '16:00:00' AS exam_end UNION ALL
    SELECT 2,             '08:00:00',               '11:00:00'
)
SELECT
    er.id              AS exam_routine_id,
    rcs.class_id,
    rcs.subject_id,
    DATE_ADD(er.routine_start_date, INTERVAL (rcs.rn - 1) DAY) AS date,
    NULL               AS group_id,
    et.exam_start      AS start_time,
    et.exam_end        AS end_time,
    b'1'               AS show_on_admit_card,
    NOW()              AS last_modified_at,
    b'1'               AS is_active
FROM exam_routine er
CROSS JOIN ranked_class_subjects rcs
JOIN class cl     ON cl.id = rcs.class_id
JOIN exam_times et ON et.shift_id = COALESCE(cl.shift_id, 1);

-- ============================================================================
-- 6. exam_class_room_assignment — one room per (routine × class)
-- ============================================================================
INSERT INTO exam_class_room_assignment (
    exam_routine_id, class_id, room_id, start_roll, end_roll
)
SELECT
    er.id                              AS exam_routine_id,
    c.id                               AS class_id,
    ((c.id - 1) % 5) + 1               AS room_id,
    1                                  AS start_roll,
    150                                AS end_roll
FROM   exam_routine er
CROSS JOIN class c
WHERE  c.is_active = b'1';

-- ============================================================================
-- Verification
-- ============================================================================
SELECT 'exam_type'                    AS tbl, COUNT(*) AS rows_ FROM exam_type UNION ALL
SELECT 'exam_routine',                COUNT(*) FROM exam_routine UNION ALL
SELECT 'exam_session',                COUNT(*) FROM exam_session UNION ALL
SELECT 'exam_class_room_assignment',  COUNT(*) FROM exam_class_room_assignment UNION ALL
SELECT 'class_routine',               COUNT(*) FROM class_routine;

-- Per-class routine slots (should be 90 = 5 days × 6 periods × 3 sections):
SELECT c.name, sh.name AS shift, COUNT(*) AS routine_slots
FROM class c
LEFT JOIN shift sh        ON sh.id = c.shift_id
LEFT JOIN class_routine cr ON cr.class_id = c.id
GROUP BY c.id, c.name, sh.name
ORDER BY c.id;

-- Exam session timings split by shift:
SELECT
    sh.name AS shift,
    MIN(es.start_time) AS earliest_start,
    MAX(es.end_time)   AS latest_end,
    COUNT(*)           AS sessions
FROM exam_session es
JOIN class cl ON cl.id = es.class_id
JOIN shift sh ON sh.id = cl.shift_id
GROUP BY sh.id, sh.name;

-- Class routine first/last bell per shift:
SELECT
    sh.name AS shift,
    MIN(cr.start_time) AS first_bell,
    MAX(cr.end_time)   AS last_bell
FROM class_routine cr
JOIN class cl ON cl.id = cr.class_id
JOIN shift sh ON sh.id = cl.shift_id
GROUP BY sh.id, sh.name;
