-- ============================================================================
-- Sub-subjects per class (user, 2026-11-12)
-- ============================================================================
-- sub_subject_class   which classes a sub-subject applies to. Classes don't all have the
--                     same parts of a subject, so a parent subject is split only in the
--                     classes its sub-subjects are assigned to; everywhere else it works
--                     as an ordinary subject. Existing sub-subjects start unassigned.
--
-- Idempotent — safe to re-run.
-- ============================================================================

CREATE TABLE IF NOT EXISTS sub_subject_class (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sub_subject_id INT NOT NULL,
    class_id       INT NOT NULL,
    CONSTRAINT uk_sub_subject_class UNIQUE (sub_subject_id, class_id),
    CONSTRAINT fk_ssc_subject FOREIGN KEY (sub_subject_id) REFERENCES subject(id),
    CONSTRAINT fk_ssc_class FOREIGN KEY (class_id) REFERENCES class(id)
);
