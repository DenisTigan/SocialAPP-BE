package com.socialapp.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.backend.entity.PushSubscription;
import com.socialapp.backend.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    private final ObjectMapper objectMapper = new ObjectMapper(); // <-- Instanțiem direct aici
    private PushService pushService;

    // Modificăm constructorul pentru a cere DOAR repository-ul
    public PushNotificationService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        // Trebuie să înregistrăm BouncyCastle pentru a putea cripta mesajele conform standardului Web Push
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }

    public void sendToUser(UUID userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = subscriptionRepository.findAllByUser_Id(userId);

        logger.info("=> DEBUG PUSH: sendToUser a fost apelat pentru userId={}, subscriptions găsite în DB={}", userId, subscriptions.size());

        if (subscriptions.isEmpty()) {
            logger.warn("=> DEBUG PUSH: Anulat! Userul {} nu are niciun device abonat în tabelul PushSubscription.", userId);
            return; // Userul nu are niciun device abonat
        }

        try {
            // Construim payload-ul pe care frontend-ul îl va decoda
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body,
                    "url", url
            ));

            // Trimitem la fiecare device pe care utilizatorul e logat
            for (PushSubscription sub : subscriptions) {
                logger.info("=> DEBUG PUSH: Se trimite notificarea către endpoint-ul: {}", sub.getEndpoint());
                sendPushMessage(sub, payload);
            }
        } catch (Exception e) {
            // Prindem excepția AICI pentru a nu bloca niciodată fluxul principal (salvarea mesajului)
            logger.error("Eroare la parsarea sau trimiterea notificărilor pentru userul {}", userId, e);
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

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 404 || statusCode == 410) {
                logger.info("Abonament expirat (status {}). Ștergem din DB endpoint-ul: {}", statusCode, sub.getEndpoint());
                subscriptionRepository.delete(sub);
            } else if (statusCode >= 400) {
                logger.warn("Serverul de push a returnat status {} pentru endpoint-ul {}", statusCode, sub.getEndpoint());
            }

        } catch (Exception e) {
            logger.error("Nu s-a putut trimite notificarea la endpoint-ul {}", sub.getEndpoint(), e);
        }
    }
}
