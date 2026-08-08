package com.princeramteke.resumeai.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
