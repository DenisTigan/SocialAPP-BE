package com.socialapp.backend.repository;

import com.socialapp.backend.entity.Comment;
import com.socialapp.backend.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPhotoOrderByCreatedAtAsc(Photo photo);
}
