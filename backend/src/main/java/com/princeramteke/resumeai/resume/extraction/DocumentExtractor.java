package com.princeramteke.resumeai.resume.extraction;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentExtractor {
    ExtractionResult extract(MultipartFile file);
}
