package com.princeramteke.resumeai.analysis.model;

/**
 * Persisted shape of a prioritized recommendation stored in the {@code recommendations} JSONB
 * column of {@code analyses} (see DATABASE.md §4). {@code impact} is HIGH|MEDIUM|LOW.
 */
public record Recommendation(String text, String impact, String reason) {
}
