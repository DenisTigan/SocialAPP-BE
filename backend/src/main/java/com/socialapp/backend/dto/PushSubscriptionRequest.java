package com.socialapp.backend.dto;

import io.jsonwebtoken.security.Keys;

public record PushSubscriptionRequest(
        String endpoint,
        Keys keys
) {
    public record Keys(String p256dh, String auth) {}
}
