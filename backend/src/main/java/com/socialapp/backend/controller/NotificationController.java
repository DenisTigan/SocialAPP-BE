package com.socialapp.backend.controller;


import com.socialapp.backend.dto.NotificationPreferencesDto;
import com.socialapp.backend.dto.PushSubscriptionRequest;
import com.socialapp.backend.entity.PushSubscription;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.PushSubscriptionRepository;
import com.socialapp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public NotificationController(PushSubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Principal principal) {
        // Adaptează findByEmail cu findByUsername dacă JWT-ul tău are username-ul în subject
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest request, Principal principal) {
        User user = getCurrentUser(principal);

        // La record folosim request.endpoint() în loc de getEndpoint()
        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(request.endpoint());

        if (existing.isPresent()) {
            PushSubscription sub = existing.get();
            sub.setUser(user);
            sub.setP256dh(request.keys().p256dh());
            sub.setAuth(request.keys().auth());
            subscriptionRepository.save(sub);
        } else {
            PushSubscription newSub = new PushSubscription(
                    user,
                    request.endpoint(),
                    request.keys().p256dh(),
                    request.keys().auth()
            );
            subscriptionRepository.save(newSub);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subscriptionRepository.findByEndpoint(endpoint).ifPresent(subscriptionRepository::delete);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDto> getPreferences(Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(new NotificationPreferencesDto(user.isNotifyMessages(), user.isNotifyPosts()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDto> updatePreferences(
            @RequestBody NotificationPreferencesDto request, Principal principal) {
        User user = getCurrentUser(principal);
        // La record folosim request.notifyMessages()
        user.setNotifyMessages(request.notifyMessages());
        user.setNotifyPosts(request.notifyPosts());
        userRepository.save(user);

        return ResponseEntity.ok(new NotificationPreferencesDto(user.isNotifyMessages(), user.isNotifyPosts()));
    }
}
