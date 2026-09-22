package com.socialapp.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        String content,
        LocalDateTime createdAt
) {
}
