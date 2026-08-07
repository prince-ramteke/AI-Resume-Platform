package com.princeramteke.resumeai.analysis.synthesis;

import com.princeramteke.resumeai.analysis.synthesis.LlmVerdict.Recommendation;
import com.princeramteke.resumeai.analysis.synthesis.LlmVerdict.SkillClaim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutputValidatorTest {

    private static final Set<String> VALID_REFS = Set.of("RESUME#2", "RESUME#5", "JD#3");

    private OutputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OutputValidator();
    }

    private LlmVerdict verdict(int score, List<SkillClaim> matched, List<SkillClaim> missing) {
        return new LlmVerdict(score, "summary", matched, missing, List.of(), List.of());
    }

    private SkillClaim skill(String name, String ref) {
        return new SkillClaim(name, "HIGH", ref);
    }

    @Test
    void validate_scoreAboveRange_clampsTo100() {
        LlmVerdict result = validator.validate(
                verdict(150, List.of(skill("Spring", "RESUME#2")), List.of()), VALID_REFS);

        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void validate_negativeScore_clampsTo0() {
        LlmVerdict result = validator.validate(
                verdict(-10, List.of(), List.of()), VALID_REFS);

        assertThat(result.score()).isZero();
    }

    @Test
    void validate_danglingEvidenceRef_isDropped() {
        LlmVerdict result = validator.validate(
                verdict(70, List.of(skill("Spring", "RESUME#2"), skill("Ghost", "RESUME#99")), List.of()),
                VALID_REFS);

        assertThat(result.matchedSkills()).singleElement()
                .satisfies(s -> assertThat(s.skill()).isEqualTo("Spring"));
    }

    @Test
    void validate_nullEvidenceRef_isDropped() {
        LlmVerdict result = validator.validate(
                verdict(70, List.of(skill("Spring", "RESUME#2"), skill("NoRef", null)), List.of()),
                VALID_REFS);

        assertThat(result.matchedSkills()).hasSize(1);
    }

    @Test
    void validate_groundedVerdict_isReturnedUnchanged() {
        LlmVerdict input = new LlmVerdict(78, "Strong match",
                List.of(skill("Spring", "RESUME#2")),
                List.of(skill("AWS", "JD#3")),
                List.of(skill("Testing", "RESUME#5")),
                List.of(new Recommendation("Add cloud", "HIGH", "JD lists AWS")));

        LlmVerdict result = validator.validate(input, VALID_REFS);

        assertThat(result.score()).isEqualTo(78);
        assertThat(result.matchedSkills()).hasSize(1);
        assertThat(result.missingSkills()).hasSize(1);
        assertThat(result.weakSkills()).hasSize(1);
        assertThat(result.recommendations()).hasSize(1);
    }

    @Test
    void validate_missingSkillGroundedInJdRef_isKept() {
        LlmVerdict result = validator.validate(
                verdict(40, List.of(), List.of(skill("AWS", "JD#3"))), VALID_REFS);

        assertThat(result.missingSkills()).singleElement()
                .satisfies(s -> assertThat(s.evidenceRef()).isEqualTo("JD#3"));
    }

    @Test
    void validate_bracketedEvidenceRef_isNormalizedAndGrounded() {
        // a model that echoes the citation tag exactly as shown in the prompt ("[RESUME#0]")
        LlmVerdict result = validator.validate(
                verdict(80, List.of(skill("Spring", "[RESUME#2]")), List.of(skill("AWS", "[JD#3]"))),
                VALID_REFS);

        assertThat(result.matchedSkills()).singleElement().satisfies(s -> {
            assertThat(s.skill()).isEqualTo("Spring");
            assertThat(s.evidenceRef()).isEqualTo("RESUME#2"); // stored canonical, unbracketed
        });
        assertThat(result.missingSkills()).singleElement()
                .satisfies(s -> assertThat(s.evidenceRef()).isEqualTo("JD#3"));
    }

    @Test
    void validate_highScoreWithNoGroundedMatch_isRejected() {
        LlmVerdict injected = verdict(100, List.of(skill("Everything", "RESUME#999")), List.of());

        assertThatThrownBy(() -> validator.validate(injected, VALID_REFS))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("no grounded matched skills");
    }

    @Test
    void validate_lowScoreWithNoMatch_isAllowed() {
        LlmVerdict result = validator.validate(verdict(20, List.of(), List.of()), VALID_REFS);

        assertThat(result.score()).isEqualTo(20);
        assertThat(result.matchedSkills()).isEmpty();
    }
}
