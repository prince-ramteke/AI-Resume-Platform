-- V3: Add metadata columns to job_descriptions for file upload support, soft delete, and search
ALTER TABLE job_descriptions ADD COLUMN content_type VARCHAR(100);
ALTER TABLE job_descriptions ADD COLUMN file_size BIGINT;
ALTER TABLE job_descriptions ADD COLUMN file_path VARCHAR(500);
ALTER TABLE job_descriptions ADD COLUMN page_count INT;
ALTER TABLE job_descriptions ADD COLUMN language VARCHAR(10);
ALTER TABLE job_descriptions ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE job_descriptions ADD COLUMN updated_at TIMESTAMPTZ;

CREATE INDEX idx_jd_user_active ON job_descriptions (user_id) WHERE deleted = false;
CREATE INDEX idx_jd_title ON job_descriptions USING gin (to_tsvector('english', title));
