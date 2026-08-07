package com.princeramteke.resumeai.resume.exception;

public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException(Long id) {
        super("Resume not found: " + id);
    }
}
