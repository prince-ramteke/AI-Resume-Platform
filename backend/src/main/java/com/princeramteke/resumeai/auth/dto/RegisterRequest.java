package com.princeramteke.resumeai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "must be at least 8 characters")
        @Pattern(regexp = ".*[a-zA-Z].*", message = "must contain at least one letter")
        @Pattern(regexp = ".*\\d.*", message = "must contain at least one digit")
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName
) {
}
