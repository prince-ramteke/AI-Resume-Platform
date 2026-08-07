package com.princeramteke.resumeai.jobdescription;

import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.SecurityConfig;
import com.princeramteke.resumeai.jobdescription.dto.JobDescriptionResponse;
import com.princeramteke.resumeai.jobdescription.dto.JobDescriptionSummaryResponse;
import com.princeramteke.resumeai.jobdescription.exception.JobDescriptionNotFoundException;
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

@WebMvcTest(JobDescriptionController.class)
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
class JobDescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private JobDescriptionService service;

    private String userToken;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-08-07T10:00:00Z");

    @BeforeEach
    void setUp() {
        userToken = tokenProvider.generateToken(USER_ID, "user@example.com", "USER");
    }

    @Test
    void createFromText_validRequest_returns201() throws Exception {
        var response = new JobDescriptionResponse(JD_ID, "Java Engineer",
                "Looking for Java...", null, null, null, null, NOW, null);
        when(service.createFromText(any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {"title":"Java Engineer","rawText":"Looking for Java..."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(JD_ID))
                .andExpect(jsonPath("$.title").value("Java Engineer"));
    }

    @Test
    void createFromText_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {"title":"","rawText":"Looking for Java..."}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createFromText_blankRawText_returns400() throws Exception {
        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {"title":"Java Engineer","rawText":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFromText_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java Engineer","rawText":"Looking for Java..."}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFromFile_validFile_returns201() throws Exception {
        var file = new MockMultipartFile("file", "jd.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});
        var response = new JobDescriptionResponse(JD_ID, "Backend Role",
                "extracted text", "application/pdf", 4L, 1, "en", NOW, null);
        when(service.createFromFile(eq("Backend Role"), any(), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/job-descriptions/upload")
                        .file(file)
                        .param("title", "Backend Role")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(JD_ID))
                .andExpect(jsonPath("$.title").value("Backend Role"));
    }

    @Test
    void list_authenticated_returns200() throws Exception {
        var summary = new JobDescriptionSummaryResponse(JD_ID, "Title", null, null, NOW);
        var page = new PageImpl<>(List.of(summary));
        when(service.listJobDescriptions(eq(USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job-descriptions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(JD_ID));
    }

    @Test
    void list_withSearch_passesSearchParam() throws Exception {
        var summary = new JobDescriptionSummaryResponse(JD_ID, "Java Title", null, null, NOW);
        var page = new PageImpl<>(List.of(summary));
        when(service.listJobDescriptions(eq(USER_ID), eq("Java"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/job-descriptions")
                        .param("search", "Java")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Title"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/job-descriptions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_existingAndOwned_returns200() throws Exception {
        var response = new JobDescriptionResponse(JD_ID, "Title", "text",
                null, null, null, null, NOW, null);
        when(service.getJobDescription(eq(JD_ID), eq(USER_ID), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/api/job-descriptions/{id}", JD_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(JD_ID))
                .andExpect(jsonPath("$.rawText").value("text"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(service.getJobDescription(eq(JD_ID), eq(USER_ID), eq(false)))
                .thenThrow(new JobDescriptionNotFoundException(JD_ID));

        mockMvc.perform(get("/api/job-descriptions/{id}", JD_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        var response = new JobDescriptionResponse(JD_ID, "New Title", "New Text",
                null, null, null, null, NOW, NOW);
        when(service.updateJobDescription(eq(JD_ID), any(), eq(USER_ID), eq(false)))
                .thenReturn(response);

        mockMvc.perform(put("/api/job-descriptions/{id}", JD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {"title":"New Title","rawText":"New Text"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void delete_existingAndOwned_returns204() throws Exception {
        mockMvc.perform(delete("/api/job-descriptions/{id}", JD_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        verify(service).deleteJobDescription(JD_ID, USER_ID, false);
    }

    @Test
    void delete_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/job-descriptions/{id}", JD_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_existingWithFile_returnsFile() throws Exception {
        byte[] content = {0x25, 0x50, 0x44, 0x46};
        var result = new JobDescriptionService.DownloadResult(
                "jd.pdf", "application/pdf", new ByteArrayInputStream(content));
        when(service.downloadJobDescription(eq(JD_ID), eq(USER_ID), eq(false)))
                .thenReturn(result);

        mockMvc.perform(get("/api/job-descriptions/{id}/download", JD_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(content));
    }

    @Test
    void download_notFound_returns404() throws Exception {
        when(service.downloadJobDescription(eq(JD_ID), eq(USER_ID), eq(false)))
                .thenThrow(new JobDescriptionNotFoundException(JD_ID));

        mockMvc.perform(get("/api/job-descriptions/{id}/download", JD_ID)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}
