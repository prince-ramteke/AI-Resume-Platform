package com.princeramteke.resumeai.analysis.dto;

/** API shape of a prioritized recommendation. {@code impact} is HIGH|MEDIUM|LOW. */
public record RecommendationResponse(String text, String impact, String reason) {
}
