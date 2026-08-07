package com.princeramteke.resumeai.analysis.synthesis;

import com.princeramteke.resumeai.analysis.synthesis.LlmVerdict.SkillClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Grounds and sanitizes a parsed {@link LlmVerdict} against the set of citation tags that
 * actually exist ({@code validRefs}). Implements the AI-safety rules from SECURITY.md §5:
 *
 * <ul>
 *   <li>the score is clamped into [0, 100];</li>
 *   <li>any skill claim whose {@code evidenceRef} does not resolve to a real tag is dropped
 *       (dangling / hallucinated references are removed, not trusted);</li>
 *   <li>a structurally impossible verdict — a passing score with no grounded matched skills —
 *       is rejected outright, defeating a prompt-injection attempt to inflate the score.</li>
 * </ul>
 *
 * <p>Pure and side-effect free: given the same inputs it returns the same cleaned verdict (or
 * throws). Recommendations carry no citation and are passed through unchanged.
 */
@Component
public class OutputValidator {

    private static final Logger log = LoggerFactory.getLogger(OutputValidator.class);

    /** A match this strong cannot be justified without at least one grounded matched skill. */
    private static final int MIN_SCORE_REQUIRING_MATCH = 50;

    public LlmVerdict validate(LlmVerdict verdict, Set<String> validRefs) {
        int score = clamp(verdict.score());

        List<SkillClaim> matched = ground(verdict.matchedSkills(), validRefs);
        List<SkillClaim> missing = ground(verdict.missingSkills(), validRefs);
        List<SkillClaim> weak = ground(verdict.weakSkills(), validRefs);

        if (matched.isEmpty() && score >= MIN_SCORE_REQUIRING_MATCH) {
            throw new InvalidVerdictException(
                    "Unsupported verdict: score " + score + " with no grounded matched skills");
        }

        return new LlmVerdict(score, verdict.summary(), matched, missing, weak, verdict.recommendations());
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private List<SkillClaim> ground(List<SkillClaim> claims, Set<String> validRefs) {
        List<SkillClaim> kept = new ArrayList<>();
        for (SkillClaim c : claims) {
            String ref = normalizeRef(c.evidenceRef());
            if (ref != null && validRefs.contains(ref)) {
                // store the canonical, unbracketed ref so it resolves against the evidence[] entries
                kept.add(new SkillClaim(c.skill(), c.importance(), ref));
            }
        }
        int dropped = claims.size() - kept.size();
        if (dropped > 0) {
            log.info("Output validation dropped {} skill claim(s) with unresolved evidence refs", dropped);
        }
        return kept;
    }

    /**
     * Strip one outer pair of square brackets so a model that echoes the citation tag exactly as
     * shown in the prompt ({@code [RESUME#0]}) resolves to the canonical stored ref
     * ({@code RESUME#0}). Un-bracketed refs are returned unchanged (trimmed).
     */
    private String normalizeRef(String ref) {
        if (ref == null) {
            return null;
        }
        String trimmed = ref.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
