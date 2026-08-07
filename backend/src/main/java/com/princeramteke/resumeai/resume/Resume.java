package com.princeramteke.resumeai.resume;

import com.princeramteke.resumeai.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column
    private String language;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Resume() {
    }

    public Resume(User user, String filename, String contentType, Long fileSize,
                  String filePath, String rawText, Integer pageCount, String language) {
        this.user = user;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.rawText = rawText;
        this.pageCount = pageCount;
        this.language = language;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public String getFilePath() { return filePath; }
    public String getRawText() { return rawText; }
    public Integer getPageCount() { return pageCount; }
    public String getLanguage() { return language; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFilename(String filename) { this.filename = filename; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setLanguage(String language) { this.language = language; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
