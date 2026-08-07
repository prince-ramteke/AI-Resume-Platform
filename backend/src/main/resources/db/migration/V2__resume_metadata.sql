-- V2__resume_metadata.sql: Add metadata and soft-delete columns to resumes

ALTER TABLE resumes ADD COLUMN content_type VARCHAR(100);
ALTER TABLE resumes ADD COLUMN file_size BIGINT;
ALTER TABLE resumes ADD COLUMN file_path VARCHAR(500);
ALTER TABLE resumes ADD COLUMN page_count INT;
ALTER TABLE resumes ADD COLUMN language VARCHAR(10);
ALTER TABLE resumes ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE resumes ADD COLUMN updated_at TIMESTAMPTZ;

CREATE INDEX idx_resumes_user_active ON resumes (user_id) WHERE deleted = false;
