package com.socialapp.backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username
) {
}
