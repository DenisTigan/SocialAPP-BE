package com.socialapp.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID userId,
        String username,
        String avatarUrl,
        String text,
        LocalDateTime createdAt
) {
}
