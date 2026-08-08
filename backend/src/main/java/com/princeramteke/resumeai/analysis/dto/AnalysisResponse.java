package com.princeramteke.resumeai.analysis.dto;

import java.time.Instant;
import java.util.List;

/**
 * Full analysis result returned by {@code POST /api/analyses} and {@code GET /api/analyses/{id}}
 * (see API.md §5). The {@code evidence} list carries the chunks every {@code evidenceRef} resolves
 * to; {@code provider} records which LLM produced the verdict.
 */
public record AnalysisResponse(
        Long id,
        int score,
        String summary,
        List<SkillResponse> matchedSkills,
        List<SkillResponse> missingSkills,
        List<SkillResponse> weakSkills,
        List<RecommendationResponse> recommendations,
        List<EvidenceResponse> evidence,
        String provider,
        Integer latencyMs,
        Instant createdAt
) {
}
