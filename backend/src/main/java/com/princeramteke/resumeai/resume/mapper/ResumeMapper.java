package com.princeramteke.resumeai.resume.mapper;

import com.princeramteke.resumeai.resume.Resume;
import com.princeramteke.resumeai.resume.dto.ResumeMetadataResponse;
import com.princeramteke.resumeai.resume.dto.ResumeResponse;
import com.princeramteke.resumeai.resume.dto.ResumeSummaryResponse;
import com.princeramteke.resumeai.resume.dto.UploadResumeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

    ResumeResponse toResponse(Resume resume);

    ResumeSummaryResponse toSummary(Resume resume);

    UploadResumeResponse toUploadResponse(Resume resume);

    ResumeMetadataResponse toMetadata(Resume resume);
}
