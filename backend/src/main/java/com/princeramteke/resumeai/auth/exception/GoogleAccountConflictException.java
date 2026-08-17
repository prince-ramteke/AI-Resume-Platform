package com.princeramteke.resumeai.auth.exception;

public class GoogleAccountConflictException extends RuntimeException {

    public GoogleAccountConflictException(String email) {
        super("A local account already exists for " + email
                + ". Sign in with your password and connect Google from your account settings.");
    }
}
