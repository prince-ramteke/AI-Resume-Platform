package com.princeramteke.resumeai.rag.chunking;

import com.princeramteke.resumeai.rag.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits document text into deterministic, overlapping, size-bounded chunks.
 *
 * <p>Uses a sliding character window of {@code chunkSize} with {@code chunkOverlap}
 * carried between consecutive chunks (both from {@link RagConfig}). The window end is
 * snapped back to the nearest whitespace in its final segment so words are not split
 * mid-token; if none is found it falls back to a hard cut. Output is fully deterministic:
 * the same input always yields the same chunks.
 */
@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    /** Fraction of the window that must be retained before snapping to a word boundary. */
    private static final double MIN_BOUNDARY_RATIO = 0.8;

    private final RagConfig ragConfig;

    public TextChunker(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.strip();
        int size = ragConfig.chunkSize();
        int overlap = ragConfig.chunkOverlap();
        int length = normalized.length();

        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;
        while (start < length) {
            int end = Math.min(start + size, length);
            if (end < length) {
                end = snapToBoundary(normalized, start, end);
            }

            String content = normalized.substring(start, end).strip();
            if (!content.isBlank()) {
                chunks.add(new TextChunk(index++, content));
            }

            if (end >= length) {
                break;
            }
            int nextStart = end - overlap;
            start = (nextStart <= start) ? start + 1 : nextStart;
        }

        log.debug("Chunked text: chars={}, chunks={}", length, chunks.size());
        return chunks;
    }

    /**
     * Moves the window end back to the last whitespace in its final segment so a word is
     * not split. Returns the original end (hard cut) if no suitable whitespace is found.
     */
    private int snapToBoundary(String text, int start, int end) {
        int minAcceptable = start + (int) ((end - start) * MIN_BOUNDARY_RATIO);
        for (int i = end; i > minAcceptable; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return end;
    }
}
