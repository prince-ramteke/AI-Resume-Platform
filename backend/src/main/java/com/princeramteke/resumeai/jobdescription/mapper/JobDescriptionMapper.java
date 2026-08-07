package com.princeramteke.resumeai.jobdescription.mapper;

import com.princeramteke.resumeai.jobdescription.JobDescription;
import com.princeramteke.resumeai.jobdescription.dto.JobDescriptionResponse;
import com.princeramteke.resumeai.jobdescription.dto.JobDescriptionSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobDescriptionMapper {

    JobDescriptionResponse toResponse(JobDescription jobDescription);

    JobDescriptionSummaryResponse toSummary(JobDescription jobDescription);
}
