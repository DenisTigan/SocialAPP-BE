package com.socialapp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email-ul este obligatoriu")
        String email,

        @NotBlank(message = "Parola este obligatorie")
        String password
) {
}
