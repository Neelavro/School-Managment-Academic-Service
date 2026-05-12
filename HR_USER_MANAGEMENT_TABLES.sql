-- ============================================================
-- HR & User Management — New Tables with Indexes
-- ============================================================

-- 1. Designation (HR-created titles: Junior Teacher, Janitor, etc.)
CREATE TABLE designation (
    id          INT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active   BIT DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 2. Designation Promotion Matrix (allowed career paths)
CREATE TABLE designation_promotion (
    id                   INT NOT NULL AUTO_INCREMENT,
    from_designation_id  INT NOT NULL,
    to_designation_id    INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_promotion_path (from_designation_id, to_designation_id),
    CONSTRAINT fk_dp_from FOREIGN KEY (from_designation_id) REFERENCES designation(id),
    CONSTRAINT fk_dp_to   FOREIGN KEY (to_designation_id)   REFERENCES designation(id)
) ENGINE=InnoDB;

-- 3. Staff (main HR profile)
CREATE TABLE staff (
    id                     BIGINT NOT NULL AUTO_INCREMENT,
    staff_system_id        VARCHAR(50)  UNIQUE,
    name_english           VARCHAR(200) NOT NULL,
    name_bangla            VARCHAR(200),
    employee_type          ENUM('TEACHING','NON_TEACHING','ADMIN','SUPPORT') NOT NULL,
    current_designation_id INT,
    joining_date           DATE,
    dob                    DATE,
    phone                  VARCHAR(20),
    email                  VARCHAR(200),
    national_id            VARCHAR(50),
    address                TEXT,
    is_active              BIT DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_staff_designation FOREIGN KEY (current_designation_id) REFERENCES designation(id)
) ENGINE=InnoDB;

CREATE INDEX idx_staff_system_id    ON staff (staff_system_id);
CREATE INDEX idx_staff_employee_type ON staff (employee_type);
CREATE INDEX idx_staff_active        ON staff (is_active);

-- 4. Staff Promotion History
CREATE TABLE staff_promotion (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    staff_id             BIGINT NOT NULL,
    from_designation_id  INT,
    to_designation_id    INT NOT NULL,
    promotion_date       DATE NOT NULL,
    notes                TEXT,
    recorded_at          DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_sp_staff FOREIGN KEY (staff_id)            REFERENCES staff(id),
    CONSTRAINT fk_sp_from  FOREIGN KEY (from_designation_id) REFERENCES designation(id),
    CONSTRAINT fk_sp_to    FOREIGN KEY (to_designation_id)   REFERENCES designation(id)
) ENGINE=InnoDB;

CREATE INDEX idx_staff_promotion_staff ON staff_promotion (staff_id);

-- 5. Staff Document (academic credentials, certificates)
CREATE TABLE staff_document (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    staff_id      BIGINT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    uploaded_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_sdoc_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;

CREATE INDEX idx_staff_document_staff ON staff_document (staff_id);

-- 6. Staff Emergency Contact
CREATE TABLE staff_emergency_contact (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT NOT NULL,
    name         VARCHAR(200) NOT NULL,
    relationship VARCHAR(100),
    phone        VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sec_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;

CREATE INDEX idx_emergency_contact_staff ON staff_emergency_contact (staff_id);

-- 7. Staff Duty Role (configurable: Dorm Tutor, Dorm Head, Supervisor, etc.)
CREATE TABLE staff_duty_role (
    id        INT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(150) NOT NULL UNIQUE,
    is_active BIT DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 8. Staff Duty Role Assignment (staff <-> duty role many-to-many)
CREATE TABLE staff_duty_role_assignment (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT NOT NULL,
    duty_role_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_staff_duty (staff_id, duty_role_id),
    CONSTRAINT fk_sdra_staff FOREIGN KEY (staff_id)     REFERENCES staff(id),
    CONSTRAINT fk_sdra_role  FOREIGN KEY (duty_role_id) REFERENCES staff_duty_role(id)
) ENGINE=InnoDB;

CREATE INDEX idx_duty_role_assignment_staff ON staff_duty_role_assignment (staff_id);

-- 9. Staff Class Assignment (teacher -> class/gender section/section/group/subject per year)
CREATE TABLE staff_class_assignment (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    staff_id          BIGINT NOT NULL,
    academic_year_id  INT NOT NULL,
    class_id          INT NOT NULL,
    gender_section_id INT,
    section_id        BIGINT,
    student_group_id  INT,
    subject_id        INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sca_staff    FOREIGN KEY (staff_id)         REFERENCES staff(id),
    CONSTRAINT fk_sca_year     FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT fk_sca_class    FOREIGN KEY (class_id)         REFERENCES class(id),
    CONSTRAINT fk_sca_subject  FOREIGN KEY (subject_id)       REFERENCES subject(id)
) ENGINE=InnoDB;

CREATE INDEX idx_sca_staff_year ON staff_class_assignment (staff_id, academic_year_id);
CREATE INDEX idx_sca_class_year ON staff_class_assignment (class_id, academic_year_id);

-- 10. Leave Type (Casual, Sick, Maternity, Academic Seminar, custom)
CREATE TABLE leave_type (
    id            INT NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL UNIQUE,
    annual_quota  INT,
    is_active     BIT DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 11. FBAC Role (system access roles: Fee Collector, Exam Controller, etc.)
CREATE TABLE fbac_role (
    id          INT NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active   BIT DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 12. Leave Approval Tier (configurable per leave type)
CREATE TABLE leave_approval_tier (
    id            INT NOT NULL AUTO_INCREMENT,
    leave_type_id INT NOT NULL,
    tier_order    INT NOT NULL,
    tier_label    VARCHAR(150) NOT NULL,
    fbac_role_id  INT,                          -- NULL = any admin can approve
    PRIMARY KEY (id),
    CONSTRAINT fk_lat_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(id),
    CONSTRAINT fk_lat_fbac_role  FOREIGN KEY (fbac_role_id)  REFERENCES fbac_role(id)
) ENGINE=InnoDB;

CREATE INDEX idx_leave_tier_type ON leave_approval_tier (leave_type_id);

-- 13. Leave Request
CREATE TABLE leave_request (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    staff_id           BIGINT NOT NULL,
    leave_type_id      INT NOT NULL,
    start_date         DATE NOT NULL,
    end_date           DATE NOT NULL,
    reason             TEXT,
    status             ENUM('PENDING','PARTIALLY_APPROVED','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    current_tier_order INT DEFAULT 1,
    submitted_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_lr_staff      FOREIGN KEY (staff_id)      REFERENCES staff(id),
    CONSTRAINT fk_lr_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(id)
) ENGINE=InnoDB;

CREATE INDEX idx_leave_request_staff  ON leave_request (staff_id);
CREATE INDEX idx_leave_request_status ON leave_request (status);

-- 14. Leave Approval Record (append-only audit of each tier action)
CREATE TABLE leave_approval_record (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    leave_request_id BIGINT NOT NULL,
    tier_order       INT NOT NULL,
    approved_by_user_id BIGINT,
    action           ENUM('APPROVED','REJECTED') NOT NULL,
    remarks          VARCHAR(500),
    action_at        DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_lar_request FOREIGN KEY (leave_request_id) REFERENCES leave_request(id)
) ENGINE=InnoDB;

CREATE INDEX idx_leave_approval_request ON leave_approval_record (leave_request_id);

-- 15. FBAC Permission Matrix (role -> submodule CRUD flags)
CREATE TABLE fbac_permission (
    id           INT NOT NULL AUTO_INCREMENT,
    fbac_role_id INT NOT NULL,
    submodule    ENUM('STUDENT_MANAGEMENT','ACADEMIC_STRUCTURE','EXAM_SETUP',
                      'EXAM_OPERATIONS','RESULTS_REPORTS','HR_MANAGEMENT',
                      'USER_MANAGEMENT','SYSTEM_SETTINGS') NOT NULL,
    can_create   BIT DEFAULT 0,
    can_read     BIT DEFAULT 0,
    can_update   BIT DEFAULT 0,
    can_delete   BIT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_role_submodule (fbac_role_id, submodule),
    CONSTRAINT fk_fp_role FOREIGN KEY (fbac_role_id) REFERENCES fbac_role(id)
) ENGINE=InnoDB;

CREATE INDEX idx_fbac_permission_role ON fbac_permission (fbac_role_id);

-- 16. System User (login credentials for admin/teacher portals)
CREATE TABLE system_user (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    phone               VARCHAR(20)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    user_type           ENUM('SUPER_ADMIN','ADMIN','TEACHER','STUDENT') NOT NULL,
    staff_id            BIGINT,                -- links to staff record (nullable for students)
    is_active           BIT DEFAULT 1,
    is_suspended        BIT DEFAULT 0,
    must_reset_password BIT DEFAULT 0,
    last_login_at       DATETIME(6),
    created_at          DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_system_user_phone  ON system_user (phone);
CREATE INDEX idx_system_user_staff  ON system_user (staff_id);
CREATE INDEX idx_system_user_type   ON system_user (user_type);

-- 17. System User Role (user <-> FBAC role many-to-many)
CREATE TABLE system_user_role (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    system_user_id BIGINT NOT NULL,
    fbac_role_id   INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_role (system_user_id, fbac_role_id),
    CONSTRAINT fk_sur_user FOREIGN KEY (system_user_id) REFERENCES system_user(id),
    CONSTRAINT fk_sur_role FOREIGN KEY (fbac_role_id)   REFERENCES fbac_role(id)
) ENGINE=InnoDB;

CREATE INDEX idx_user_role_user ON system_user_role (system_user_id);

-- 18. Audit Log (append-only, CUD actions only)
CREATE TABLE audit_log (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    ip_address  VARCHAR(50),
    action_type ENUM('CREATE','UPDATE','DELETE') NOT NULL,
    submodule   ENUM('STUDENT_MANAGEMENT','ACADEMIC_STRUCTURE','EXAM_SETUP',
                     'EXAM_OPERATIONS','RESULTS_REPORTS','HR_MANAGEMENT',
                     'USER_MANAGEMENT','SYSTEM_SETTINGS'),
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    description TEXT,
    timestamp   DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_user      ON audit_log (user_id);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp);
CREATE INDEX idx_audit_submodule ON audit_log (submodule);

-- ============================================================
-- Staff Extended Fields — alter existing staff table
-- (adds all new columns from the onboarding form expansion)
-- ============================================================
ALTER TABLE staff
    ADD COLUMN father_name          VARCHAR(200),
    ADD COLUMN mother_name          VARCHAR(200),
    ADD COLUMN spouse_name          VARCHAR(200),
    ADD COLUMN gender_id            INT,
    ADD COLUMN religion             VARCHAR(100),
    ADD COLUMN marital_status       VARCHAR(50),
    ADD COLUMN blood_group          VARCHAR(10),
    ADD COLUMN nationality          VARCHAR(100),
    ADD COLUMN smart_card_number    VARCHAR(50),
    ADD COLUMN birth_reg_number     VARCHAR(50),
    ADD COLUMN present_division     VARCHAR(100),
    ADD COLUMN present_district     VARCHAR(100),
    ADD COLUMN present_upazila      VARCHAR(100),
    ADD COLUMN present_union        VARCHAR(100),
    ADD COLUMN present_post_office  VARCHAR(100),
    ADD COLUMN present_village      VARCHAR(200),
    ADD COLUMN permanent_division   VARCHAR(100),
    ADD COLUMN permanent_district   VARCHAR(100),
    ADD COLUMN permanent_upazila    VARCHAR(100),
    ADD COLUMN permanent_union      VARCHAR(100),
    ADD COLUMN permanent_post_office VARCHAR(100),
    ADD COLUMN permanent_village    VARCHAR(200),
    ADD COLUMN contract_type        ENUM('PERMANENT','CONTRACTUAL','PART_TIME','PROBATIONARY'),
    ADD COLUMN department           VARCHAR(150),
    ADD COLUMN bank_name            VARCHAR(200),
    ADD COLUMN bank_branch          VARCHAR(200),
    ADD COLUMN bank_account_number  VARCHAR(50),
    ADD COLUMN bank_routing_number  VARCHAR(50),
    ADD COLUMN bkash_number         VARCHAR(20),
    ADD COLUMN e_tin                VARCHAR(20);

-- Drop the old single-string address column (now replaced by structured fields)
ALTER TABLE staff DROP COLUMN address;

-- 19. Academic Qualification (education history — teachers primarily, optional for others)
CREATE TABLE academic_qualification (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    staff_id            BIGINT NOT NULL,
    level               ENUM('SSC_DAKHIL','HSC_ALIM','BACHELOR_FAZIL','MASTERS_KAMIL',
                             'HIFZ_QIRAT','DAURA_TAKMIL','OTHER') NOT NULL,
    institute_name      VARCHAR(300),
    board               VARCHAR(150),
    group_or_subject    VARCHAR(150),
    passing_year        INT,
    result              VARCHAR(50),
    roll_number         VARCHAR(50),
    registration_number VARCHAR(50),
    sanad_details       VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_aq_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;

CREATE INDEX idx_aq_staff ON academic_qualification (staff_id);

-- 20. Teacher Profile (MPO, NTRCA, experience — TEACHING staff only)
CREATE TABLE teacher_profile (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    staff_id              BIGINT NOT NULL UNIQUE,
    mpo_status            ENUM('MPO_LISTED','NON_MPO'),
    mpo_index_number      VARCHAR(100),
    ntrca_reg_number      VARCHAR(100),
    ntrca_cycle           VARCHAR(50),
    years_of_experience   INT,
    previous_institutions TEXT,
    trainings_completed   TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_tp_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;

-- 21. Staff Dependent (spouse, children — for dependent records and stipends)
CREATE TABLE staff_dependent (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    staff_id     BIGINT NOT NULL,
    name         VARCHAR(200) NOT NULL,
    relationship VARCHAR(50)  NOT NULL,
    dob          DATE,
    nid_number   VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT fk_sd_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;

CREATE INDEX idx_staff_dependent_staff ON staff_dependent (staff_id);
