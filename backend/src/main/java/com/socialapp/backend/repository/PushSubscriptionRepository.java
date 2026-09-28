package com.socialapp.backend.repository;

import com.socialapp.backend.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    // Găsește toate device-urile (abonamentele) unui anumit user
    List<PushSubscription> findAllByUser_Id(UUID userId);

    // Caută un abonament exact după endpoint (pentru funcția de upsert)
    Optional<PushSubscription> findByEndpoint(String endpoint);

    // Șterge un abonament (folosit la logout sau când serverul de push returnează 404/410)
    void deleteByEndpoint(String endpoint);
}
