package com.princeramteke.resumeai.jobdescription;

import com.princeramteke.resumeai.jobdescription.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/job-descriptions")
@Tag(name = "Job Descriptions", description = "Create, retrieve, update, and delete job descriptions")
public class JobDescriptionController {

    private final JobDescriptionService service;

    public JobDescriptionController(JobDescriptionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a job description from pasted text")
    public ResponseEntity<JobDescriptionResponse> createFromText(
            @Valid @RequestBody CreateJobDescriptionRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        JobDescriptionResponse response = service.createFromText(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a job description from an uploaded file (PDF, DOCX, or TXT)")
    public ResponseEntity<JobDescriptionResponse> createFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") @NotBlank @Size(max = 255) String title,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        JobDescriptionResponse response = service.createFromFile(title, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's job descriptions (paginated, optional title search)")
    public ResponseEntity<Page<JobDescriptionSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Page<JobDescriptionSummaryResponse> page = service.listJobDescriptions(
                userId, search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job description by ID (includes full text)")
    public ResponseEntity<JobDescriptionResponse> get(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        JobDescriptionResponse response = service.getJobDescription(
                id, userId, isAdmin(authentication));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job description's title and text")
    public ResponseEntity<JobDescriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobDescriptionRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        JobDescriptionResponse response = service.updateJobDescription(
                id, request, userId, isAdmin(authentication));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a job description")
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        service.deleteJobDescription(id, userId, isAdmin(authentication));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the original uploaded file (only for file-based JDs)")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        JobDescriptionService.DownloadResult result = service.downloadJobDescription(
                id, userId, isAdmin(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        result.contentType() != null ? result.contentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.title() + "\"")
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
