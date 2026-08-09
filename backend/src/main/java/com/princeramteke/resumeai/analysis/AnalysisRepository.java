package com.princeramteke.resumeai.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Data access for {@code analyses}. Analyses are never soft-deleted (no {@code deleted} flag),
 * so ownership finders are plain. The list query eagerly fetches {@code jobDescription} via an
 * entity graph so the summary's {@code jobTitle} costs no extra per-row query (avoids N+1).
 */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "jobDescription")
    Page<Analysis> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Result cache lookup (v1.1): returns the most recent analysis for this
     * {@code (userId, resumeId, jobDescriptionId)} tuple whose {@code createdAt}
     * is at or after {@code freshnessThreshold} — i.e. the row was produced
     * after the last modification of either underlying document.
     *
     * <p>Ownership is baked into the query ({@code userId} in the WHERE clause),
     * so a caller who does not own either resource can never receive another
     * user's cached row. Backed by
     * {@code idx_analyses_cache_lookup (user_id, resume_id, job_description_id,
     * created_at DESC)}; see {@code V4__analysis_cache_index.sql}.
     */
    Optional<Analysis> findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId, Long resumeId, Long jobDescriptionId, Instant freshnessThreshold);
}
