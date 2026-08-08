package com.princeramteke.resumeai.analysis.synthesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Parses the raw model text into a typed {@link LlmVerdict}. Accepts JSON only. On a first parse
 * failure it makes exactly ONE repair attempt — extracting the outermost {@code { ... }} object,
 * which strips common wrappers like code fences or surrounding prose — then parses that. If both
 * attempts fail the input is rejected with {@link InvalidVerdictException}; a malformed structure
 * is never silently ignored or coerced into an empty result.
 *
 * <p>Unknown fields are tolerated (models often add extra keys) but a missing/blank body or a
 * non-JSON payload is a hard failure. The re-prompt retry against the model is a separate, higher
 * level concern (the analysis service), not this parser's job.
 */
@Component
public class VerdictParser {

    private static final Logger log = LoggerFactory.getLogger(VerdictParser.class);

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    public LlmVerdict parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidVerdictException("LLM returned empty content");
        }
        try {
            return objectMapper.readValue(raw, LlmVerdict.class);
        } catch (JsonProcessingException first) {
            String repaired = extractJsonObject(raw);
            if (repaired == null) {
                throw new InvalidVerdictException("LLM output is not valid JSON", first);
            }
            log.info("Verdict parse: strict parse failed, attempting one JSON-object repair");
            try {
                return objectMapper.readValue(repaired, LlmVerdict.class);
            } catch (JsonProcessingException second) {
                throw new InvalidVerdictException("LLM output is not valid JSON after repair", second);
            }
        }
    }

    /** Extract the outermost JSON object, or {@code null} if there is no plausible object. */
    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return raw.substring(start, end + 1);
    }
}
