package com.socialapp.backend.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record UserProfileResponse(

        UUID id,
        String username,
        String bio,
        String avatarUrl,
        boolean online,
        LocalDateTime createdAt,
        long postsCount,
        long totalLikesReceived
) {
}
