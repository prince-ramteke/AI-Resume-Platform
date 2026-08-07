package com.princeramteke.resumeai.analysis.dto;

import com.princeramteke.resumeai.rag.chunk.SourceType;

/** API shape of a cited chunk. {@code ref} is the citation tag skill claims resolve to. */
public record EvidenceResponse(String ref, SourceType sourceType, int chunkIndex, String snippet) {
}
