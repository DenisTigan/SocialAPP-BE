package com.socialapp.backend.service;


import com.socialapp.backend.dto.ConversationResponse;
import com.socialapp.backend.dto.MessageRequest;
import com.socialapp.backend.dto.MessageResponse;
import com.socialapp.backend.entity.Message;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.MessageRepository;
import com.socialapp.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate,
                          PushNotificationService pushNotificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
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

        Message savedMessage = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                savedMessage.getId(),
                sender.getId(),
                receiver.getId(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        messagingTemplate.convertAndSendToUser(
                receiverId,
                "/queue/messages",
                response
        );

        logger.info("=> DEBUG MESSAGE: Verificăm preferințele pentru receiver-ul (ID: {}). isNotifyMessages real din DB = {}", receiver.getId(), receiver.isNotifyMessages());

        if (receiver.isNotifyMessages()) {
            logger.info("=> DEBUG MESSAGE: isNotifyMessages este TRUE. Se apelează sendToUser asincron...");

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
        } else {
            logger.warn("=> DEBUG MESSAGE: Nu s-a apelat sendToUser deoarece isNotifyMessages este FALSE!");
        }

        return response;
    }

    public List<MessageResponse> getChatHistory(String currentUserId, String partnerId) {
        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul curent nu a fost găsit!"));

        User partner = userRepository.findById(UUID.fromString(partnerId))
                .orElseThrow(() -> new RuntimeException("Partenerul de discuție nu a fost găsit!"));

        List<Message> history = messageRepository.findChatHistory(currentUser, partner);

        return history.stream()
                .map(msg -> new MessageResponse(
                        msg.getId(),
                        msg.getSender().getId(),
                        msg.getReceiver().getId(),
                        msg.getContent(),
                        msg.getCreatedAt()
                ))
                .toList();
    }

    public List<ConversationResponse> getConversations(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        List<Message> allMessages = messageRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, user);

        Map<UUID, ConversationResponse> conversations = new LinkedHashMap<>();

        for (Message msg : allMessages) {
            User partner = msg.getSender().equals(user) ? msg.getReceiver() : msg.getSender();

            if (!conversations.containsKey(partner.getId())) {
                conversations.put(partner.getId(), new ConversationResponse(
                        partner.getId(),
                        partner.getUsername(),
                        partner.getAvatarUrl(), // <-- NOU
                        msg.getContent(),
                        msg.getCreatedAt()
                ));
            }
        }

        return new ArrayList<>(conversations.values());
    }
}
