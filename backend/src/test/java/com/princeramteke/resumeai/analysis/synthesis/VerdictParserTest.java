package com.princeramteke.resumeai.analysis.synthesis;

import com.princeramteke.resumeai.llm.FakeLlmClient;
import com.princeramteke.resumeai.llm.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerdictParserTest {

    private static final String VALID_JSON = """
            {
              "score": 78,
              "summary": "Strong backend match",
              "matchedSkills": [{"skill":"Spring Boot","importance":"HIGH","evidenceRef":"RESUME#2"}],
              "missingSkills": [{"skill":"AWS","importance":"HIGH","evidenceRef":"JD#3"}],
              "weakSkills": [],
              "recommendations": [{"text":"Add cloud experience","impact":"HIGH","reason":"JD lists AWS"}]
            }
            """;

    private VerdictParser parser;

    @BeforeEach
    void setUp() {
        parser = new VerdictParser();
    }

    @Test
    void parse_validJson_returnsTypedVerdict() {
        LlmVerdict verdict = parser.parse(VALID_JSON);

        assertThat(verdict.score()).isEqualTo(78);
        assertThat(verdict.summary()).isEqualTo("Strong backend match");
        assertThat(verdict.matchedSkills()).singleElement()
                .satisfies(s -> {
                    assertThat(s.skill()).isEqualTo("Spring Boot");
                    assertThat(s.importance()).isEqualTo("HIGH");
                    assertThat(s.evidenceRef()).isEqualTo("RESUME#2");
                });
        assertThat(verdict.missingSkills()).hasSize(1);
        assertThat(verdict.recommendations()).singleElement()
                .satisfies(r -> assertThat(r.reason()).isEqualTo("JD lists AWS"));
    }

    @Test
    void parse_fromFakeLlmClient_parsesCannedContent() {
        var llm = new FakeLlmClient(VALID_JSON);

        LlmVerdict verdict = parser.parse(llm.complete(new LlmRequest("sys", "user")).content());

        assertThat(verdict.score()).isEqualTo(78);
    }

    @Test
    void parse_jsonWrappedInCodeFences_repairsAndParses() {
        String fenced = "```json\n" + VALID_JSON + "\n```";

        LlmVerdict verdict = parser.parse(fenced);

        assertThat(verdict.score()).isEqualTo(78);
    }

    @Test
    void parse_jsonSurroundedByProse_repairsAndParses() {
        String noisy = "Here is the analysis you requested:\n" + VALID_JSON + "\nHope this helps!";

        LlmVerdict verdict = parser.parse(noisy);

        assertThat(verdict.score()).isEqualTo(78);
    }

    @Test
    void parse_missingArrays_normalizesToEmptyLists() {
        LlmVerdict verdict = parser.parse("{\"score\":50,\"summary\":\"partial\"}");

        assertThat(verdict.matchedSkills()).isEmpty();
        assertThat(verdict.missingSkills()).isEmpty();
        assertThat(verdict.weakSkills()).isEmpty();
        assertThat(verdict.recommendations()).isEmpty();
    }

    @Test
    void parse_unknownFields_areIgnored() {
        String extra = "{\"score\":60,\"summary\":\"ok\",\"reasoning\":\"chain of thought\",\"confidence\":0.9}";

        LlmVerdict verdict = parser.parse(extra);

        assertThat(verdict.score()).isEqualTo(60);
    }

    @Test
    void parse_nonJson_throwsInvalidVerdictException() {
        assertThatThrownBy(() -> parser.parse("I cannot help with that request."))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void parse_blank_throwsInvalidVerdictException() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parse_brokenJsonAfterRepair_throwsInvalidVerdictException() {
        assertThatThrownBy(() -> parser.parse("prefix { \"score\": 80, \"summary\": broken } suffix"))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("after repair");
    }
}
