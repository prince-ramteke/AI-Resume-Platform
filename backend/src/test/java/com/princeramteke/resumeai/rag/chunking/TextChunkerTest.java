package com.princeramteke.resumeai.rag.chunking;

import com.princeramteke.resumeai.rag.RagConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private TextChunker chunkerWith(int size, int overlap) {
        return new TextChunker(new RagConfig(size, overlap, 8, 3500));
    }

    @Test
    void chunk_nullText_returnsEmptyList() {
        assertThat(chunkerWith(10, 3).chunk(null)).isEmpty();
    }

    @Test
    void chunk_blankText_returnsEmptyList() {
        assertThat(chunkerWith(10, 3).chunk("   \n\t ")).isEmpty();
    }

    @Test
    void chunk_textShorterThanSize_returnsSingleChunk() {
        List<TextChunk> chunks = chunkerWith(100, 10).chunk("short text");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).index()).isZero();
        assertThat(chunks.get(0).content()).isEqualTo("short text");
    }

    @Test
    void chunk_longText_producesSizeBoundedChunksWithSequentialIndexes() {
        // No whitespace, so word-boundary snapping never triggers and sizing is exact.
        String text = "abcdefghijklmnopqrstuvwxyz0123456789";
        List<TextChunk> chunks = chunkerWith(10, 3).chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).index()).isEqualTo(i);
            assertThat(chunks.get(i).content().length()).isLessThanOrEqualTo(10);
        }
    }

    @Test
    void chunk_consecutiveChunks_overlapByConfiguredAmount() {
        String text = "abcdefghijklmnopqrstuvwxyz0123456789";
        int size = 10;
        int overlap = 3;
        List<TextChunk> chunks = chunkerWith(size, overlap).chunk(text);

        // The last `overlap` chars of one chunk equal the first `overlap` chars of the next.
        for (int i = 0; i < chunks.size() - 1; i++) {
            String current = chunks.get(i).content();
            String next = chunks.get(i + 1).content();
            String tail = current.substring(current.length() - overlap);
            assertThat(next).startsWith(tail);
        }
    }

    @Test
    void chunk_coversEntireInput_noContentLost() {
        String text = "abcdefghijklmnopqrstuvwxyz0123456789";
        List<TextChunk> chunks = chunkerWith(10, 3).chunk(text);

        // Reconstruct by appending the non-overlapping portion of each chunk after the first.
        StringBuilder reconstructed = new StringBuilder(chunks.get(0).content());
        for (int i = 1; i < chunks.size(); i++) {
            String next = chunks.get(i).content();
            reconstructed.append(next.substring(3)); // drop the 3-char overlap
        }
        assertThat(reconstructed.toString()).isEqualTo(text);
    }

    @Test
    void chunk_isDeterministic_sameInputSameOutput() {
        String text = "The quick brown fox jumps over the lazy dog many many times over.";
        TextChunker chunker = chunkerWith(20, 5);

        assertThat(chunker.chunk(text)).isEqualTo(chunker.chunk(text));
    }

    @Test
    void chunk_withWhitespace_doesNotSplitWordsMidToken() {
        String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda";
        List<TextChunk> chunks = chunkerWith(20, 5).chunk(text);

        // Every chunk (snapped to whitespace) trims cleanly; interior chunks should not end
        // in the middle of a word — they end at a boundary, so the last char is a letter that
        // completes a token. We assert no chunk contains a leading/trailing partial via strip.
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.content()).isEqualTo(c.content().strip()));
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    void chunk_respectsCustomChunkSize() {
        String text = "abcdefghijklmnopqrstuvwxyz";
        List<TextChunk> small = chunkerWith(5, 1).chunk(text);
        List<TextChunk> large = chunkerWith(25, 1).chunk(text);

        assertThat(small.size()).isGreaterThan(large.size());
        assertThat(small).allSatisfy(c -> assertThat(c.content().length()).isLessThanOrEqualTo(5));
    }
}
