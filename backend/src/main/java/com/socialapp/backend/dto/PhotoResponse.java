package com.socialapp.backend.dto;
import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        UUID userId,
        String username,
        String imageUrl,
        String caption,
        LocalDateTime createdAt,
        long likeCount,
        boolean isLikedByCurrentUser
) {
}
