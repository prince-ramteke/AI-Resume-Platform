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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final ResumeMapper resumeMapper;
    private final DocumentExtractor documentExtractor;

    public ResumeService(ResumeRepository resumeRepository,
                         UserRepository userRepository,
                         StorageService storageService,
                         FileValidator fileValidator,
                         ResumeMapper resumeMapper,
                         DocumentExtractor documentExtractor) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.resumeMapper = resumeMapper;
        this.documentExtractor = documentExtractor;
    }

    @Transactional
    public UploadResumeResponse upload(MultipartFile file, Long userId) {
        log.info("Upload started: userId={}, filename={}", userId, file.getOriginalFilename());

        fileValidator.validate(file);

        String filePath = storeFile(file, userId);
        ExtractionResult extraction = documentExtractor.extract(file);

        User user = userRepository.getReferenceById(userId);
        var resume = new Resume(
                user,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                filePath,
                extraction.text(),
                extraction.pageCount(),
                extraction.language()
        );

        resume = resumeRepository.save(resume);
        log.info("Upload finished: userId={}, resumeId={}", userId, resume.getId());
        return resumeMapper.toUploadResponse(resume);
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResume(Long id, Long userId, boolean isAdmin) {
        Resume resume = findResumeForUser(id, userId, isAdmin);
        return resumeMapper.toResponse(resume);
    }

    @Transactional(readOnly = true)
    public Page<ResumeSummaryResponse> listResumes(Long userId, Pageable pageable) {
        return resumeRepository.findAllByUserIdAndDeletedFalse(userId, pageable)
                .map(resumeMapper::toSummary);
    }

    @Transactional
    public UploadResumeResponse replaceResume(Long id, MultipartFile file, Long userId, boolean isAdmin) {
        Resume resume = findResumeForUser(id, userId, isAdmin);
        log.info("Replace started: userId={}, resumeId={}", userId, id);

        fileValidator.validate(file);

        if (resume.getFilePath() != null) {
            storageService.delete(resume.getFilePath());
        }

        String filePath = storeFile(file, userId);
        ExtractionResult extraction = documentExtractor.extract(file);

        resume.setFilename(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setFileSize(file.getSize());
        resume.setFilePath(filePath);
        resume.setRawText(extraction.text());
        resume.setPageCount(extraction.pageCount());
        resume.setLanguage(extraction.language());

        resume = resumeRepository.save(resume);
        log.info("Replace finished: userId={}, resumeId={}", userId, id);
        return resumeMapper.toUploadResponse(resume);
    }

    @Transactional
    public void deleteResume(Long id, Long userId, boolean isAdmin) {
        Resume resume = findResumeForUser(id, userId, isAdmin);
        resume.setDeleted(true);
        resumeRepository.save(resume);
        log.info("Soft-deleted resume: userId={}, resumeId={}", userId, id);
    }

    @Transactional(readOnly = true)
    public DownloadResult downloadResume(Long id, Long userId, boolean isAdmin) {
        Resume resume = findResumeForUser(id, userId, isAdmin);
        InputStream content = storageService.load(resume.getFilePath());
        return new DownloadResult(resume.getFilename(), resume.getContentType(), content);
    }

    private Resume findResumeForUser(Long id, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return resumeRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new ResumeNotFoundException(id));
        }
        return resumeRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResumeNotFoundException(id));
    }

    private String storeFile(MultipartFile file, Long userId) {
        try {
            return storageService.store(userId, file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new com.princeramteke.resumeai.resume.exception.StorageException(
                    "Failed to read uploaded file", e);
        }
    }

    public record DownloadResult(String filename, String contentType, InputStream content) {}
}
