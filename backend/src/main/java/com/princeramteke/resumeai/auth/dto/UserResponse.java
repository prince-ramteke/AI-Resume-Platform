package com.princeramteke.resumeai.auth.dto;

public record UserResponse(Long id, String email, String role, boolean emailVerified, String authProvider) {
}
