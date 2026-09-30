-- ============================================================================
-- Class teachers for classes without sections
-- ============================================================================
-- A class teacher keeps one register: a section, or — when a class has no
-- sections for a gender section — the class + gender section (section_id NULL).
-- Adds class_id and gender_section_id and fills them for existing rows from
-- their section. Attendance itself is per enrollment and date, so it doesn't
-- change.
--
-- Idempotent — safe to re-run.
-- ============================================================================

DROP PROCEDURE IF EXISTS class_teacher_scope;
DELIMITER //
CREATE PROCEDURE class_teacher_scope()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'class_teacher' AND COLUMN_NAME = 'class_id') THEN
    ALTER TABLE class_teacher
      ADD COLUMN class_id          INT NULL AFTER staff_id,
      ADD COLUMN gender_section_id INT NULL AFTER class_id,
      ADD KEY idx_ct_class_gender (class_id, gender_section_id, academic_year_id),
      ADD CONSTRAINT fk_ct_class FOREIGN KEY (class_id) REFERENCES class(id) ON DELETE CASCADE,
      ADD CONSTRAINT fk_ct_gender_section FOREIGN KEY (gender_section_id) REFERENCES gender_section(id) ON DELETE SET NULL;
  END IF;
END //
DELIMITER ;
CALL class_teacher_scope();
DROP PROCEDURE class_teacher_scope;

-- Databases created from src/main/resources/schema.sql have section_id NOT NULL; a class + gender section
-- register has none. (Re-running this is harmless.)
ALTER TABLE class_teacher MODIFY section_id BIGINT NULL;

-- Existing class teachers are all section registers: take the class and gender section from the section.
UPDATE class_teacher ct
JOIN section s ON s.id = ct.section_id
SET ct.class_id = s.class_id, ct.gender_section_id = s.gender_id
WHERE ct.class_id IS NULL;

-- Check: every class teacher should now have a class.
SELECT COUNT(*) AS class_teachers_without_class FROM class_teacher WHERE class_id IS NULL;
