package com.socialapp.backend.dto;

public record NotificationPreferencesDto(
        boolean notifyMessages,
        boolean notifyPosts
) {
}
