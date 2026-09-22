package com.socialapp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
        @NotBlank(message = "Email-ul este obligatoriu")
        String email,

        @NotBlank(message = "Codul este obligatoriu")
        String code
) {
}
