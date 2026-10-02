package com.socialapp.backend.dto;

import java.util.UUID;

public record PresenceEvent(
        UUID userId,
        boolean online
) {
}
