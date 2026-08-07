package com.princeramteke.resumeai.rag.prompt;

import com.princeramteke.resumeai.rag.retrieval.ChunkEvidence;

import java.util.List;

/**
 * Assembled, token-bounded input for the later analysis engine: the full job-description
 * text plus the resume evidence chunks selected to fit the prompt budget. This is a pure
 * data carrier — building the final model prompt and synthesizing a verdict is M5's job.
 */
public record PromptContext(String jobDescriptionText, List<ChunkEvidence> resumeEvidence) {
}
