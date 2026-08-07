package com.princeramteke.resumeai.analysis.model;

/**
 * Persisted shape of a single skill assessment stored inside the {@code matched_skills},
 * {@code missing_skills}, and {@code weak_skills} JSONB columns of {@code analyses}
 * (see DATABASE.md §4). {@code importance} is HIGH|MEDIUM|LOW; {@code evidenceRef} is a
 * citation tag such as {@code "RESUME#2"} or {@code "JD#3"}.
 */
public record SkillClaim(String skill, String importance, String evidenceRef) {
}
