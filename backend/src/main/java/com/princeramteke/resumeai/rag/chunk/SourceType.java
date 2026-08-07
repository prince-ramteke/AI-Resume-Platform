package com.princeramteke.resumeai.rag.chunk;

/**
 * Identifies which kind of document a {@link DocumentChunk} was derived from.
 * Stored as its {@code name()} in the {@code source_type} column (RESUME | JD).
 */
public enum SourceType {
    RESUME,
    JD
}
