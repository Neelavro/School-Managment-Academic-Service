-- fbac_permission.submodule was created on older databases (HR_USER_MANAGEMENT_TABLES.sql)
-- as an 8-value ENUM that predates the current Submodule enum, so saving any current
-- submodule failed with "Data truncated for column 'submodule'".
-- Store it as VARCHAR like user_management.sql does, so new Submodule values need no ALTER.
ALTER TABLE fbac_permission MODIFY submodule VARCHAR(50) NOT NULL;
