package com.princeramteke.resumeai.analysis;

import com.princeramteke.resumeai.analysis.model.Evidence;
import com.princeramteke.resumeai.analysis.model.Recommendation;
import com.princeramteke.resumeai.analysis.model.SkillClaim;
import com.princeramteke.resumeai.auth.User;
import com.princeramteke.resumeai.jobdescription.JobDescription;
import com.princeramteke.resumeai.resume.Resume;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * A completed analysis of a resume against a job description. Write-once: an analysis is created
 * by the analysis service and never updated, so this entity exposes no setters. The verdict's
 * list fields ({@code matched_skills}, {@code missing_skills}, {@code weak_skills},
 * {@code recommendations}, {@code evidence}) are stored as JSONB via
 * {@link JdbcTypeCode}({@link SqlTypes#JSON}) — Hibernate serializes the model records with
 * Jackson (see DATABASE.md §5); the vector columns are not involved here.
 */
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(nullable = false)
    private int score;

    @Column(length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_skills", columnDefinition = "jsonb", nullable = false)
    private List<SkillClaim> matchedSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_skills", columnDefinition = "jsonb", nullable = false)
    private List<SkillClaim> missingSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_skills", columnDefinition = "jsonb", nullable = false)
    private List<SkillClaim> weakSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Recommendation> recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Evidence> evidence;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Analysis() {
    }

    public Analysis(User user, Resume resume, JobDescription jobDescription,
                    int score, String summary,
                    List<SkillClaim> matchedSkills, List<SkillClaim> missingSkills,
                    List<SkillClaim> weakSkills, List<Recommendation> recommendations,
                    List<Evidence> evidence, String provider, Integer latencyMs) {
        this.user = user;
        this.resume = resume;
        this.jobDescription = jobDescription;
        this.score = score;
        this.summary = summary;
        this.matchedSkills = orEmpty(matchedSkills);
        this.missingSkills = orEmpty(missingSkills);
        this.weakSkills = orEmpty(weakSkills);
        this.recommendations = orEmpty(recommendations);
        this.evidence = orEmpty(evidence);
        this.provider = provider;
        this.latencyMs = latencyMs;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Resume getResume() { return resume; }
    public JobDescription getJobDescription() { return jobDescription; }
    public int getScore() { return score; }
    public String getSummary() { return summary; }
    public List<SkillClaim> getMatchedSkills() { return matchedSkills; }
    public List<SkillClaim> getMissingSkills() { return missingSkills; }
    public List<SkillClaim> getWeakSkills() { return weakSkills; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public List<Evidence> getEvidence() { return evidence; }
    public String getProvider() { return provider; }
    public Integer getLatencyMs() { return latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
}
