package com.princeramteke.resumeai.auth.exception;

public class OAuthExchangeCodeExpiredException extends RuntimeException {

    public OAuthExchangeCodeExpiredException() {
        super("OAuth exchange code has expired or was already used");
    }
}
