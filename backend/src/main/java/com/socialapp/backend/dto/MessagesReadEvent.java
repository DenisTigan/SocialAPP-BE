package com.socialapp.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessagesReadEvent(
        UUID readerId,
        LocalDateTime readAt
) {
}
