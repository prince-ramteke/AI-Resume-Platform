package com.princeramteke.resumeai.rag.chunk;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA view over the {@code document_chunks} table (created in V1). The {@code embedding}
 * (pgvector) and {@code metadata} (jsonb) columns are intentionally not mapped as managed
 * fields: chunks are written via a native insert that casts the embedding to {@code vector},
 * and read back for retrieval via a native cosine-similarity query. This entity backs the
 * derived {@code existsBy...} check and keeps the repository typed.
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected DocumentChunk() {
    }

    public Long getId() { return id; }
    public SourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
