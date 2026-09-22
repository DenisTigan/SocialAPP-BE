package com.socialapp.backend.controller;

import com.socialapp.backend.dto.ConversationResponse;
import com.socialapp.backend.dto.MessageRequest;
import com.socialapp.backend.dto.MessageResponse;
import com.socialapp.backend.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Endpoint pentru trimiterea unui mesaj
    @PostMapping("/{receiverId}")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String receiverId,
            @Valid @RequestBody MessageRequest request) {

        // Luăm ID-ul utilizatorului autentificat direct din JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderId = authentication.getName();

        MessageResponse response = messageService.sendMessage(senderId, receiverId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint pentru citirea istoricului cu un anumit utilizator
    @GetMapping("/{partnerId}")
    public ResponseEntity<List<MessageResponse>> getChatHistory(@PathVariable String partnerId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        List<MessageResponse> history = messageService.getChatHistory(currentUserId, partnerId);
        return ResponseEntity.ok(history);
    }
    @GetMapping("/inbox")
    public ResponseEntity<List<ConversationResponse>> getInbox() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        List<ConversationResponse> inbox = messageService.getConversations(userId);
        return ResponseEntity.ok(inbox);
    }
}
