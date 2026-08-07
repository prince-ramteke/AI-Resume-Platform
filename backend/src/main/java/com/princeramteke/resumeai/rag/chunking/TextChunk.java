package com.princeramteke.resumeai.rag.chunking;

/**
 * A single ordered slice of a document's text. {@code index} is 0-based and
 * preserves chunk order for later retrieval and evidence referencing.
 */
public record TextChunk(int index, String content) {
}
