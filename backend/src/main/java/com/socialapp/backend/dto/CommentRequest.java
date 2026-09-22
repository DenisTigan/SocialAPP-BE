package com.socialapp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank(message = "Comentariul nu poate fi gol")
        String text
) {
}
