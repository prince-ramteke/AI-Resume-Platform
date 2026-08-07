package com.princeramteke.resumeai.resume;

import com.princeramteke.resumeai.resume.dto.ResumeResponse;
import com.princeramteke.resumeai.resume.dto.ResumeSummaryResponse;
import com.princeramteke.resumeai.resume.dto.UploadResumeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@Tag(name = "Resumes", description = "Resume upload, retrieval, replacement, and deletion")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a resume (PDF or DOCX, max 10 MB)")
    public ResponseEntity<UploadResumeResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        UploadResumeResponse response = resumeService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's resumes (paginated)")
    public ResponseEntity<Page<ResumeSummaryResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Page<ResumeSummaryResponse> page = resumeService.listResumes(userId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resume by ID (includes extracted text)")
    public ResponseEntity<ResumeResponse> get(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        ResumeResponse response = resumeService.getResume(id, userId, isAdmin(authentication));
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace a resume with a new file")
    public ResponseEntity<UploadResumeResponse> replace(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        UploadResumeResponse response = resumeService.replaceResume(
                id, file, userId, isAdmin(authentication));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        resumeService.deleteResume(id, userId, isAdmin(authentication));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the original resume file")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        ResumeService.DownloadResult result = resumeService.downloadResume(
                id, userId, isAdmin(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        result.contentType() != null ? result.contentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .body(new InputStreamResource(result.content()));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
