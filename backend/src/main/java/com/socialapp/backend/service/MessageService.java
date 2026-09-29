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
    private final SimpMessagingTemplate messagingTemplate; // <-- Adăugat
    private final PushNotificationService pushNotificationService; // <-- ADAUGĂ SERVICIUL NOU

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    // Injectăm toate cele 4 dependințe
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

        // Trimiterea WebSocket (pentru când aplicația este deschisă)
        messagingTemplate.convertAndSendToUser(
                receiverId,
                "/queue/messages",
                response
        );


        logger.info("=> DEBUG MESSAGE: Verificăm preferințele pentru receiver-ul (ID: {}). isNotifyMessages real din DB = {}", receiver.getId(), receiver.isNotifyMessages());

        // --- NOU: VERIFICĂM ȘI TRIMITEM NOTIFICARE PUSH PENTRU CÂND E ÎNCHISĂ ---
        if (receiver.isNotifyMessages()) {

            logger.info("=> DEBUG MESSAGE: isNotifyMessages este TRUE. Se apelează sendToUser asincron...");


            // Scurtăm mesajul dacă e prea lung pentru o notificare
            String previewText = request.content();
            if (previewText.length() > 50) {
                previewText = previewText.substring(0, 47) + "...";
            }

            final String finalPreview = previewText;
            // Trimitem asincron pentru a nu întârzia răspunsul HTTP către expeditor
            CompletableFuture.runAsync(() -> {
                pushNotificationService.sendToUser(
                        receiver.getId(),
                        sender.getUsername() + " ți-a trimis un mesaj",
                        finalPreview,
                        "/messages/" + sender.getId() // Aici va duce clicul pe notificare
                );
            });
        }else {
            logger.warn("=> DEBUG MESSAGE: Nu s-a apelat sendToUser deoarece isNotifyMessages este FALSE!");
        }

        return response;
    }

    public List<MessageResponse> getChatHistory(String currentUserId, String partnerId) {
        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul curent nu a fost găsit!"));

        User partner = userRepository.findById(UUID.fromString(partnerId))
                .orElseThrow(() -> new RuntimeException("Partenerul de discuție nu a fost găsit!"));

        // Apelăm query-ul personalizat care aduce mesajele din ambele sensuri
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

        // 1. Luăm toate mesajele userului, deja sortate descrescător (cele mai noi primele)
        List<Message> allMessages = messageRepository.findBySenderOrReceiverOrderByCreatedAtDesc(user, user);

        // 2. Folosim un LinkedHashMap pentru a păstra ordinea cronologică și a elimina duplicatele de parteneri
        Map<UUID, ConversationResponse> conversations = new LinkedHashMap<>();

        for (Message msg : allMessages) {
            // Determinăm cine este "celălalt" utilizator din mesaj
            User partner = msg.getSender().equals(user) ? msg.getReceiver() : msg.getSender();

            // Dacă nu am adăugat deja o conversație cu acest partener, o adăugăm (fiind prima, e și cea mai recentă)
            if (!conversations.containsKey(partner.getId())) {
                conversations.put(partner.getId(), new ConversationResponse(
                        partner.getId(),
                        partner.getUsername(),
                        msg.getContent(),
                        msg.getCreatedAt()
                ));
            }
        }

        return new ArrayList<>(conversations.values());
    }
}
