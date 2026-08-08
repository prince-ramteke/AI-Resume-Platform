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
import com.princeramteke.resumeai.analysis.synthesis.InvalidVerdictException;
import com.princeramteke.resumeai.analysis.synthesis.LlmVerdict;
import com.princeramteke.resumeai.analysis.synthesis.OutputValidator;
import com.princeramteke.resumeai.analysis.synthesis.VerdictParser;
import com.princeramteke.resumeai.jobdescription.JobDescription;
import com.princeramteke.resumeai.jobdescription.JobDescriptionRepository;
import com.princeramteke.resumeai.jobdescription.exception.JobDescriptionNotFoundException;
import com.princeramteke.resumeai.llm.LlmClient;
import com.princeramteke.resumeai.llm.LlmRequest;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.chunking.TextChunker;
import com.princeramteke.resumeai.rag.ingestion.IngestionService;
import com.princeramteke.resumeai.rag.retrieval.ChunkEvidence;
import com.princeramteke.resumeai.rag.retrieval.RetrievalService;
import com.princeramteke.resumeai.resume.Resume;
import com.princeramteke.resumeai.resume.ResumeRepository;
import com.princeramteke.resumeai.resume.exception.ResumeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Orchestrates a single analysis of a resume against a job description, composing the RAG
 * foundation (ingestion, retrieval, prompt assembly) with the LLM abstraction and the synthesis
 * core (parse, validate, ground) into a persisted, evidence-grounded verdict.
 *
 * <p><b>Transaction boundary:</b> {@code analyze} is intentionally not {@code @Transactional} —
 * the embedding (ingestion) and LLM calls are slow and must never hold a database connection.
 * The only write is {@link AnalysisRepository#save}, which runs in Spring Data's own short
 * transaction. Ownership follows the platform model: a resource the caller does not own is
 * indistinguishable from one that does not exist (404), preventing enumeration.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    /** Appended to the prompt on the single repair retry so a deterministic model gets new input. */
    private static final String REPAIR_REMINDER =
            "\n\nREMINDER: Your previous response was not valid JSON. Respond with STRICT, VALID JSON ONLY "
                    + "that matches the schema exactly — no prose, no markdown, no code fences.";

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AnalysisRepository analysisRepository;
    private final IngestionService ingestionService;
    private final RetrievalService retrievalService;
    private final TextChunker textChunker;
    private final AnalysisPromptFactory promptFactory;
    private final LlmClient llmClient;
    private final VerdictParser verdictParser;
    private final OutputValidator outputValidator;
    private final AnalysisMapper analysisMapper;

    public AnalysisService(ResumeRepository resumeRepository,
                           JobDescriptionRepository jobDescriptionRepository,
                           AnalysisRepository analysisRepository,
                           IngestionService ingestionService,
                           RetrievalService retrievalService,
                           TextChunker textChunker,
                           AnalysisPromptFactory promptFactory,
                           LlmClient llmClient,
                           VerdictParser verdictParser,
                           OutputValidator outputValidator,
                           AnalysisMapper analysisMapper) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.analysisRepository = analysisRepository;
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
        this.textChunker = textChunker;
        this.promptFactory = promptFactory;
        this.llmClient = llmClient;
        this.verdictParser = verdictParser;
        this.outputValidator = outputValidator;
        this.analysisMapper = analysisMapper;
    }

    public AnalysisResponse analyze(AnalysisRequest request, Long userId) {
        // 1-3: ownership validation + resume/JD lookup (non-owner or missing -> 404)
        Resume resume = resumeRepository.findByIdAndUserIdAndDeletedFalse(request.resumeId(), userId)
                .orElseThrow(() -> new ResumeNotFoundException(request.resumeId()));
        JobDescription jd = jobDescriptionRepository
                .findByIdAndUserIdAndDeletedFalse(request.jobDescriptionId(), userId)
                .orElseThrow(() -> new JobDescriptionNotFoundException(request.jobDescriptionId()));

        log.info("Analysis started: userId={}, resumeId={}, jobDescriptionId={}",
                userId, resume.getId(), jd.getId());

        // 4: resume ingestion (idempotent; embeddings run outside any DB transaction)
        ingestionService.ingest(SourceType.RESUME, resume.getId(), resume.getRawText());

        // 5: retrieval — resume chunks nearest to the JD text (JD is the query, not embedded)
        List<ChunkEvidence> resumeEvidence = retrievalService.retrieve(
                SourceType.RESUME, resume.getId(), jd.getRawText());

        // JD chunked in-memory for grounding only (JD#n tags); never embedded or stored
        List<ChunkEvidence> jdEvidence = chunkJobDescription(jd.getRawText());

        // 6: prompt assembly (system rubric isolated; evidence delimited as untrusted)
        LlmRequest prompt = promptFactory.build(jdEvidence, resumeEvidence);

        // 7-9: LLM completion -> parse -> validate/ground, with one stricter repair retry
        Map<String, ChunkEvidence> evidencePool = indexByRef(jdEvidence, resumeEvidence);
        long startNanos = System.nanoTime();
        LlmVerdict verdict = synthesize(prompt, evidencePool.keySet());
        int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000L);

        // 10: persist (the single DB write; committed in the repository's own transaction)
        Analysis analysis = new Analysis(
                resume.getUser(), resume, jd,
                verdict.score(), verdict.summary(),
                toModelSkills(verdict.matchedSkills()),
                toModelSkills(verdict.missingSkills()),
                toModelSkills(verdict.weakSkills()),
                toModelRecommendations(verdict.recommendations()),
                citedEvidence(verdict, evidencePool),
                llmClient.providerName(), latencyMs);
        Analysis saved = analysisRepository.save(analysis);

        log.info("Analysis finished: userId={}, analysisId={}, score={}, provider={}, latencyMs={}",
                userId, saved.getId(), saved.getScore(), saved.getProvider(), latencyMs);

        // 11: map to DTO
        return analysisMapper.toResponse(saved);
    }

    public AnalysisResponse getAnalysis(Long id, Long userId, boolean isAdmin) {
        return analysisMapper.toResponse(findForUser(id, userId, isAdmin));
    }

    public Page<AnalysisSummaryResponse> listAnalyses(Long userId, Pageable pageable) {
        return analysisRepository.findAllByUserId(userId, pageable).map(analysisMapper::toSummary);
    }

    private Analysis findForUser(Long id, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return analysisRepository.findById(id)
                    .orElseThrow(() -> new AnalysisNotFoundException(id));
        }
        return analysisRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
    }

    /** LLM completion, parse and validation with exactly one stricter repair retry. */
    private LlmVerdict synthesize(LlmRequest prompt, Set<String> validRefs) {
        try {
            return parseAndValidate(llmClient.complete(prompt).content(), validRefs);
        } catch (InvalidVerdictException first) {
            log.warn("Verdict unusable ({}); retrying once with a stricter prompt", first.getMessage());
            LlmRequest repair = new LlmRequest(prompt.systemPrompt(), prompt.userPrompt() + REPAIR_REMINDER);
            try {
                return parseAndValidate(llmClient.complete(repair).content(), validRefs);
            } catch (InvalidVerdictException second) {
                throw new AnalysisFailedException(
                        "LLM produced unusable output after one repair retry", second);
            }
        }
    }

    private LlmVerdict parseAndValidate(String raw, Set<String> validRefs) {
        return outputValidator.validate(verdictParser.parse(raw), validRefs);
    }

    private List<ChunkEvidence> chunkJobDescription(String jdText) {
        return textChunker.chunk(jdText).stream()
                .map(c -> new ChunkEvidence("JD#" + c.index(), SourceType.JD, c.index(), c.content(), 0.0))
                .toList();
    }

    private Map<String, ChunkEvidence> indexByRef(List<ChunkEvidence> jdEvidence,
                                                  List<ChunkEvidence> resumeEvidence) {
        Map<String, ChunkEvidence> pool = new LinkedHashMap<>();
        jdEvidence.forEach(e -> pool.put(e.ref(), e));
        resumeEvidence.forEach(e -> pool.put(e.ref(), e));
        return pool;
    }

    /** Evidence entries actually cited by the (already grounded) verdict, in stable order. */
    private List<Evidence> citedEvidence(LlmVerdict verdict, Map<String, ChunkEvidence> pool) {
        Set<String> refs = new LinkedHashSet<>();
        Stream.of(verdict.matchedSkills(), verdict.missingSkills(), verdict.weakSkills())
                .flatMap(List::stream)
                .map(LlmVerdict.SkillClaim::evidenceRef)
                .forEach(refs::add);
        return refs.stream()
                .map(pool::get)
                .filter(Objects::nonNull)
                .map(e -> new Evidence(e.ref(), e.sourceType(), e.chunkIndex(), e.snippet()))
                .toList();
    }

    private List<SkillClaim> toModelSkills(List<LlmVerdict.SkillClaim> claims) {
        return claims.stream()
                .map(c -> new SkillClaim(c.skill(), c.importance(), c.evidenceRef()))
                .toList();
    }

    private List<Recommendation> toModelRecommendations(List<LlmVerdict.Recommendation> recommendations) {
        return recommendations.stream()
                .map(r -> new Recommendation(r.text(), r.impact(), r.reason()))
                .toList();
    }
}
