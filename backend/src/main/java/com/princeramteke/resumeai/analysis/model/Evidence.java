package com.princeramteke.resumeai.analysis.model;

import com.princeramteke.resumeai.rag.chunk.SourceType;

/**
 * Persisted shape of a cited chunk stored in the {@code evidence} JSONB column of
 * {@code analyses} (see DATABASE.md §4). {@code ref} is the citation tag every skill claim
 * resolves to (e.g. {@code "RESUME#2"}); {@code sourceType} distinguishes resume vs job-description
 * chunks. Carries only the snippet — never an embedding vector.
 */
public record Evidence(String ref, SourceType sourceType, int chunkIndex, String snippet) {
}
