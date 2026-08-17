package com.princeramteke.resumeai.auth.exception;

public class TooManyOtpAttemptsException extends RuntimeException {

    public TooManyOtpAttemptsException() {
        super("Too many verification attempts. Request a new code.");
    }
}
