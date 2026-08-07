package com.princeramteke.resumeai.jobdescription.exception;

public class JobDescriptionNotFoundException extends RuntimeException {

    public JobDescriptionNotFoundException(Long id) {
        super("Job description not found: " + id);
    }
}
