package com.socialapp.backend.repository;

import com.socialapp.backend.entity.Photo;
import com.socialapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    long countByUser(User user);
    List<Photo> findByUserOrderByCreatedAtDesc(User user);
}
