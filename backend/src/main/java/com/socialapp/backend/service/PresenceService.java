package com.socialapp.backend.service;
import com.socialapp.backend.dto.PresenceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    // Mapare sessionId -> userId (pentru a ști cine s-a deconectat)
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();

    // Mapare userId -> Set de sessionIds (suportă mai multe tab-uri deschise simultan)
    private final Map<UUID, Set<String>> userSessionsMap = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void userConnected(String sessionId, String userIdStr) {
        if (sessionId == null || userIdStr == null) return;

        UUID userId = UUID.fromString(userIdStr);
        sessionUserMap.put(sessionId, userId);

        Set<String> sessions = userSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(sessionId);

        // Dacă este prima sesiune a userului, anunțăm că a devenit ONLINE
        if (wasOffline) {
            logger.info("=> PRESENCE: User {} este acum ONLINE", userId);
            messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(userId, true));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        UUID userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                // Dacă nu mai are niciun tab deschis, anunțăm că a devenit OFFLINE
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(userId);
                    logger.info("=> PRESENCE: User {} este acum OFFLINE", userId);
                    messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(userId, false));
                }
            }
        }
    }

    public boolean isUserOnline(UUID userId) {
        Set<String> sessions = userSessionsMap.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<UUID> getOnlineUserIds() {
        return userSessionsMap.keySet();
    }
}
