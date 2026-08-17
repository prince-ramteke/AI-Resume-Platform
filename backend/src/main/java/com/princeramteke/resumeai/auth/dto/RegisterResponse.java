package com.princeramteke.resumeai.auth.dto;

public record RegisterResponse(Long id, String email, String role, boolean emailVerificationRequired) {
}
