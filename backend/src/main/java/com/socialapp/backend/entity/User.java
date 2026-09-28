package com.socialapp.backend.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Contul ramane inactiv (implicit false) pana la verificare
    @Column(nullable = false)
    private boolean enabled = false;

    // --- CÂMPURI NOI PENTRU NOTIFICĂRI PUSH ---
    @Column(name = "notify_messages", nullable = false, columnDefinition = "boolean default true")
    private boolean notifyMessages = true;

    @Column(name = "notify_posts", nullable = false, columnDefinition = "boolean default true")
    private boolean notifyPosts = true;
    // ------------------------------------------

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- GETTERS și SETTERS ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Getters / Setters pentru notificări
    public boolean isNotifyMessages() { return notifyMessages; }
    public void setNotifyMessages(boolean notifyMessages) { this.notifyMessages = notifyMessages; }

    public boolean isNotifyPosts() { return notifyPosts; }
    public void setNotifyPosts(boolean notifyPosts) { this.notifyPosts = notifyPosts; }

}
