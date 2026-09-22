package com.socialapp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
        @NotBlank(message = "Mesajul nu poate fi gol")
        String content
) {
}
