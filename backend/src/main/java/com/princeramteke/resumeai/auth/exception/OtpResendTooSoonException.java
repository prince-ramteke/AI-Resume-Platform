package com.princeramteke.resumeai.auth.exception;

public class OtpResendTooSoonException extends RuntimeException {

    private final int retryAfterSeconds;

    public OtpResendTooSoonException(int retryAfterSeconds) {
        super("Please wait before requesting a new code");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
