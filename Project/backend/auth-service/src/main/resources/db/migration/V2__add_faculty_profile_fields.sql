-- ============================================================
-- Auth Service Database Migration V2: Add Faculty Profile Fields
-- ============================================================

ALTER TABLE users ADD COLUMN employee_id VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN department VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN phone VARCHAR(50) NULL;

