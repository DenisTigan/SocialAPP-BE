package com.socialapp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username este obligatoriu")
        @Size(min = 3, max = 50, message = "Username trebuie sa aiba intre 3 si 50 caractere")
        String username,

        @NotBlank(message = "Email-ul este obligatoriu")
        @Email(message = "Formatul email-ului este invalid")
        String email,

        @NotBlank(message = "Parola este obligatorie")
        @Size(min = 6, message = "Parola trebuie sa aiba minim 6 caractere")
        String password
) {
}
