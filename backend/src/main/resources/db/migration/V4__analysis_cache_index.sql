-- v1.1 — analysis result caching.
--
-- Composite index backing the cache lookup in AnalysisRepository:
--   findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc
--
-- Ordering matches the WHERE + ORDER BY, so the planner walks straight to the
-- newest row for the tuple. The existing idx_analyses_user (user_id, created_at
-- DESC) still serves the paginated history query; the two do not overlap.
CREATE INDEX idx_analyses_cache_lookup
    ON analyses (user_id, resume_id, job_description_id, created_at DESC);
