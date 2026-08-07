package com.princeramteke.resumeai.analysis.synthesis;

import java.util.List;

/**
 * The parsed, typed shape of the model's JSON output — internal to synthesis and never exposed
 * at the API boundary. Carries the score, a one-line summary, the skill claims (each citing an
 * {@code evidenceRef}), and recommendations. The response {@code evidence[]} array is assembled
 * by the application from real chunks, not taken from the model, so it is deliberately absent here.
 *
 * <p>Null lists are normalized to empty so downstream grounding never sees {@code null}.
 */
public record LlmVerdict(
        int score,
        String summary,
        List<SkillClaim> matchedSkills,
        List<SkillClaim> missingSkills,
        List<SkillClaim> weakSkills,
        List<Recommendation> recommendations
) {
    public LlmVerdict {
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
        missingSkills = missingSkills == null ? List.of() : List.copyOf(missingSkills);
        weakSkills = weakSkills == null ? List.of() : List.copyOf(weakSkills);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    /** A single skill assessment tied to a citation. {@code importance} is HIGH|MEDIUM|LOW. */
    public record SkillClaim(String skill, String importance, String evidenceRef) {
    }

    /** A prioritized improvement suggestion. {@code impact} is HIGH|MEDIUM|LOW. */
    public record Recommendation(String text, String impact, String reason) {
    }
}
