package com.princeramteke.resumeai.auth.exception;

public class OtpInvalidException extends RuntimeException {

    public OtpInvalidException() {
        super("Invalid or expired OTP");
    }
}
