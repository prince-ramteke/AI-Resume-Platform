package com.princeramteke.resumeai.analysis.mapper;

import com.princeramteke.resumeai.analysis.Analysis;
import com.princeramteke.resumeai.analysis.dto.AnalysisResponse;
import com.princeramteke.resumeai.analysis.dto.AnalysisSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the {@link Analysis} entity to its API DTOs. MapStruct bridges the JSONB model records
 * to the matching response records automatically (same component names), keeping the persistence
 * shape off the API boundary. The summary pulls {@code jobTitle} from the associated job
 * description (fetched via entity graph on the list query, so no N+1).
 */
@Mapper(componentModel = "spring")
public interface AnalysisMapper {

    AnalysisResponse toResponse(Analysis analysis);

    @Mapping(target = "jobTitle", source = "jobDescription.title")
    AnalysisSummaryResponse toSummary(Analysis analysis);
}
