package com.socialapp.backend.service;


import com.socialapp.backend.dto.*;
import com.socialapp.backend.entity.Message;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.MessageRepository;
import com.socialapp.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;
    private final PresenceService presenceService;

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate,
                          PushNotificationService pushNotificationService,
                          PresenceService presenceService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
        this.presenceService = presenceService;
    }

    @Transactional
    public MessageResponse sendMessage(String senderId, String receiverId, MessageRequest request) {
        User sender = userRepository.findById(UUID.fromString(senderId))
                .orElseThrow(() -> new RuntimeException("Expeditorul nu a fost găsit!"));

        User receiver = userRepository.findById(UUID.fromString(receiverId))
                .orElseThrow(() -> new RuntimeException("Destinatarul nu a fost găsit!"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.content());
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                savedMessage.getId(),
                sender.getId(),
                receiver.getId(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt(),
                savedMessage.isRead(),
                savedMessage.getReadAt()
        );

        // Trimiterea WebSocket către destinatar
        messagingTemplate.convertAndSendToUser(
                receiverId,
                "/queue/messages",
                response
        );

        if (receiver.isNotifyMessages()) {
            String previewText = request.content();
            if (previewText.length() > 50) {
                previewText = previewText.substring(0, 47) + "...";
            }

            final String finalPreview = previewText;
            CompletableFuture.runAsync(() -> {
                pushNotificationService.sendToUser(
                        receiver.getId(),
                        sender.getUsername() + " ți-a trimis un mesaj",
                        finalPreview,
                        "/messages/" + sender.getId()
                );
            });
        }

        return response;
    }

    @Transactional
    public List<MessageResponse> getChatHistory(String currentUserId, String partnerId) {
        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul curent nu a fost găsit!"));

        User partner = userRepository.findById(UUID.fromString(partnerId))
                .orElseThrow(() -> new RuntimeException("Partenerul de discuție nu a fost găsit!"));

        // Când deschidem istoricul cu partenerul, marcăm automat mesajele primite de la el ca "Văzute"
        markMessagesAsReadInternal(currentUser, partner);

        List<Message> history = messageRepository.findChatHistory(currentUser, partner);

        return history.stream()
                .map(msg -> new MessageResponse(
                        msg.getId(),
                        msg.getSender().getId(),
                        msg.getReceiver().getId(),
                        msg.getContent(),
                        msg.getCreatedAt(),
                        msg.isRead(),
                        msg.getReadAt()
                ))
                .toList();
    }

    // --- NOU: Endpoint explicit pentru marcarea mesajelor ca "Văzute" (când fereastra de chat e deja deschisă) ---
    @Transactional
    public void markMessagesAsRead(String currentUserId, String partnerId) {
        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul curent nu a fost găsit!"));

        User partner = userRepository.findById(UUID.fromString(partnerId))
                .orElseThrow(() -> new RuntimeException("Partenerul nu a fost găsit!"));

        markMessagesAsReadInternal(currentUser, partner);
    }

    private void markMessagesAsReadInternal(User currentUser, User partner) {
        List<Message> unreadMessages = messageRepository.findBySenderAndReceiverAndIsReadFalse(partner, currentUser);

        if (!unreadMessages.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Message msg : unreadMessages) {
                msg.setRead(true);
                msg.setReadAt(now);
            }
            messageRepository.saveAll(unreadMessages);

            // Anunțăm expeditorul în timp real prin WebSocket că mesajele lui au fost VĂZUTE
            messagingTemplate.convertAndSendToUser(
                    partner.getId().toString(),
                    "/queue/messages-read",
                    new MessagesReadEvent(currentUser.getId(), now)
            );
        }
    }

    // --- NOU: Trimitere indicator "Typing..." prin WebSocket ---
    public void sendTypingStatus(String senderId, String receiverId, boolean isTyping) {
        TypingEvent event = new TypingEvent(UUID.fromString(senderId), isTyping);
        messagingTemplate.convertAndSendToUser(
                receiverId,
                "/queue/typing",
                event
        );
    }

    public List<ConversationResponse> getConversations(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        List<Message> allMessages = messageRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, user);

        Map<UUID, ConversationResponse> conversations = new LinkedHashMap<>();

        for (Message msg : allMessages) {
            User partner = msg.getSender().getId().equals(user.getId()) ? msg.getReceiver() : msg.getSender();

            if (!conversations.containsKey(partner.getId())) {
                long unreadCount = messageRepository.countBySenderAndReceiverAndIsReadFalse(partner, user);
                boolean isPartnerOnline = presenceService.isUserOnline(partner.getId());

                conversations.put(partner.getId(), new ConversationResponse(
                        partner.getId(),
                        partner.getUsername(),
                        partner.getAvatarUrl(),
                        isPartnerOnline,
                        msg.getContent(),
                        msg.getCreatedAt(),
                        msg.getSender().getId(),
                        msg.isRead(),
                        unreadCount
                ));
            }
        }

        return new ArrayList<>(conversations.values());
    }
}
