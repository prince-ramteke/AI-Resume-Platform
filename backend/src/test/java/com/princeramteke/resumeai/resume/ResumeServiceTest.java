package com.princeramteke.resumeai.resume;

import com.princeramteke.resumeai.auth.User;
import com.princeramteke.resumeai.auth.UserRepository;
import com.princeramteke.resumeai.resume.dto.ResumeResponse;
import com.princeramteke.resumeai.resume.dto.ResumeSummaryResponse;
import com.princeramteke.resumeai.resume.dto.UploadResumeResponse;
import com.princeramteke.resumeai.resume.exception.ResumeNotFoundException;
import com.princeramteke.resumeai.resume.extraction.DocumentExtractor;
import com.princeramteke.resumeai.resume.extraction.ExtractionResult;
import com.princeramteke.resumeai.resume.mapper.ResumeMapper;
import com.princeramteke.resumeai.resume.storage.StorageService;
import com.princeramteke.resumeai.resume.validation.FileValidator;
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
class ResumeServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private FileValidator fileValidator;
    @Mock private ResumeMapper resumeMapper;
    @Mock private DocumentExtractor documentExtractor;

    private ResumeService resumeService;

    private static final Long USER_ID = 1L;
    private static final Long RESUME_ID = 10L;

    // Minimal PDF content with valid magic bytes
    private static final byte[] PDF_BYTES = {
            0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
            0x0A, 0x31, 0x20, 0x30, 0x20, 0x6F, 0x62, 0x6A
    };

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(
                resumeRepository, userRepository, storageService,
                fileValidator, resumeMapper, documentExtractor);
    }

    @Test
    void upload_validFile_persistsAndReturnsResponse() throws Exception {
        var file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", PDF_BYTES);
        var user = new User("test@example.com", "hash");
        setId(user, USER_ID);

        when(storageService.store(eq(USER_ID), eq("resume.pdf"), any(InputStream.class)))
                .thenReturn("/uploads/1/uuid_resume.pdf");
        when(documentExtractor.extract(file))
                .thenReturn(new ExtractionResult("extracted resume text", 2, "en"));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            setResumeId(r, RESUME_ID);
            return r;
        });
        var expected = new UploadResumeResponse(RESUME_ID, "resume.pdf", "application/pdf", (long) PDF_BYTES.length, Instant.now());
        when(resumeMapper.toUploadResponse(any(Resume.class))).thenReturn(expected);

        UploadResumeResponse response = resumeService.upload(file, USER_ID);

        assertThat(response.id()).isEqualTo(RESUME_ID);
        verify(fileValidator).validate(file);
        verify(storageService).store(eq(USER_ID), eq("resume.pdf"), any(InputStream.class));

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(captor.capture());
        Resume saved = captor.getValue();
        assertThat(saved.getFilename()).isEqualTo("resume.pdf");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void upload_invalidFile_throwsValidationException() {
        var file = new MockMultipartFile("file", "bad.exe", "application/exe", new byte[]{1, 2});
        doThrow(new com.princeramteke.resumeai.resume.exception.UnsupportedFileTypeException("bad"))
                .when(fileValidator).validate(file);

        assertThatThrownBy(() -> resumeService.upload(file, USER_ID))
                .isInstanceOf(com.princeramteke.resumeai.resume.exception.UnsupportedFileTypeException.class);
        verify(resumeRepository, never()).save(any());
    }

    @Test
    void getResume_existingAndOwned_returnsResponse() {
        Resume resume = createResume();
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume));
        var expected = new ResumeResponse(RESUME_ID, "resume.pdf", "application/pdf",
                1024L, "text", 2, "en", Instant.now(), null);
        when(resumeMapper.toResponse(resume)).thenReturn(expected);

        ResumeResponse response = resumeService.getResume(RESUME_ID, USER_ID, false);

        assertThat(response.id()).isEqualTo(RESUME_ID);
    }

    @Test
    void getResume_notOwned_throwsNotFound() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getResume(RESUME_ID, USER_ID, false))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void getResume_deletedResume_throwsNotFound() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getResume(RESUME_ID, USER_ID, false))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void getResume_admin_bypassesOwnership() {
        Resume resume = createResume();
        when(resumeRepository.findByIdAndDeletedFalse(RESUME_ID))
                .thenReturn(Optional.of(resume));
        var expected = new ResumeResponse(RESUME_ID, "resume.pdf", "application/pdf",
                1024L, "text", 2, "en", Instant.now(), null);
        when(resumeMapper.toResponse(resume)).thenReturn(expected);

        ResumeResponse response = resumeService.getResume(RESUME_ID, 999L, true);

        assertThat(response.id()).isEqualTo(RESUME_ID);
        verify(resumeRepository, never()).findByIdAndUserIdAndDeletedFalse(anyLong(), anyLong());
    }

    @Test
    void listResumes_returnsPaginatedSummaries() {
        var pageable = PageRequest.of(0, 20);
        Resume resume = createResume();
        Page<Resume> page = new PageImpl<>(List.of(resume));
        when(resumeRepository.findAllByUserIdAndDeletedFalse(USER_ID, pageable)).thenReturn(page);
        var summary = new ResumeSummaryResponse(RESUME_ID, "resume.pdf", "application/pdf", 1024L, Instant.now());
        when(resumeMapper.toSummary(resume)).thenReturn(summary);

        Page<ResumeSummaryResponse> result = resumeService.listResumes(USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(RESUME_ID);
    }

    @Test
    void replaceResume_existingAndOwned_updatesAndReturns() throws Exception {
        Resume resume = createResume();
        resume.setFilePath("/old/path.pdf");
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume));
        var file = new MockMultipartFile("file", "new.pdf", "application/pdf", PDF_BYTES);
        when(documentExtractor.extract(file))
                .thenReturn(new ExtractionResult("new text", 3, "en"));
        when(storageService.store(eq(USER_ID), eq("new.pdf"), any(InputStream.class)))
                .thenReturn("/new/path.pdf");
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);
        var expected = new UploadResumeResponse(RESUME_ID, "new.pdf", "application/pdf", (long) PDF_BYTES.length, Instant.now());
        when(resumeMapper.toUploadResponse(any(Resume.class))).thenReturn(expected);

        UploadResumeResponse response = resumeService.replaceResume(RESUME_ID, file, USER_ID, false);

        assertThat(response.filename()).isEqualTo("new.pdf");
        verify(storageService).delete("/old/path.pdf");
        verify(fileValidator).validate(file);
    }

    @Test
    void replaceResume_notOwned_throwsNotFound() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "new.pdf", "application/pdf", PDF_BYTES);

        assertThatThrownBy(() -> resumeService.replaceResume(RESUME_ID, file, USER_ID, false))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void deleteResume_existingAndOwned_softDeletes() {
        Resume resume = createResume();
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(resume);

        resumeService.deleteResume(RESUME_ID, USER_ID, false);

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void deleteResume_notOwned_throwsNotFound() {
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.deleteResume(RESUME_ID, USER_ID, false))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    void downloadResume_existingAndOwned_returnsStream() {
        Resume resume = createResume();
        resume.setFilePath("/stored/file.pdf");
        when(resumeRepository.findByIdAndUserIdAndDeletedFalse(RESUME_ID, USER_ID))
                .thenReturn(Optional.of(resume));
        when(storageService.load("/stored/file.pdf"))
                .thenReturn(new ByteArrayInputStream(PDF_BYTES));

        ResumeService.DownloadResult result = resumeService.downloadResume(RESUME_ID, USER_ID, false);

        assertThat(result.filename()).isEqualTo("resume.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).isNotNull();
    }

    private Resume createResume() {
        var user = new User("test@example.com", "hash");
        setId(user, USER_ID);
        var resume = new Resume(user, "resume.pdf", "application/pdf", 1024L,
                "/path/file.pdf", "extracted text", 2, "en");
        setResumeId(resume, RESUME_ID);
        return resume;
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ignored) {}
    }

    private void setResumeId(Resume resume, Long id) {
        try {
            var field = Resume.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(resume, id);
        } catch (Exception ignored) {}
    }
}
