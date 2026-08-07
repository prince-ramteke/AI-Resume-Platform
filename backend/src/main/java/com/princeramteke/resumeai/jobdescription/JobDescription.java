package com.princeramteke.resumeai.jobdescription;

import com.princeramteke.resumeai.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path")
    private String filePath;

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

    protected JobDescription() {
    }

    public JobDescription(User user, String title, String rawText) {
        this.user = user;
        this.title = title;
        this.rawText = rawText;
    }

    public JobDescription(User user, String title, String rawText,
                          String contentType, Long fileSize, String filePath,
                          Integer pageCount, String language) {
        this.user = user;
        this.title = title;
        this.rawText = rawText;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
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
    public String getTitle() { return title; }
    public String getRawText() { return rawText; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public String getFilePath() { return filePath; }
    public Integer getPageCount() { return pageCount; }
    public String getLanguage() { return language; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setLanguage(String language) { this.language = language; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
