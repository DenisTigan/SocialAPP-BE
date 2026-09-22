package com.socialapp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequest(
        @NotBlank(message = "Email-ul este obligatoriu")
        String email
) {
}
