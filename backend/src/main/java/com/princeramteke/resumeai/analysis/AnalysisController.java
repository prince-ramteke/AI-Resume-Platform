package com.princeramteke.resumeai.analysis;

import com.princeramteke.resumeai.analysis.dto.AnalysisRequest;
import com.princeramteke.resumeai.analysis.dto.AnalysisResponse;
import com.princeramteke.resumeai.analysis.dto.AnalysisSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST API for resume-vs-job-description analyses. All routes require authentication; ownership
 * is enforced in the service (non-owner → 404 to prevent enumeration), with an admin override on
 * single-analysis reads. See API.md §5.
 */
@RestController
@RequestMapping("/api/analyses")
@Tag(name = "Analyses", description = "Run and retrieve resume-vs-job-description analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Run an analysis of a resume against a job description",
            description = "Scores the resume against the JD and returns a grounded verdict. "
                    + "Both resources must be owned by the caller.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Analysis created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Resume or job description not found / not owned"),
            @ApiResponse(responseCode = "422", description = "LLM produced unusable output after repair retry")
    })
    public ResponseEntity<AnalysisResponse> create(
            @Valid @RequestBody AnalysisRequest request,
            Authentication authentication) {
        AnalysisResponse response = service.analyze(request, getUserId(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's analyses (paginated history)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of analysis summaries"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<Page<AnalysisSummaryResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(service.listAnalyses(getUserId(authentication), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single analysis by ID (full result)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The analysis"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Analysis not found / not owned")
    })
    public ResponseEntity<AnalysisResponse> get(
            @PathVariable Long id,
            Authentication authentication) {
        AnalysisResponse response = service.getAnalysis(
                id, getUserId(authentication), isAdmin(authentication));
        return ResponseEntity.ok(response);
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
