package com.princeramteke.resumeai.auth.exception;

public class OtpResendTooSoonException extends RuntimeException {

    public OtpResendTooSoonException(int cooldownSeconds) {
        super("Please wait " + cooldownSeconds + " seconds before requesting a new code");
    }
}
