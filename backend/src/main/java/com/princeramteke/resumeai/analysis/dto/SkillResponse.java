package com.princeramteke.resumeai.analysis.dto;

/** API shape of a skill assessment. {@code importance} is HIGH|MEDIUM|LOW. */
public record SkillResponse(String skill, String importance, String evidenceRef) {
}
