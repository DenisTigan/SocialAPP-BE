package com.socialapp.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.backend.entity.PushSubscription;
import com.socialapp.backend.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Encoding;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // Instanțele noastre customizate
    private Java11PushService pushService;
    private HttpClient nativeHttpClient;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // Inițializăm serviciul nostru care extinde librăria
        pushService = new Java11PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);

        // Inițializăm clientul nativ modern din Java 11+
        nativeHttpClient = HttpClient.newHttpClient();
    }

    public void sendToUser(UUID userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = subscriptionRepository.findAllByUser_Id(userId);

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
                sendPushMessage(sub, payload);
            }
        } catch (Exception e) {
            logger.error("=> DEBUG PUSH EROARE: Eroare la parsarea sau trimiterea notificărilor", e);
        }
    }

    private void sendPushMessage(PushSubscription sub, String payload) {
        try {
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes()
            );

            // 1. Lăsăm librăria să cripteze datele și să extragem cererea nativă
            HttpRequest request = pushService.buildNativeRequest(notification);

            // 2. Trimitem folosind noul client nativ din Java (ocolim httpasyncclient)
            HttpResponse<Void> response = nativeHttpClient.send(request, HttpResponse.BodyHandlers.discarding());

            int status = response.statusCode();

            // 3. Gestionarea codurilor HTTP
            if (status == 201 || status == 200) {
                logger.info("=> DEBUG PUSH: Notificare trimisă cu succes la endpoint-ul: {}", sub.getEndpoint());
            } else if (status == 404 || status == 410) {
                logger.warn("=> DEBUG PUSH: Endpoint expirat sau invalid (status {}). Se șterge din DB...", status);
                subscriptionRepository.delete(sub);
            } else if (status >= 400) {
                logger.error("=> DEBUG PUSH: Eroare de la serverul Google/Mozilla. Status: {}", status);
            }

        } catch (Exception e) {
            logger.error("=> DEBUG PUSH: Eroare generală la construirea sau trimiterea notificării.", e);
        }
    }

    /**
     * Subclasă internă creată strict pentru a "sparge" bariera metodei protejate preparePost()
     * din interiorul librăriei nl.martijndwars:web-push.
     */
    private static class Java11PushService extends PushService {

        public Java11PushService(String publicKey, String privateKey, String subject) throws GeneralSecurityException {
            super(publicKey, privateKey, subject);
        }

        public HttpRequest buildNativeRequest(Notification notification) throws Exception {
            // Apelăm logica internă a librăriei care generează VAPID, token-uri și payload criptat AES128GCM
            org.apache.http.client.methods.HttpPost apachePost = super.preparePost(notification, Encoding.AES128GCM);

            // Extragem body-ul criptat rezultat
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (apachePost.getEntity() != null) {
                apachePost.getEntity().writeTo(baos);
            }
            byte[] payloadBytes = baos.toByteArray();

            // Îl convertim într-un obiect modern java.net.http.HttpRequest
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apachePost.getURI().toString()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes));

            // Copiem exact header-ele generate de librărie (Authorization, Crypto-Key, TTL, etc.)
            for (org.apache.http.Header header : apachePost.getAllHeaders()) {
                builder.header(header.getName(), header.getValue());
            }

            return builder.build();
        }
    }
}
