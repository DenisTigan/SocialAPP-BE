package com.socialapp.backend.dto;

import java.util.UUID;

public record TypingEvent(
        UUID senderId,
        boolean typing
) {
}
