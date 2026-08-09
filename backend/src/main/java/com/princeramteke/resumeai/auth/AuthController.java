package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, login, and current-user endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the access token using a refresh token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        // Public route: the refresh token is the credential. Ownership is derived
        // from the stored token record, so no valid access token is required.
        LoginResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token (logout)")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        // Public route: revocation is authorized by presenting the refresh token itself.
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
