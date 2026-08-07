package com.princeramteke.resumeai.resume.exception;

public class ResumeTooLargeException extends RuntimeException {

    public ResumeTooLargeException() {
        super("File exceeds maximum size of 10 MB");
    }
}
