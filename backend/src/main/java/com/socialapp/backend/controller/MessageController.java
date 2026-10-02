package com.socialapp.backend.controller;

import com.socialapp.backend.dto.ConversationResponse;
import com.socialapp.backend.dto.MessageRequest;
import com.socialapp.backend.dto.MessageResponse;
import com.socialapp.backend.dto.TypingRequest;
import com.socialapp.backend.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // 1. Trimiterea unui mesaj: POST /api/messages/{receiverId}
    @PostMapping("/{receiverId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String receiverId,
            @Valid @RequestBody MessageRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderId = authentication.getName();

        MessageResponse response = messageService.sendMessage(senderId, receiverId, request);
        return ResponseEntity.ok(response);
    }

    // 2. Citirea istoricului cu un anumit utilizator (marchează automat și ca "Văzut"): GET /api/messages/{partnerId}
    @GetMapping("/{partnerId}")
    public ResponseEntity<List<MessageResponse>> getChatHistory(@PathVariable String partnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        List<MessageResponse> history = messageService.getChatHistory(currentUserId, partnerId);
        return ResponseEntity.ok(history);
    }

    // 3. NOU: Marchează mesajele de la partnerId ca "Văzute" (când ești deja în fereastra de chat): PUT /api/messages/{partnerId}/read
    @PutMapping("/{partnerId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String partnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        messageService.markMessagesAsRead(currentUserId, partnerId);
        return ResponseEntity.ok().build();
    }

    // 4. Inbox (Lista conversațiilor): GET /api/messages/inbox
    @GetMapping("/inbox")
    public ResponseEntity<List<ConversationResponse>> getInbox() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        List<ConversationResponse> inbox = messageService.getConversations(userId);
        return ResponseEntity.ok(inbox);
    }

    // 5. NOU: Indicator "Typing..." prin WebSocket (STOMP destination: /app/chat.typing)
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingRequest request, Principal principal) {
        if (principal != null && request.receiverId() != null) {
            messageService.sendTypingStatus(
                    principal.getName(),
                    request.receiverId().toString(),
                    request.typing()
            );
        }
    }

    // 6. NOU: Indicator "Typing..." prin REST (alternativă la STOMP): POST /api/messages/{receiverId}/typing?typing=true
    @PostMapping("/{receiverId}/typing")
    public ResponseEntity<Void> sendTypingRest(
            @PathVariable String receiverId,
            @RequestParam(defaultValue = "true") boolean typing) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderId = authentication.getName();

        messageService.sendTypingStatus(senderId, receiverId, typing);
        return ResponseEntity.ok().build();
    }
}
