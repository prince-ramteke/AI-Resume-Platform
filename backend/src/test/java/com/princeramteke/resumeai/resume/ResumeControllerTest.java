package com.princeramteke.resumeai.resume;

import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.RateLimitProperties;
import com.princeramteke.resumeai.config.SecurityConfig;
import com.princeramteke.resumeai.resume.dto.ResumeResponse;
import com.princeramteke.resumeai.resume.dto.ResumeSummaryResponse;
import com.princeramteke.resumeai.resume.dto.UploadResumeResponse;
import com.princeramteke.resumeai.resume.exception.ResumeNotFoundException;
import com.princeramteke.resumeai.resume.exception.UnsupportedFileTypeException;
import com.princeramteke.resumeai.security.AnalysisRateLimitFilter;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@Import({SecurityConfig.class, CorsConfig.class,
        JwtTokenProvider.class, JwtAuthenticationFilter.class,
        JwtAuthEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class, AnalysisRateLimitFilter.class})
@EnableConfigurationProperties({JwtConfig.class, RateLimitProperties.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiry-minutes=60"
})
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private ResumeService resumeService;

    private String userToken;
    private String adminToken;

    private static final Long USER_ID = 1L;
    private static final Long RESUME_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-08-07T10:00:00Z");

    @BeforeEach
    void setUp() {
        userToken = tokenProvider.generateToken(USER_ID, "user@example.com", "USER");
        adminToken = tokenProvider.generateToken(2L, "admin@example.com", "ADMIN");
    }

    @Test
    void upload_validFile_returns201() throws Exception {
        var file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});
        var response = new UploadResumeResponse(RESUME_ID, "resume.pdf", "application/pdf", 4L, NOW);
        when(resumeService.upload(any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RESUME_ID))
                .andExpect(jsonPath("$.filename").value("resume.pdf"));
    }

    @Test
    void upload_noToken_returns401() throws Exception {
        var file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(multipart("/api/resumes").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_invalidFile_returns400() throws Exception {
        var file = new MockMultipartFile(
                "file", "bad.exe", "application/exe", new byte[]{1, 2});
        when(resumeService.upload(any(), eq(USER_ID)))
                .thenThrow(new UnsupportedFileTypeException("Unsupported file type: exe"));

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void list_authenticated_returns200() throws Exception {
        var summary = new ResumeSummaryResponse(RESUME_ID, "resume.pdf", "application/pdf", 1024L, NOW);
        var page = new PageImpl<>(List.of(summary));
        when(resumeService.listResumes(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/resumes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(RESUME_ID))
                .andExpect(jsonPath("$.content[0].filename").value("resume.pdf"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_existingAndOwned_returns200() throws Exception {
        var response = new ResumeResponse(RESUME_ID, "resume.pdf", "application/pdf",
                1024L, "extracted text", 2, "en", NOW, null);
        when(resumeService.getResume(eq(RESUME_ID), eq(USER_ID), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/resumes/{id}", RESUME_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESUME_ID))
                .andExpect(jsonPath("$.rawText").value("extracted text"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(resumeService.getResume(eq(RESUME_ID), eq(USER_ID), eq(false)))
                .thenThrow(new ResumeNotFoundException(RESUME_ID));

        mockMvc.perform(get("/api/resumes/{id}", RESUME_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void replace_validFile_returns200() throws Exception {
        var file = new MockMultipartFile(
                "file", "new.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});
        var response = new UploadResumeResponse(RESUME_ID, "new.pdf", "application/pdf", 4L, NOW);
        when(resumeService.replaceResume(eq(RESUME_ID), any(), eq(USER_ID), eq(false)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/resumes/{id}", RESUME_ID)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("new.pdf"));
    }

    @Test
    void delete_existingAndOwned_returns204() throws Exception {
        mockMvc.perform(delete("/api/resumes/{id}", RESUME_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        verify(resumeService).deleteResume(RESUME_ID, USER_ID, false);
    }

    @Test
    void delete_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/resumes/{id}", RESUME_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_existingAndOwned_returnsFile() throws Exception {
        byte[] content = {0x25, 0x50, 0x44, 0x46};
        var result = new ResumeService.DownloadResult(
                "resume.pdf", "application/pdf", new ByteArrayInputStream(content));
        when(resumeService.downloadResume(eq(RESUME_ID), eq(USER_ID), eq(false)))
                .thenReturn(result);

        mockMvc.perform(get("/api/resumes/{id}/download", RESUME_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"resume.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(content));
    }

    @Test
    void download_notFound_returns404() throws Exception {
        when(resumeService.downloadResume(eq(RESUME_ID), eq(USER_ID), eq(false)))
                .thenThrow(new ResumeNotFoundException(RESUME_ID));

        mockMvc.perform(get("/api/resumes/{id}/download", RESUME_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}
