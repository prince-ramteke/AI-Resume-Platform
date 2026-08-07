package com.princeramteke.resumeai.jobdescription;

import com.princeramteke.resumeai.auth.User;
import com.princeramteke.resumeai.auth.UserRepository;
import com.princeramteke.resumeai.jobdescription.dto.*;
import com.princeramteke.resumeai.jobdescription.exception.JobDescriptionNotFoundException;
import com.princeramteke.resumeai.jobdescription.mapper.JobDescriptionMapper;
import com.princeramteke.resumeai.jobdescription.validation.JdFileValidator;
import com.princeramteke.resumeai.resume.exception.StorageException;
import com.princeramteke.resumeai.resume.extraction.DocumentExtractor;
import com.princeramteke.resumeai.resume.extraction.ExtractionResult;
import com.princeramteke.resumeai.resume.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class JobDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(JobDescriptionService.class);

    private final JobDescriptionRepository repository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final JdFileValidator fileValidator;
    private final JobDescriptionMapper mapper;
    private final DocumentExtractor documentExtractor;

    public JobDescriptionService(JobDescriptionRepository repository,
                                 UserRepository userRepository,
                                 StorageService storageService,
                                 JdFileValidator fileValidator,
                                 JobDescriptionMapper mapper,
                                 DocumentExtractor documentExtractor) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.mapper = mapper;
        this.documentExtractor = documentExtractor;
    }

    @Transactional
    public JobDescriptionResponse createFromText(CreateJobDescriptionRequest request, Long userId) {
        log.info("Creating JD from text: userId={}, title={}", userId, request.title());

        User user = userRepository.getReferenceById(userId);
        var jd = new JobDescription(user, request.title(), request.rawText());
        jd = repository.save(jd);

        log.info("JD created: userId={}, jdId={}", userId, jd.getId());
        return mapper.toResponse(jd);
    }

    @Transactional
    public JobDescriptionResponse createFromFile(String title, MultipartFile file, Long userId) {
        log.info("Creating JD from file: userId={}, title={}, filename={}",
                userId, title, file.getOriginalFilename());

        fileValidator.validate(file);

        String filePath = storeFile(file, userId);
        User user = userRepository.getReferenceById(userId);

        JobDescription jd;
        if (isPlainText(file)) {
            String rawText = readPlainText(file);
            jd = new JobDescription(user, title, rawText,
                    file.getContentType(), file.getSize(), filePath, null, null);
        } else {
            ExtractionResult result = documentExtractor.extract(file);
            jd = new JobDescription(user, title, result.text(),
                    file.getContentType(), file.getSize(), filePath,
                    result.pageCount(), result.language());
        }

        jd = repository.save(jd);
        log.info("JD created from file: userId={}, jdId={}", userId, jd.getId());
        return mapper.toResponse(jd);
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponse getJobDescription(Long id, Long userId, boolean isAdmin) {
        JobDescription jd = findForUser(id, userId, isAdmin);
        return mapper.toResponse(jd);
    }

    @Transactional(readOnly = true)
    public Page<JobDescriptionSummaryResponse> listJobDescriptions(Long userId, String search,
                                                                    Pageable pageable) {
        Page<JobDescription> page;
        if (search != null && !search.isBlank()) {
            page = repository.findAllByUserIdAndDeletedFalseAndTitleContainingIgnoreCase(
                    userId, search.strip(), pageable);
        } else {
            page = repository.findAllByUserIdAndDeletedFalse(userId, pageable);
        }
        return page.map(mapper::toSummary);
    }

    @Transactional
    public JobDescriptionResponse updateJobDescription(Long id,
                                                        UpdateJobDescriptionRequest request,
                                                        Long userId, boolean isAdmin) {
        JobDescription jd = findForUser(id, userId, isAdmin);
        log.info("Updating JD: userId={}, jdId={}", userId, id);

        jd.setTitle(request.title());
        jd.setRawText(request.rawText());
        jd = repository.save(jd);

        log.info("JD updated: userId={}, jdId={}", userId, id);
        return mapper.toResponse(jd);
    }

    @Transactional
    public void deleteJobDescription(Long id, Long userId, boolean isAdmin) {
        JobDescription jd = findForUser(id, userId, isAdmin);
        jd.setDeleted(true);
        repository.save(jd);
        log.info("Soft-deleted JD: userId={}, jdId={}", userId, id);
    }

    @Transactional(readOnly = true)
    public DownloadResult downloadJobDescription(Long id, Long userId, boolean isAdmin) {
        JobDescription jd = findForUser(id, userId, isAdmin);
        if (jd.getFilePath() == null) {
            throw new JobDescriptionNotFoundException(id);
        }
        InputStream content = storageService.load(jd.getFilePath());
        return new DownloadResult(jd.getTitle(), jd.getContentType(), content);
    }

    private JobDescription findForUser(Long id, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return repository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new JobDescriptionNotFoundException(id));
        }
        return repository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new JobDescriptionNotFoundException(id));
    }

    private String storeFile(MultipartFile file, Long userId) {
        try {
            return storageService.store(userId, file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }
    }

    private String readPlainText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Failed to read text file", e);
        }
    }

    private boolean isPlainText(MultipartFile file) {
        return "text/plain".equals(file.getContentType());
    }

    public record DownloadResult(String title, String contentType, InputStream content) {}
}
