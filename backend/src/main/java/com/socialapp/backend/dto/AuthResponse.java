package com.socialapp.backend.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username
) {
}
