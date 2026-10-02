package com.socialapp.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID partnerId,
        String partnerUsername,
        String partnerAvatarUrl,
        boolean partnerOnline,
        String lastMessage,
        LocalDateTime timestamp,
        UUID lastMessageSenderId,
        boolean lastMessageRead,
        long unreadCount
) {
}
