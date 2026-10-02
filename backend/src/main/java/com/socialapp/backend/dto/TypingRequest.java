package com.socialapp.backend.dto;

import java.util.UUID;

public record TypingRequest(
        UUID receiverId,
        boolean typing
) {
}
