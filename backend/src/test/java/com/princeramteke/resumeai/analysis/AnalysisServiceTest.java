package com.princeramteke.resumeai.analysis;

import com.princeramteke.resumeai.analysis.dto.AnalysisRequest;
import com.princeramteke.resumeai.analysis.dto.AnalysisResponse;
import com.princeramteke.resumeai.analysis.dto.AnalysisSummaryResponse;
import com.princeramteke.resumeai.analysis.exception.AnalysisFailedException;
import com.princeramteke.resumeai.analysis.exception.AnalysisNotFoundException;
import com.princeramteke.resumeai.analysis.mapper.AnalysisMapper;
import com.princeramteke.resumeai.analysis.model.Evidence;
import com.princeramteke.resumeai.analysis.model.Recommendation;
import com.princeramteke.resumeai.analysis.model.SkillClaim;
import com.princeramteke.resumeai.analysis.synthesis.AnalysisPromptFactory;
import com.princeramteke.resumeai.analysis.synthesis.OutputValidator;
import com.princeramteke.resumeai.analysis.synthesis.VerdictParser;
import com.princeramteke.resumeai.auth.User;
import com.princeramteke.resumeai.jobdescription.JobDescription;
import com.princeramteke.resumeai.jobdescription.JobDescriptionRepository;
import com.princeramteke.resumeai.jobdescription.exception.JobDescriptionNotFoundException;
import com.princeramteke.resumeai.llm.FakeLlmClient;
import com.princeramteke.resumeai.llm.LlmClient;
import com.princeramteke.resumeai.llm.exception.LlmException;
import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.chunking.TextChunker;
import com.princeramteke.resumeai.rag.ingestion.IngestionService;
import com.princeramteke.resumeai.rag.prompt.PromptAssembler;
import com.princeramteke.resumeai.rag.retrieval.ChunkEvidence;
import com.princeramteke.resumeai.rag.retrieval.RetrievalService;
import com.princeramteke.resumeai.resume.Resume;
import com.princeramteke.resumeai.resume.ResumeRepository;
import com.princeramteke.resumeai.resume.exception.ResumeNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final long USER_ID = 1L;
    private static final long RESUME_ID = 12L;
    private static final long JD_ID = 7L;
    private static final String RESUME_TEXT = "Java Spring Boot developer, built REST APIs";
    private static final String JD_TEXT = "We need AWS experience.";

    private static final String VALID_VERDICT = """
            {
              "score": 75,
              "summary": "Strong backend match, missing cloud",
              "matchedSkills": [{"skill":"Spring Boot","importance":"HIGH","evidenceRef":"RESUME#0"}],
              "missingSkills": [{"skill":"AWS","importance":"HIGH","evidenceRef":"JD#0"}],
              "weakSkills": [],
              "recommendations": [{"text":"Add AWS","impact":"HIGH","reason":"JD requires AWS"}]
            }
            """;

    @Mock private ResumeRepository resumeRepository;
    @Mock private JobDescriptionRepository jobDescriptionRepository;
    @Mock private AnalysisRepository analysisRepository;
    @Mock private IngestionService ingestionService;
    @Mock private RetrievalService retrievalService;

    private TextChunker textChunker;
    private AnalysisPromptFactory promptFactory;
    private VerdictParser verdictParser;
    private OutputValidator outputValidator;
    private AnalysisMapper mapper;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        RagConfig ragConfig = new RagConfig(500, 50, 8, 3500);
        textChunker = new TextChunker(ragConfig);
        meterRegistry = new SimpleMeterRegistry();
        promptFactory = new AnalysisPromptFactory(new PromptAssembler(ragConfig, meterRegistry));
        verdictParser = new VerdictParser();
        outputValidator = new OutputValidator();
        mapper = org.mapstruct.factory.Mappers.getMapper(AnalysisMapper.class);
    }

    private AnalysisService service(LlmClient llm) {
        return new AnalysisService(resumeRepository, jobDescriptionRepository, analysisRepository,
                ingestionService, retrievalService, textChunker, promptFactory, llm,
                verdictParser, outputValidator, mapper, meterRegistry);
    }

    /** analysis.count value for one (result, cache) tag combination; 0.0 if the meter is absent. */
    private double analysisCount(String result, String cache) {
        var counter = meterRegistry.find("analysis.count")
                .tag("result", result).tag("cache", cache).counter();
        return counter == null ? 0.0 : counter.count();
    }

    /** llm.tokens value for one (provider, type) tag combination; 0.0 if the meter is absent. */
    private double tokenCount(String provider, String type) {
        var counter = meterRegistry.find("llm.tokens")
                .tag("provider", provider).tag("type", type).counter();
        return counter == null ? 0.0 : counter.count();
    }

    // Fixed base timestamps for deterministic cache-freshness tests. Anything
    // "later than" these represents an edit; anything "earlier" is stale.
    private static final Instant RESUME_CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant JD_CREATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    private Resume resume() {
        User user = new User("prince@example.com", "hash");
        Resume resume = new Resume(user, "resume.pdf", "application/pdf", 1000L,
                "/path/resume.pdf", RESUME_TEXT, 1, "en");
        ReflectionTestUtils.setField(resume, "id", RESUME_ID);
        // Prime timestamps — the service reads createdAt/updatedAt for the
        // cache-freshness invariant; @PrePersist doesn't fire on in-memory
        // objects.
        ReflectionTestUtils.setField(resume, "createdAt", RESUME_CREATED_AT);
        return resume;
    }

    private JobDescription jobDescription() {
        JobDescription jd = new JobDescription(new User("prince@example.com", "hash"),
                "Backend Engineer", JD_TEXT);
        ReflectionTestUtils.setField(jd, "id", JD_ID);
        ReflectionTestUtils.setField(jd, "createdAt", JD_CREATED_AT);
        return jd;
    }

    private void stubOwnedResumeAndJd() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume()));
        when(jobDescriptionRepository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jobDescription()));
    }

    private void stubRetrieval() {
        when(retrievalService.retrieve(SourceType.RESUME, RESUME_ID, JD_TEXT))
                .thenReturn(List.of(new ChunkEvidence(
                        "RESUME#0", SourceType.RESUME, 0, "Built REST APIs with Spring Boot", 0.9)));
    }

    private void stubSaveReturnsArgument() {
        when(analysisRepository.save(any(Analysis.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AnalysisRequest request() {
        return new AnalysisRequest(RESUME_ID, JD_ID);
    }

    @Test
    void analyze_happyPath_runsPipelineAndReturnsGroundedResponse() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        stubSaveReturnsArgument();

        AnalysisResponse response = service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(response.score()).isEqualTo(75);
        assertThat(response.summary()).isEqualTo("Strong backend match, missing cloud");
        assertThat(response.matchedSkills()).singleElement()
                .satisfies(s -> assertThat(s.evidenceRef()).isEqualTo("RESUME#0"));
        assertThat(response.missingSkills()).singleElement()
                .satisfies(s -> assertThat(s.evidenceRef()).isEqualTo("JD#0"));
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.evidence()).extracting(e -> e.ref())
                .containsExactlyInAnyOrder("RESUME#0", "JD#0");

        verify(ingestionService).ingest(SourceType.RESUME, RESUME_ID, RESUME_TEXT);
        verify(retrievalService).retrieve(SourceType.RESUME, RESUME_ID, JD_TEXT);
    }

    @Test
    void analyze_cacheHit_returnsPreviousAnalysisAndSkipsPipeline() {
        // Freshness threshold = max(RESUME_CREATED_AT, JD_CREATED_AT) = JD_CREATED_AT.
        // A cached analysis created after that is fresh and must be returned as-is.
        stubOwnedResumeAndJd();
        Analysis cached = new Analysis(
                new User("prince@example.com", "hash"), resume(), jobDescription(),
                88, "cached summary",
                List.of(new SkillClaim("Cached Skill", "HIGH", "RESUME#0")),
                List.of(), List.of(),
                List.of(new Recommendation("cached rec", "HIGH", "cached reason")),
                List.of(new Evidence("RESUME#0", SourceType.RESUME, 0, "cached snippet")),
                "ollama", 4200);
        ReflectionTestUtils.setField(cached, "id", 55L);
        ReflectionTestUtils.setField(cached, "createdAt", JD_CREATED_AT.plusSeconds(60));
        when(analysisRepository
                .findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        USER_ID, RESUME_ID, JD_ID, JD_CREATED_AT))
                .thenReturn(Optional.of(cached));

        AnalysisResponse response = service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.score()).isEqualTo(88);
        assertThat(response.summary()).isEqualTo("cached summary");
        // Pipeline steps 4-10 must all be skipped.
        verify(ingestionService, never()).ingest(any(), any(), any());
        verify(retrievalService, never()).retrieve(any(), any(), any());
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void analyze_cachedButResumeWasReplaced_runsPipelineAgain() {
        // Resume was replaced AFTER the last analysis: freshnessThreshold moves
        // to the replacement instant, and the repository's ">= threshold" query
        // returns empty. Service must fall through to the full pipeline.
        Resume replacedResume = resume();
        ReflectionTestUtils.setField(replacedResume, "updatedAt", JD_CREATED_AT.plusSeconds(3600));
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(replacedResume));
        when(jobDescriptionRepository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jobDescription()));
        when(analysisRepository
                .findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        stubRetrieval();
        stubSaveReturnsArgument();

        AnalysisResponse response = service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(response.score()).isEqualTo(75); // from VALID_VERDICT, not a cached row
        verify(ingestionService).ingest(SourceType.RESUME, RESUME_ID, RESUME_TEXT);
        verify(retrievalService).retrieve(SourceType.RESUME, RESUME_ID, JD_TEXT);
        verify(analysisRepository).save(any(Analysis.class));
    }

    @Test
    void analyze_cacheLookup_isScopedToOwner() {
        // Ownership is baked into the derived query: userId is part of the
        // WHERE clause, so a different user's cached row is invisible even if
        // the resume/JD pair matches. We assert the exact userId passed to the
        // repository call — a regression that dropped that arg would cross the
        // isolation boundary.
        stubOwnedResumeAndJd();
        stubRetrieval();
        stubSaveReturnsArgument();
        when(analysisRepository
                .findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        verify(analysisRepository)
                .findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        eq(USER_ID), eq(RESUME_ID), eq(JD_ID), any(Instant.class));
    }

    @Test
    void analyze_resumeNotFound_throwsAndStopsPipeline() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID))
                .isInstanceOf(ResumeNotFoundException.class);

        verify(jobDescriptionRepository, never()).findByIdAndUserIdAndDeletedFalse(any(), any());
        verify(ingestionService, never()).ingest(any(), any(), any());
    }

    @Test
    void analyze_jobDescriptionNotFound_throws() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume()));
        when(jobDescriptionRepository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID))
                .isInstanceOf(JobDescriptionNotFoundException.class);

        verify(ingestionService, never()).ingest(any(), any(), any());
    }

    @Test
    void analyze_resumeOwnedByAnotherUser_throwsNotFound() {
        // ownership model: a resource not owned by the caller is invisible (userId-scoped finder) -> 404
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void analyze_unusableVerdictAfterRetry_throwsAnalysisFailedAndDoesNotPersist() {
        stubOwnedResumeAndJd();
        stubRetrieval();

        assertThatThrownBy(() ->
                service(new FakeLlmClient("this is not json")).analyze(request(), USER_ID))
                .isInstanceOf(AnalysisFailedException.class)
                .hasMessageContaining("after one repair retry");

        verify(analysisRepository, never()).save(any());
    }

    @Test
    void analyze_persistenceFailure_propagates() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        when(analysisRepository.save(any(Analysis.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("database unavailable");
    }

    @Test
    void analyze_providerException_propagatesAndDoesNotPersist() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        LlmClient failing = mock(LlmClient.class);
        // providerName tags the llm.latency timer (recorded even when complete() fails).
        when(failing.providerName()).thenReturn("ollama");
        when(failing.complete(any())).thenThrow(new LlmException("provider unreachable"));

        assertThatThrownBy(() -> service(failing).analyze(request(), USER_ID))
                .isInstanceOf(LlmException.class);

        verify(analysisRepository, never()).save(any());
    }

    @Test
    void analyze_groundingRemovesDanglingEvidenceReferences() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        stubSaveReturnsArgument();
        String verdictWithDanglingRef = """
                {
                  "score": 70,
                  "summary": "match",
                  "matchedSkills": [
                    {"skill":"Spring Boot","importance":"HIGH","evidenceRef":"RESUME#0"},
                    {"skill":"Hallucinated","importance":"HIGH","evidenceRef":"RESUME#88"}
                  ],
                  "missingSkills": [],
                  "weakSkills": [],
                  "recommendations": []
                }
                """;

        AnalysisResponse response =
                service(new FakeLlmClient(verdictWithDanglingRef)).analyze(request(), USER_ID);

        assertThat(response.matchedSkills()).singleElement()
                .satisfies(s -> assertThat(s.skill()).isEqualTo("Spring Boot"));
        assertThat(response.evidence()).extracting(e -> e.ref()).containsExactly("RESUME#0");
    }

    @Test
    void analyze_recordsLatency() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(captor.getValue().getLatencyMs()).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void analyze_persistsProviderName() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisResponse response = service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(captor.getValue().getProvider()).isEqualTo("fake");
        assertThat(response.provider()).isEqualTo("fake");
    }

    @Test
    void getAnalysis_notOwned_throwsNotFound() {
        when(analysisRepository.findByIdAndUserId(55L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(new FakeLlmClient(VALID_VERDICT)).getAnalysis(55L, USER_ID, false))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    @Test
    void getAnalysis_adminCanReadAnyAnalysis() {
        Analysis analysis = new Analysis(new User("a@b.com", "h"), resume(), jobDescription(),
                80, "ok", List.of(new SkillClaim("Java", "HIGH", "RESUME#0")),
                List.of(), List.of(), List.of(new Recommendation("t", "LOW", "r")),
                List.of(new Evidence("RESUME#0", SourceType.RESUME, 0, "snippet")), "ollama", 100);
        when(analysisRepository.findById(55L)).thenReturn(Optional.of(analysis));

        AnalysisResponse response = service(new FakeLlmClient(VALID_VERDICT)).getAnalysis(55L, USER_ID, true);

        assertThat(response.score()).isEqualTo(80);
        assertThat(response.matchedSkills()).singleElement()
                .satisfies(s -> assertThat(s.skill()).isEqualTo("Java"));
    }

    @Test
    void listAnalyses_mapsSummariesWithJobTitle() {
        Analysis analysis = new Analysis(new User("a@b.com", "h"), resume(), jobDescription(),
                65, "ok", List.of(), List.of(), List.of(), List.of(), List.of(), "ollama", 50);
        Page<Analysis> page = new PageImpl<>(List.of(analysis));
        when(analysisRepository.findAllByUserId(USER_ID, PageRequest.of(0, 20))).thenReturn(page);

        Page<AnalysisSummaryResponse> result =
                service(new FakeLlmClient(VALID_VERDICT)).listAnalyses(USER_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).singleElement().satisfies(s -> {
            assertThat(s.score()).isEqualTo(65);
            assertThat(s.jobTitle()).isEqualTo("Backend Engineer");
        });
    }

    // ---------------------------------------------------------------------
    // Observability metrics (v1.1) — purely additive; no behavior change.
    // ---------------------------------------------------------------------

    @Test
    void metrics_successfulCacheMiss_incrementsSuccessMissAndRecordsLatency() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        stubSaveReturnsArgument();

        service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(analysisCount("success", "miss")).isEqualTo(1.0);
        assertThat(analysisCount("success", "hit")).isZero();
        assertThat(analysisCount("failure", "miss")).isZero();
        // The pipeline latency timer recorded exactly one sample.
        assertThat(meterRegistry.find("analysis.latency").timer().count()).isEqualTo(1L);
    }

    @Test
    void metrics_cacheHit_incrementsSuccessHitAndSkipsLatencyTimer() {
        stubOwnedResumeAndJd();
        Analysis cached = new Analysis(
                new User("prince@example.com", "hash"), resume(), jobDescription(),
                88, "cached summary",
                List.of(new SkillClaim("Cached Skill", "HIGH", "RESUME#0")),
                List.of(), List.of(),
                List.of(new Recommendation("cached rec", "HIGH", "cached reason")),
                List.of(new Evidence("RESUME#0", SourceType.RESUME, 0, "cached snippet")),
                "ollama", 4200);
        ReflectionTestUtils.setField(cached, "id", 55L);
        ReflectionTestUtils.setField(cached, "createdAt", JD_CREATED_AT.plusSeconds(60));
        when(analysisRepository
                .findFirstByUserIdAndResumeIdAndJobDescriptionIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        USER_ID, RESUME_ID, JD_ID, JD_CREATED_AT))
                .thenReturn(Optional.of(cached));

        service(new FakeLlmClient(VALID_VERDICT)).analyze(request(), USER_ID);

        assertThat(analysisCount("success", "hit")).isEqualTo(1.0);
        assertThat(analysisCount("success", "miss")).isZero();
        // Cache hit short-circuits before the pipeline timer — no latency sample recorded.
        assertThat(meterRegistry.find("analysis.latency").timer().count()).isZero();
    }

    @Test
    void metrics_analysisFailure_incrementsFailureMissAndPropagatesOriginalException() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        LlmClient failing = mock(LlmClient.class);
        when(failing.providerName()).thenReturn("ollama");
        when(failing.complete(any())).thenThrow(new LlmException("provider unreachable"));

        assertThatThrownBy(() -> service(failing).analyze(request(), USER_ID))
                .isInstanceOf(LlmException.class)
                .hasMessage("provider unreachable");

        assertThat(analysisCount("failure", "miss")).isEqualTo(1.0);
        assertThat(analysisCount("success", "miss")).isZero();
        assertThat(analysisCount("success", "hit")).isZero();
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void metrics_llmLatencyAndTokens_recordExactValuesAndProviderLabel() {
        stubOwnedResumeAndJd();
        stubRetrieval();
        stubSaveReturnsArgument();
        // Known token usage from the fake provider; provider label is "fake".
        service(new FakeLlmClient(VALID_VERDICT, 123, 45)).analyze(request(), USER_ID);

        // 4: LLM latency timer records at least the single completion call.
        assertThat(meterRegistry.find("llm.latency").tag("provider", "fake").timer().count())
                .isEqualTo(1L);
        // 5 + 6: prompt/completion counters receive the exact LlmResponse values under the right labels.
        assertThat(tokenCount("fake", "prompt")).isEqualTo(123.0);
        assertThat(tokenCount("fake", "completion")).isEqualTo(45.0);
    }
}
