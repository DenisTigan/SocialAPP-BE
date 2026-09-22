package com.socialapp.backend.repository;

import com.socialapp.backend.entity.Message;
import com.socialapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query("SELECT m FROM Message m WHERE (m.sender = :userA AND m.receiver = :userB) " +
            "OR (m.sender = :userB AND m.receiver = :userA) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("userA") User userA, @Param("userB") User userB);

    // Adaugă sub query-ul existent pentru istoric
    List<Message> findBySenderOrReceiverOrderByCreatedAtDesc(User sender, User receiver);
}
