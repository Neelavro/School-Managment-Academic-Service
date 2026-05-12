-- ============================================================
-- School Management System — academic_service
-- Full Schema: CREATE TABLE + INDEXES
-- MySQL 8.0+  |  charset utf8mb4
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. LOOKUP / REFERENCE TABLES  (no foreign keys)
-- ============================================================

CREATE TABLE IF NOT EXISTS gender (
    id        INT         NOT NULL AUTO_INCREMENT,
    gender    VARCHAR(50) NOT NULL,
    is_active TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_status (
    id          INT         NOT NULL AUTO_INCREMENT,
    status_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_status_name (status_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shift (
    id        INT          NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NOT NULL,
    is_active TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS academic_year (
    id        INT          NOT NULL AUTO_INCREMENT,
    year_name VARCHAR(255),
    is_active TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_group (
    id         INT          NOT NULL AUTO_INCREMENT,
    group_name VARCHAR(255),
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gender_section (
    id          INT          NOT NULL AUTO_INCREMENT,
    gender_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_gender_section_name (gender_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subject (
    id        INT          NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NOT NULL,
    code      VARCHAR(50),
    is_active TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_type (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    order_index INT,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exam_type_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_component (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    order_index INT,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    deleted_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_exam_component_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS room (
    id        INT          NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NOT NULL,
    capacity  INT,
    is_active TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_room_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS designation (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_designation_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_duty_role (
    id        INT          NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(255) NOT NULL,
    is_active TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_staff_duty_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fbac_role (
    id          INT          NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_fbac_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS leave_type (
    id           INT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255) NOT NULL,
    annual_quota INT,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_leave_type_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_settings (
    id               INT          NOT NULL AUTO_INCREMENT,
    institution_name VARCHAR(255),
    address          TEXT,
    logo_url         VARCHAR(255),
    heading          VARCHAR(255),
    signature_url    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. GRADING
-- ============================================================

CREATE TABLE IF NOT EXISTS grading_policies (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NOT NULL,
    is_active TINYINT(1)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_grading_policy_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grades (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    grading_policy_id BIGINT       NOT NULL,
    name              VARCHAR(255) NOT NULL,
    gpa_value         DOUBLE       NOT NULL,
    min_mark          DOUBLE       NOT NULL,
    max_mark          DOUBLE       NOT NULL,
    comment           VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_grade_policy FOREIGN KEY (grading_policy_id)
        REFERENCES grading_policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. CLASS STRUCTURE
-- ============================================================

CREATE TABLE IF NOT EXISTS class (
    id                 INT        NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255),
    shift_id           INT,
    grading_policy_id  BIGINT,
    use_gpa_for_result TINYINT(1) NOT NULL DEFAULT 0,
    order_index        INT,
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_class_shift          FOREIGN KEY (shift_id)          REFERENCES shift (id),
    CONSTRAINT fk_class_grading_policy FOREIGN KEY (grading_policy_id) REFERENCES grading_policies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Junction: class ↔ student_group
CREATE TABLE IF NOT EXISTS class_student_group (
    class_id         INT NOT NULL,
    student_group_id INT NOT NULL,
    PRIMARY KEY (class_id, student_group_id),
    CONSTRAINT fk_csg_class FOREIGN KEY (class_id)         REFERENCES class (id),
    CONSTRAINT fk_csg_group FOREIGN KEY (student_group_id) REFERENCES student_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_csg_group ON class_student_group (student_group_id);

CREATE TABLE IF NOT EXISTS section (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    section_name VARCHAR(255) NOT NULL,
    class_id     INT          NOT NULL,
    gender_id    INT,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_section_class  FOREIGN KEY (class_id)  REFERENCES class (id),
    CONSTRAINT fk_section_gender FOREIGN KEY (gender_id) REFERENCES gender_section (id),
    INDEX idx_section_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- class_id + subject_id + student_group_id uniquely identifies a subject-group assignment
CREATE TABLE IF NOT EXISTS class_subject_group (
    id                INT        NOT NULL AUTO_INCREMENT,
    class_id          INT        NOT NULL,
    subject_id        INT        NOT NULL,
    student_group_id  INT,
    is_active         TINYINT(1) NOT NULL DEFAULT 1,
    is_fourth_subject TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_class_subject_group (class_id, subject_id, student_group_id),
    CONSTRAINT fk_csubg_class   FOREIGN KEY (class_id)         REFERENCES class (id),
    CONSTRAINT fk_csubg_subject FOREIGN KEY (subject_id)       REFERENCES subject (id),
    CONSTRAINT fk_csubg_group   FOREIGN KEY (student_group_id) REFERENCES student_group (id),
    INDEX idx_csubg_class   (class_id),
    INDEX idx_csubg_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. MARKING STRUCTURE
-- ============================================================

CREATE TABLE IF NOT EXISTS marking_structure (
    id               INT        NOT NULL AUTO_INCREMENT,
    exam_type_id     INT        NOT NULL,
    class_id         INT        NOT NULL,
    subject_id       INT        NOT NULL,
    group_id         INT,                   -- NULL = class-wide
    total_marks      INT        NOT NULL,
    pass_marks       INT,
    created_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active        TINYINT(1) NOT NULL DEFAULT 1,
    deleted_at       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_marking_structure (exam_type_id, class_id, subject_id, group_id),
    CONSTRAINT fk_ms_exam_type FOREIGN KEY (exam_type_id) REFERENCES exam_type (id),
    CONSTRAINT fk_ms_class     FOREIGN KEY (class_id)     REFERENCES class (id),
    CONSTRAINT fk_ms_subject   FOREIGN KEY (subject_id)   REFERENCES subject (id),
    CONSTRAINT fk_ms_group     FOREIGN KEY (group_id)     REFERENCES student_group (id),
    INDEX idx_ms_class_group  (class_id, group_id),
    INDEX idx_ms_deleted_at   (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS marking_structure_component (
    id                   INT        NOT NULL AUTO_INCREMENT,
    marking_structure_id INT        NOT NULL,
    exam_component_id    INT        NOT NULL,
    max_marks            INT        NOT NULL,
    pass_marks           INT,
    created_at           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at     DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active            TINYINT(1) NOT NULL DEFAULT 1,
    deleted_at           DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_msc (marking_structure_id, exam_component_id),
    CONSTRAINT fk_msc_structure FOREIGN KEY (marking_structure_id) REFERENCES marking_structure (id),
    CONSTRAINT fk_msc_component FOREIGN KEY (exam_component_id)    REFERENCES exam_component (id),
    INDEX idx_msc_structure (marking_structure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. EXAM ROUTINE & SESSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS exam_routine (
    id               INT          NOT NULL AUTO_INCREMENT,
    title            VARCHAR(255) NOT NULL,
    exam_type_id     INT          NOT NULL,
    academic_year_id INT          NOT NULL,
    status           ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    published_at     DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_er_exam_type     FOREIGN KEY (exam_type_id)     REFERENCES exam_type (id),
    CONSTRAINT fk_er_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_year (id),
    INDEX idx_er_academic_year (academic_year_id),
    INDEX idx_er_exam_type     (exam_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_session (
    id                 INT        NOT NULL AUTO_INCREMENT,
    exam_routine_id    INT        NOT NULL,
    class_id           INT        NOT NULL,
    subject_id         INT        NOT NULL,
    group_id           INT,
    date               DATE,
    start_time         TIME,
    end_time           TIME,
    show_on_admit_card TINYINT(1) NOT NULL DEFAULT 1,
    last_modified_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_es_routine FOREIGN KEY (exam_routine_id) REFERENCES exam_routine (id),
    CONSTRAINT fk_es_class   FOREIGN KEY (class_id)        REFERENCES class (id),
    CONSTRAINT fk_es_subject FOREIGN KEY (subject_id)      REFERENCES subject (id),
    CONSTRAINT fk_es_group   FOREIGN KEY (group_id)        REFERENCES student_group (id),
    INDEX idx_es_routine_active             (exam_routine_id, is_active),
    INDEX idx_es_routine_class_active       (exam_routine_id, class_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_class_room_assignment (
    id              INT NOT NULL AUTO_INCREMENT,
    exam_routine_id INT NOT NULL,
    class_id        INT NOT NULL,
    room_id         INT NOT NULL,
    start_roll      INT,
    end_roll        INT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ecra (exam_routine_id, class_id, room_id),
    CONSTRAINT fk_ecra_routine FOREIGN KEY (exam_routine_id) REFERENCES exam_routine (id),
    CONSTRAINT fk_ecra_class   FOREIGN KEY (class_id)        REFERENCES class (id),
    CONSTRAINT fk_ecra_room    FOREIGN KEY (room_id)         REFERENCES room (id),
    INDEX idx_ecra_routine (exam_routine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. STUDENTS
-- ============================================================

-- Created before student to resolve the circular FK (student.image_id ↔ student_image.student_id).
-- student_image.student_id FK is added via ALTER TABLE after student is created.
CREATE TABLE IF NOT EXISTS student_image (
    id         INT          NOT NULL AUTO_INCREMENT,
    image_url  VARCHAR(255) NOT NULL,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    student_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    student_system_id         VARCHAR(100),
    name_bangla               VARCHAR(255),
    name_english              VARCHAR(255),
    class_roll                INT,
    father_name_bangla        VARCHAR(255),
    father_name_english       VARCHAR(255),
    father_occupation         VARCHAR(255),
    father_phone              VARCHAR(50),
    father_monthly_salary     VARCHAR(50),
    mother_name_bangla        VARCHAR(255),
    mother_name_english       VARCHAR(255),
    mother_occupation         VARCHAR(255),
    mother_phone              VARCHAR(50),
    mother_monthly_salary     VARCHAR(50),
    guardian_name_bangla      VARCHAR(255),
    guardian_name_english     VARCHAR(255),
    guardian_occupation       VARCHAR(255),
    guardian_phone            VARCHAR(50),
    guardian_relation         VARCHAR(100),
    current_holding_no        VARCHAR(255),
    current_road_or_village   VARCHAR(255),
    current_district          VARCHAR(255),
    current_thana             VARCHAR(255),
    permanent_holding_no      VARCHAR(255),
    permanent_road_or_village VARCHAR(255),
    permanent_district        VARCHAR(255),
    permanent_thana           VARCHAR(255),
    dob                       DATE,
    nationality               VARCHAR(100),
    is_active                 TINYINT(1),
    gender_id                 INT,
    student_status_id         INT,
    image_id                  INT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_system_id (student_system_id),
    CONSTRAINT fk_student_gender        FOREIGN KEY (gender_id)         REFERENCES gender (id),
    CONSTRAINT fk_student_status        FOREIGN KEY (student_status_id) REFERENCES student_status (id),
    CONSTRAINT fk_student_image         FOREIGN KEY (image_id)          REFERENCES student_image (id),
    INDEX idx_student_system_id   (student_system_id),
    INDEX idx_student_is_active   (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Resolve circular dependency: link student_image back to student
ALTER TABLE student_image
    ADD CONSTRAINT fk_student_image_student FOREIGN KEY (student_id) REFERENCES student (id);

CREATE INDEX idx_student_image_student ON student_image (student_id);

CREATE TABLE IF NOT EXISTS enrollment (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    student_system_id VARCHAR(100) NOT NULL,
    academic_year_id  INT,
    class_id          INT,
    section_id        BIGINT,
    shift_id          INT,
    gender_section_id INT,
    student_group_id  INT,
    class_roll        INT,
    is_active         TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_enr_student        FOREIGN KEY (student_system_id)  REFERENCES student (student_system_id),
    CONSTRAINT fk_enr_academic_year  FOREIGN KEY (academic_year_id)   REFERENCES academic_year (id),
    CONSTRAINT fk_enr_class          FOREIGN KEY (class_id)           REFERENCES class (id),
    CONSTRAINT fk_enr_section        FOREIGN KEY (section_id)         REFERENCES section (id),
    CONSTRAINT fk_enr_shift          FOREIGN KEY (shift_id)           REFERENCES shift (id),
    CONSTRAINT fk_enr_gender_section FOREIGN KEY (gender_section_id)  REFERENCES gender_section (id),
    CONSTRAINT fk_enr_group          FOREIGN KEY (student_group_id)   REFERENCES student_group (id),
    INDEX idx_enrollment_class_active          (class_id, is_active),
    INDEX idx_enrollment_year_class_active     (academic_year_id, class_id, is_active),
    INDEX idx_enrollment_sysid_active          (student_system_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Marks stored flat: enrollment + routine + subject + component = one row
CREATE TABLE IF NOT EXISTS student_mark (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    enrollment_id     BIGINT       NOT NULL,
    routine_id        INT          NOT NULL,
    subject_id        INT          NOT NULL,
    exam_component_id INT          NOT NULL,
    marks_obtained    DECIMAL(6,2),
    status            VARCHAR(20),           -- NULL / PRESENT / ABSENT / EXPELLED
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sm (enrollment_id, routine_id, subject_id, exam_component_id),
    CONSTRAINT fk_sm_component FOREIGN KEY (exam_component_id) REFERENCES exam_component (id),
    INDEX idx_sm_routine_subject (routine_id, subject_id),
    INDEX idx_sm_enrollment      (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. STAFF / HR
-- ============================================================

CREATE TABLE IF NOT EXISTS staff (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    staff_system_id        VARCHAR(100),
    name_english           VARCHAR(255) NOT NULL,
    name_bangla            VARCHAR(255),
    father_name            VARCHAR(255),
    mother_name            VARCHAR(255),
    spouse_name            VARCHAR(255),
    dob                    DATE,
    gender_id              INT,
    religion               VARCHAR(100),
    marital_status         VARCHAR(50),
    blood_group            VARCHAR(10),
    nationality            VARCHAR(100),
    national_id            VARCHAR(100),
    smart_card_number      VARCHAR(100),
    birth_reg_number       VARCHAR(100),
    phone                  VARCHAR(50),
    email                  VARCHAR(255),
    -- Present address (Bangladesh structure)
    present_division       VARCHAR(100),
    present_district       VARCHAR(100),
    present_upazila        VARCHAR(100),
    present_union          VARCHAR(100),
    present_post_office    VARCHAR(100),
    present_village        VARCHAR(255),
    -- Permanent address
    permanent_division     VARCHAR(100),
    permanent_district     VARCHAR(100),
    permanent_upazila      VARCHAR(100),
    permanent_union        VARCHAR(100),
    permanent_post_office  VARCHAR(100),
    permanent_village      VARCHAR(255),
    -- Employment
    employee_type          ENUM('TEACHING','NON_TEACHING','ADMIN','SUPPORT') NOT NULL,
    contract_type          ENUM('PERMANENT','CONTRACTUAL','PART_TIME','PROBATIONARY'),
    department             VARCHAR(255),
    current_designation_id INT,
    joining_date           DATE,
    -- Financial
    bank_name              VARCHAR(255),
    bank_branch            VARCHAR(255),
    bank_account_number    VARCHAR(100),
    bank_routing_number    VARCHAR(100),
    bkash_number           VARCHAR(50),
    e_tin                  VARCHAR(100),
    is_active              TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_staff_system_id (staff_system_id),
    CONSTRAINT fk_staff_gender      FOREIGN KEY (gender_id)              REFERENCES gender (id),
    CONSTRAINT fk_staff_designation FOREIGN KEY (current_designation_id) REFERENCES designation (id),
    INDEX idx_staff_system_id     (staff_system_id),
    INDEX idx_staff_employee_type (employee_type),
    INDEX idx_staff_active        (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS academic_qualification (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    staff_id            BIGINT NOT NULL,
    level               ENUM('SSC_DAKHIL','HSC_ALIM','BACHELOR_FAZIL','MASTERS_KAMIL',
                             'HIFZ_QIRAT','DAURA_TAKMIL','OTHER') NOT NULL,
    institute_name      VARCHAR(255),
    board               VARCHAR(255),
    group_or_subject    VARCHAR(255),
    passing_year        INT,
    result              VARCHAR(100),
    roll_number         VARCHAR(100),
    registration_number VARCHAR(100),
    sanad_details       VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_aq_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    INDEX idx_aq_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_dependent (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    dob          DATE,
    nid_number   VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_sdep_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    INDEX idx_staff_dependent_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_document (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    staff_id      BIGINT       NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    file_url      VARCHAR(255) NOT NULL,
    uploaded_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_sdoc_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    INDEX idx_staff_document_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_duty_role_assignment (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT NOT NULL,
    duty_role_id INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sdra (staff_id, duty_role_id),
    CONSTRAINT fk_sdra_staff     FOREIGN KEY (staff_id)     REFERENCES staff (id),
    CONSTRAINT fk_sdra_duty_role FOREIGN KEY (duty_role_id) REFERENCES staff_duty_role (id),
    INDEX idx_duty_role_assignment_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_emergency_contact (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    phone        VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sec_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    INDEX idx_emergency_contact_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_promotion (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    staff_id            BIGINT NOT NULL,
    from_designation_id INT,
    to_designation_id   INT    NOT NULL,
    promotion_date      DATE   NOT NULL,
    notes               TEXT,
    recorded_at         DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_sp_staff    FOREIGN KEY (staff_id)            REFERENCES staff (id),
    CONSTRAINT fk_sp_from_des FOREIGN KEY (from_designation_id) REFERENCES designation (id),
    CONSTRAINT fk_sp_to_des   FOREIGN KEY (to_designation_id)   REFERENCES designation (id),
    INDEX idx_staff_promotion_staff (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS designation_promotion (
    id                  INT NOT NULL AUTO_INCREMENT,
    from_designation_id INT NOT NULL,
    to_designation_id   INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_designation_promotion (from_designation_id, to_designation_id),
    CONSTRAINT fk_dp_from FOREIGN KEY (from_designation_id) REFERENCES designation (id),
    CONSTRAINT fk_dp_to   FOREIGN KEY (to_designation_id)   REFERENCES designation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff_class_assignment (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    staff_id          BIGINT NOT NULL,
    academic_year_id  INT    NOT NULL,
    class_id          INT    NOT NULL,
    gender_section_id INT,
    section_id        BIGINT,
    student_group_id  INT,
    subject_id        INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sca_staff          FOREIGN KEY (staff_id)          REFERENCES staff (id),
    CONSTRAINT fk_sca_academic_year  FOREIGN KEY (academic_year_id)  REFERENCES academic_year (id),
    CONSTRAINT fk_sca_class          FOREIGN KEY (class_id)          REFERENCES class (id),
    CONSTRAINT fk_sca_gender_section FOREIGN KEY (gender_section_id) REFERENCES gender_section (id),
    CONSTRAINT fk_sca_section        FOREIGN KEY (section_id)        REFERENCES section (id),
    CONSTRAINT fk_sca_group          FOREIGN KEY (student_group_id)  REFERENCES student_group (id),
    CONSTRAINT fk_sca_subject        FOREIGN KEY (subject_id)        REFERENCES subject (id),
    INDEX idx_sca_staff_year (staff_id, academic_year_id),
    INDEX idx_sca_class_year (class_id, academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS teacher_profile (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    staff_id              BIGINT NOT NULL,
    mpo_status            ENUM('MPO_LISTED','NON_MPO'),
    mpo_index_number      VARCHAR(255),
    ntrca_reg_number      VARCHAR(255),
    ntrca_cycle           VARCHAR(255),
    years_of_experience   INT,
    previous_institutions TEXT,
    trainings_completed   TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_teacher_profile_staff (staff_id),
    CONSTRAINT fk_tp_staff FOREIGN KEY (staff_id) REFERENCES staff (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. USER MANAGEMENT / FBAC PERMISSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS fbac_permission (
    id           INT        NOT NULL AUTO_INCREMENT,
    fbac_role_id INT        NOT NULL,
    submodule    ENUM('STUDENT_MANAGEMENT','ACADEMIC_STRUCTURE','EXAM_SETUP',
                      'EXAM_OPERATIONS','RESULTS_REPORTS','HR_MANAGEMENT',
                      'USER_MANAGEMENT','SYSTEM_SETTINGS') NOT NULL,
    can_create   TINYINT(1) NOT NULL DEFAULT 0,
    can_read     TINYINT(1) NOT NULL DEFAULT 0,
    can_update   TINYINT(1) NOT NULL DEFAULT 0,
    can_delete   TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_fbac_permission (fbac_role_id, submodule),
    CONSTRAINT fk_fp_role FOREIGN KEY (fbac_role_id) REFERENCES fbac_role (id),
    INDEX idx_fbac_permission_role (fbac_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_user (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    phone               VARCHAR(50)  NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    user_type           ENUM('SUPER_ADMIN','ADMIN','TEACHER','STUDENT') NOT NULL,
    staff_id            BIGINT,
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    is_suspended        TINYINT(1)   NOT NULL DEFAULT 0,
    must_reset_password TINYINT(1)   NOT NULL DEFAULT 0,
    last_login_at       DATETIME,
    created_at          DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_system_user_phone (phone),
    INDEX idx_system_user_phone (phone),
    INDEX idx_system_user_staff (staff_id),
    INDEX idx_system_user_type  (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_user_role (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    system_user_id BIGINT NOT NULL,
    fbac_role_id   INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_system_user_role (system_user_id, fbac_role_id),
    CONSTRAINT fk_sur_user FOREIGN KEY (system_user_id) REFERENCES system_user (id),
    CONSTRAINT fk_sur_role FOREIGN KEY (fbac_role_id)   REFERENCES fbac_role (id),
    INDEX idx_user_role_user (system_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. LEAVE MANAGEMENT
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_approval_tier (
    id            INT          NOT NULL AUTO_INCREMENT,
    leave_type_id INT          NOT NULL,
    tier_order    INT          NOT NULL,
    tier_label    VARCHAR(255) NOT NULL,
    fbac_role_id  INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_lat_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type (id),
    CONSTRAINT fk_lat_fbac_role  FOREIGN KEY (fbac_role_id)  REFERENCES fbac_role (id),
    INDEX idx_leave_tier_type (leave_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS leave_request (
    id                 BIGINT     NOT NULL AUTO_INCREMENT,
    staff_id           BIGINT     NOT NULL,
    leave_type_id      INT        NOT NULL,
    start_date         DATE       NOT NULL,
    end_date           DATE       NOT NULL,
    reason             TEXT,
    status             ENUM('PENDING','PARTIALLY_APPROVED','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    current_tier_order INT        NOT NULL DEFAULT 1,
    submitted_at       DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_lr_staff      FOREIGN KEY (staff_id)      REFERENCES staff (id),
    CONSTRAINT fk_lr_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type (id),
    INDEX idx_leave_request_staff  (staff_id),
    INDEX idx_leave_request_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS leave_approval_record (
    id                  BIGINT     NOT NULL AUTO_INCREMENT,
    leave_request_id    BIGINT     NOT NULL,
    tier_order          INT        NOT NULL,
    approved_by_user_id BIGINT,
    action              ENUM('APPROVED','REJECTED') NOT NULL,
    remarks             VARCHAR(255),
    action_at           DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_lar_leave_request FOREIGN KEY (leave_request_id) REFERENCES leave_request (id),
    INDEX idx_leave_approval_request (leave_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 10. AUDIT LOG
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    ip_address  VARCHAR(50),
    action_type ENUM('CREATE','UPDATE','DELETE') NOT NULL,
    submodule   ENUM('STUDENT_MANAGEMENT','ACADEMIC_STRUCTURE','EXAM_SETUP',
                     'EXAM_OPERATIONS','RESULTS_REPORTS','HR_MANAGEMENT',
                     'USER_MANAGEMENT','SYSTEM_SETTINGS'),
    entity_type VARCHAR(255),
    entity_id   VARCHAR(255),
    description TEXT,
    timestamp   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_user      (user_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_submodule (submodule)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
