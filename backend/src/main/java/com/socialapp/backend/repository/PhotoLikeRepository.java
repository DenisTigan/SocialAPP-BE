package com.socialapp.backend.repository;

import com.socialapp.backend.entity.Photo;
import com.socialapp.backend.entity.PhotoLike;
import com.socialapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoLikeRepository extends JpaRepository<PhotoLike, UUID> {
    Optional<PhotoLike> findByUserAndPhoto(User user, Photo photo);
    // Adaugate acum:
    long countByPhoto(Photo photo);
    boolean existsByUserAndPhoto(User user, Photo photo);
}



