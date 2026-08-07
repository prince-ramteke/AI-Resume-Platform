package com.princeramteke.resumeai.jobdescription;

import com.princeramteke.resumeai.auth.User;
import com.princeramteke.resumeai.auth.UserRepository;
import com.princeramteke.resumeai.jobdescription.dto.*;
import com.princeramteke.resumeai.jobdescription.exception.JobDescriptionNotFoundException;
import com.princeramteke.resumeai.jobdescription.mapper.JobDescriptionMapper;
import com.princeramteke.resumeai.jobdescription.validation.JdFileValidator;
import com.princeramteke.resumeai.resume.extraction.DocumentExtractor;
import com.princeramteke.resumeai.resume.extraction.ExtractionResult;
import com.princeramteke.resumeai.resume.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceTest {

    @Mock private JobDescriptionRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private JdFileValidator fileValidator;
    @Mock private JobDescriptionMapper mapper;
    @Mock private DocumentExtractor documentExtractor;

    private JobDescriptionService service;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new JobDescriptionService(
                repository, userRepository, storageService,
                fileValidator, mapper, documentExtractor);
    }

    @Test
    void createFromText_validRequest_persistsAndReturns() {
        var request = new CreateJobDescriptionRequest("Java Engineer", "Looking for Java...");
        var user = createUser();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(repository.save(any(JobDescription.class))).thenAnswer(inv -> {
            JobDescription jd = inv.getArgument(0);
            setId(jd, JD_ID);
            return jd;
        });
        var expected = new JobDescriptionResponse(JD_ID, "Java Engineer", "Looking for Java...",
                null, null, null, null, Instant.now(), null);
        when(mapper.toResponse(any(JobDescription.class))).thenReturn(expected);

        JobDescriptionResponse response = service.createFromText(request, USER_ID);

        assertThat(response.id()).isEqualTo(JD_ID);
        assertThat(response.title()).isEqualTo("Java Engineer");

        ArgumentCaptor<JobDescription> captor = ArgumentCaptor.forClass(JobDescription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Java Engineer");
        assertThat(captor.getValue().getRawText()).isEqualTo("Looking for Java...");
    }

    @Test
    void createFromFile_pdf_extractsAndPersists() {
        var file = new MockMultipartFile("file", "jd.pdf", "application/pdf",
                new byte[]{0x25, 0x50, 0x44, 0x46});
        var user = createUser();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(storageService.store(eq(USER_ID), eq("jd.pdf"), any(InputStream.class)))
                .thenReturn("/uploads/1/uuid_jd.pdf");
        when(documentExtractor.extract(file))
                .thenReturn(new ExtractionResult("extracted JD text", 1, "en"));
        when(repository.save(any(JobDescription.class))).thenAnswer(inv -> {
            JobDescription jd = inv.getArgument(0);
            setId(jd, JD_ID);
            return jd;
        });
        var expected = new JobDescriptionResponse(JD_ID, "Backend Role", "extracted JD text",
                "application/pdf", 4L, 1, "en", Instant.now(), null);
        when(mapper.toResponse(any(JobDescription.class))).thenReturn(expected);

        JobDescriptionResponse response = service.createFromFile("Backend Role", file, USER_ID);

        assertThat(response.id()).isEqualTo(JD_ID);
        verify(fileValidator).validate(file);
        verify(documentExtractor).extract(file);
    }

    @Test
    void createFromFile_txt_readsDirectlyNoTika() {
        var file = new MockMultipartFile("file", "jd.txt", "text/plain",
                "Plain text JD content".getBytes());
        var user = createUser();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(storageService.store(eq(USER_ID), eq("jd.txt"), any(InputStream.class)))
                .thenReturn("/uploads/1/uuid_jd.txt");
        when(repository.save(any(JobDescription.class))).thenAnswer(inv -> {
            JobDescription jd = inv.getArgument(0);
            setId(jd, JD_ID);
            return jd;
        });
        var expected = new JobDescriptionResponse(JD_ID, "TXT Role", "Plain text JD content",
                "text/plain", 21L, null, null, Instant.now(), null);
        when(mapper.toResponse(any(JobDescription.class))).thenReturn(expected);

        JobDescriptionResponse response = service.createFromFile("TXT Role", file, USER_ID);

        assertThat(response.id()).isEqualTo(JD_ID);
        verify(documentExtractor, never()).extract(any());
    }

    @Test
    void getJobDescription_existingAndOwned_returnsResponse() {
        JobDescription jd = createJd();
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jd));
        var expected = new JobDescriptionResponse(JD_ID, "Title", "Text",
                null, null, null, null, Instant.now(), null);
        when(mapper.toResponse(jd)).thenReturn(expected);

        JobDescriptionResponse response = service.getJobDescription(JD_ID, USER_ID, false);

        assertThat(response.id()).isEqualTo(JD_ID);
    }

    @Test
    void getJobDescription_notOwned_throwsNotFound() {
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJobDescription(JD_ID, USER_ID, false))
                .isInstanceOf(JobDescriptionNotFoundException.class);
    }

    @Test
    void getJobDescription_admin_bypassesOwnership() {
        JobDescription jd = createJd();
        when(repository.findByIdAndDeletedFalse(JD_ID)).thenReturn(Optional.of(jd));
        var expected = new JobDescriptionResponse(JD_ID, "Title", "Text",
                null, null, null, null, Instant.now(), null);
        when(mapper.toResponse(jd)).thenReturn(expected);

        JobDescriptionResponse response = service.getJobDescription(JD_ID, 999L, true);

        assertThat(response.id()).isEqualTo(JD_ID);
        verify(repository, never()).findByIdAndUserIdAndDeletedFalse(anyLong(), anyLong());
    }

    @Test
    void listJobDescriptions_returnsPaginatedSummaries() {
        var pageable = PageRequest.of(0, 20);
        JobDescription jd = createJd();
        Page<JobDescription> page = new PageImpl<>(List.of(jd));
        when(repository.findAllByUserIdAndDeletedFalse(USER_ID, pageable)).thenReturn(page);
        var summary = new JobDescriptionSummaryResponse(JD_ID, "Title", null, null, Instant.now());
        when(mapper.toSummary(jd)).thenReturn(summary);

        Page<JobDescriptionSummaryResponse> result = service.listJobDescriptions(
                USER_ID, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listJobDescriptions_withSearch_filtersResults() {
        var pageable = PageRequest.of(0, 20);
        JobDescription jd = createJd();
        Page<JobDescription> page = new PageImpl<>(List.of(jd));
        when(repository.findAllByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(
                USER_ID, "Java", pageable)).thenReturn(page);
        var summary = new JobDescriptionSummaryResponse(JD_ID, "Title", null, null, Instant.now());
        when(mapper.toSummary(jd)).thenReturn(summary);

        Page<JobDescriptionSummaryResponse> result = service.listJobDescriptions(
                USER_ID, "Java", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(
                USER_ID, "Java", pageable);
    }

    @Test
    void updateJobDescription_existingAndOwned_updates() {
        JobDescription jd = createJd();
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jd));
        when(repository.save(any(JobDescription.class))).thenReturn(jd);
        var expected = new JobDescriptionResponse(JD_ID, "New Title", "New Text",
                null, null, null, null, Instant.now(), Instant.now());
        when(mapper.toResponse(any(JobDescription.class))).thenReturn(expected);

        var request = new UpdateJobDescriptionRequest("New Title", "New Text");
        JobDescriptionResponse response = service.updateJobDescription(
                JD_ID, request, USER_ID, false);

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(jd.getTitle()).isEqualTo("New Title");
        assertThat(jd.getRawText()).isEqualTo("New Text");
    }

    @Test
    void deleteJobDescription_existingAndOwned_softDeletes() {
        JobDescription jd = createJd();
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jd));
        when(repository.save(any(JobDescription.class))).thenReturn(jd);

        service.deleteJobDescription(JD_ID, USER_ID, false);

        ArgumentCaptor<JobDescription> captor = ArgumentCaptor.forClass(JobDescription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void deleteJobDescription_notOwned_throwsNotFound() {
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteJobDescription(JD_ID, USER_ID, false))
                .isInstanceOf(JobDescriptionNotFoundException.class);
    }

    @Test
    void downloadJobDescription_withFile_returnsStream() {
        JobDescription jd = createJd();
        jd.setFilePath("/stored/jd.pdf");
        jd.setContentType("application/pdf");
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jd));
        when(storageService.load("/stored/jd.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        JobDescriptionService.DownloadResult result = service.downloadJobDescription(
                JD_ID, USER_ID, false);

        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void downloadJobDescription_textOnly_throwsNotFound() {
        JobDescription jd = createJd();
        when(repository.findByIdAndUserIdAndDeletedFalse(JD_ID, USER_ID))
                .thenReturn(Optional.of(jd));

        assertThatThrownBy(() -> service.downloadJobDescription(JD_ID, USER_ID, false))
                .isInstanceOf(JobDescriptionNotFoundException.class);
    }

    private JobDescription createJd() {
        var user = createUser();
        var jd = new JobDescription(user, "Title", "Job description text");
        setId(jd, JD_ID);
        return jd;
    }

    private User createUser() {
        var user = new User("test@example.com", "hash");
        setUserId(user, USER_ID);
        return user;
    }

    private void setId(JobDescription jd, Long id) {
        try {
            var field = JobDescription.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(jd, id);
        } catch (Exception ignored) {}
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
    }
}
