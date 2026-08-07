package com.princeramteke.resumeai.analysis;

import com.princeramteke.resumeai.analysis.dto.AnalysisResponse;
import com.princeramteke.resumeai.analysis.dto.AnalysisSummaryResponse;
import com.princeramteke.resumeai.analysis.dto.EvidenceResponse;
import com.princeramteke.resumeai.analysis.dto.RecommendationResponse;
import com.princeramteke.resumeai.analysis.dto.SkillResponse;
import com.princeramteke.resumeai.analysis.exception.AnalysisFailedException;
import com.princeramteke.resumeai.analysis.exception.AnalysisNotFoundException;
import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.SecurityConfig;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.resume.exception.ResumeNotFoundException;
import com.princeramteke.resumeai.security.JwtAccessDeniedHandler;
import com.princeramteke.resumeai.security.JwtAuthEntryPoint;
import com.princeramteke.resumeai.security.JwtAuthenticationFilter;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@Import({SecurityConfig.class, CorsConfig.class,
        JwtTokenProvider.class, JwtAuthenticationFilter.class,
        JwtAuthEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class})
@EnableConfigurationProperties(JwtConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiry-minutes=60"
})
class AnalysisControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long ANALYSIS_ID = 55L;
    private static final Instant NOW = Instant.parse("2026-08-07T10:20:00Z");
    private static final String VALID_BODY = "{\"resumeId\":12,\"jobDescriptionId\":7}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AnalysisService service;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = tokenProvider.generateToken(USER_ID, "user@example.com", "USER");
        adminToken = tokenProvider.generateToken(ADMIN_ID, "admin@example.com", "ADMIN");
    }

    private AnalysisResponse sampleResponse() {
        return new AnalysisResponse(ANALYSIS_ID, 78, "Strong backend match",
                List.of(new SkillResponse("Spring Boot", "HIGH", "RESUME#0")),
                List.of(new SkillResponse("AWS", "HIGH", "JD#0")),
                List.of(),
                List.of(new RecommendationResponse("Add AWS", "HIGH", "JD requires AWS")),
                List.of(new EvidenceResponse("RESUME#0", SourceType.RESUME, 0, "Built REST APIs")),
                "ollama", 4120, NOW);
    }

    @Test
    void create_validRequest_returns201WithLocationAndBody() throws Exception {
        when(service.analyze(any(), eq(USER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/analyses/55")))
                .andExpect(jsonPath("$.id").value(ANALYSIS_ID))
                .andExpect(jsonPath("$.score").value(78))
                .andExpect(jsonPath("$.matchedSkills[0].evidenceRef").value("RESUME#0"))
                .andExpect(jsonPath("$.provider").value("ollama"));
    }

    @Test
    void create_missingResumeId_returns400() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("{\"jobDescriptionId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_nonPositiveId_returns400() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("{\"resumeId\":-1,\"jobDescriptionId\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_resumeNotOwned_returns404() throws Exception {
        // ownership model: non-owner is indistinguishable from missing -> 404 (never 403)
        when(service.analyze(any(), eq(USER_ID)))
                .thenThrow(new ResumeNotFoundException(12L));

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_llmUnusableOutput_returns422() throws Exception {
        when(service.analyze(any(), eq(USER_ID)))
                .thenThrow(new AnalysisFailedException("LLM produced unusable output after one repair retry"));

        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void list_authenticated_returns200() throws Exception {
        var summary = new AnalysisSummaryResponse(ANALYSIS_ID, 78, "Backend Engineer", NOW);
        when(service.listAnalyses(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/analyses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ANALYSIS_ID))
                .andExpect(jsonPath("$.content[0].jobTitle").value("Backend Engineer"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/analyses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_ownedAnalysis_returns200() throws Exception {
        when(service.getAnalysis(eq(ANALYSIS_ID), eq(USER_ID), eq(false)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/analyses/{id}", ANALYSIS_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANALYSIS_ID))
                .andExpect(jsonPath("$.evidence[0].ref").value("RESUME#0"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(service.getAnalysis(eq(ANALYSIS_ID), eq(USER_ID), eq(false)))
                .thenThrow(new AnalysisNotFoundException(ANALYSIS_ID));

        mockMvc.perform(get("/api/analyses/{id}", ANALYSIS_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void get_notOwned_returns404NotForbidden() throws Exception {
        // a USER requesting another user's analysis gets 404, not 403 (anti-enumeration)
        when(service.getAnalysis(eq(ANALYSIS_ID), eq(USER_ID), eq(false)))
                .thenThrow(new AnalysisNotFoundException(ANALYSIS_ID));

        mockMvc.perform(get("/api/analyses/{id}", ANALYSIS_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_adminToken_passesAdminOverride() throws Exception {
        when(service.getAnalysis(eq(ANALYSIS_ID), eq(ADMIN_ID), eq(true)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/analyses/{id}", ANALYSIS_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        verify(service).getAnalysis(ANALYSIS_ID, ADMIN_ID, true);
    }

    @Test
    void get_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/analyses/{id}", ANALYSIS_ID))
                .andExpect(status().isUnauthorized());
    }
}
