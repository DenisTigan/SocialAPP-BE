package com.socialapp.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.backend.entity.PushSubscription;
import com.socialapp.backend.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Inițializăm PushService conform documentației oficiale (pentru request-uri sincrone)
    private PushService pushService;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }

    public void sendToUser(UUID userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = subscriptionRepository.findAllByUser_Id(userId);
        logger.info("=> DEBUG PUSH: sendToUser a fost apelat pentru userId={}, subscriptions găsite în DB={}", userId, subscriptions.size());

        if (subscriptions.isEmpty()) {
            logger.warn("=> DEBUG PUSH: Anulat! Userul {} nu are niciun device abonat.", userId);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body,
                    "url", url
            ));

            for (PushSubscription sub : subscriptions) {
                logger.info("=> DEBUG PUSH: Se trimite notificarea către endpoint-ul: {}", sub.getEndpoint());
                sendPushMessage(sub, payload);
            }
        } catch (Exception e) {
            logger.error("=> DEBUG PUSH EROARE: Eroare la parsarea sau trimiterea notificărilor", e);
        }
    }

    private void sendPushMessage(PushSubscription sub, String payload) {
        int maxRetries = 2;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes()
                );

                pushService.send(notification);
                logger.info("=> DEBUG PUSH: Notificare trimisă cu succes la încercarea {}!", attempt);
                return; // Succes! Ieșim din loop.

            } catch (ExecutionException e) {
                if (e.getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
                    logger.warn("=> DEBUG PUSH: Eroare SNI detectată (încercarea {}). Se reîncearcă conexiunea...", attempt);
                    if (attempt == maxRetries) {
                        logger.error("=> DEBUG PUSH: Eșec definitiv după retry-uri.", e);
                    } else {
                        try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                } else {
                    logger.error("=> DEBUG PUSH: Altă eroare de execuție.", e);
                    break;
                }
            } catch (Exception e) {
                logger.error("=> DEBUG PUSH: Eroare generală la trimitere.", e);
                break;
            }
        }
    }
}
