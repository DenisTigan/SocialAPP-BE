package com.socialapp.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialapp.backend.dto.CommentRequest;
import com.socialapp.backend.dto.CommentResponse;
import com.socialapp.backend.dto.PhotoResponse;
import com.socialapp.backend.dto.UpdatePhotoRequest;
import com.socialapp.backend.entity.Comment;
import com.socialapp.backend.entity.Photo;
import com.socialapp.backend.entity.PhotoLike;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.CommentRepository;
import com.socialapp.backend.repository.PhotoLikeRepository;
import com.socialapp.backend.repository.PhotoRepository;
import com.socialapp.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PhotoService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    private final Cloudinary cloudinary;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PhotoLikeRepository photoLikeRepository;
    private final CommentRepository commentRepository;
    private final PushNotificationService pushNotificationService;

    public PhotoService(Cloudinary cloudinary, PhotoRepository photoRepository,
                        UserRepository userRepository, PhotoLikeRepository photoLikeRepository,
                        CommentRepository commentRepository,
                        PushNotificationService pushNotificationService) {
        this.cloudinary = cloudinary;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.photoLikeRepository = photoLikeRepository;
        this.commentRepository = commentRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional
    public PhotoResponse uploadPhoto(MultipartFile file, String caption, String userId) throws IOException {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit!"));

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String imageUrl = uploadResult.get("secure_url").toString();
        String publicId = uploadResult.get("public_id").toString(); // <-- NOU

        Photo photo = new Photo();
        photo.setUser(user);
        photo.setImageUrl(imageUrl);
        photo.setPublicId(publicId); // <-- Salvăm public_id pentru ștergere ulterioară
        photo.setCaption(caption);

        Photo savedPhoto = photoRepository.save(photo);

        CompletableFuture.runAsync(() -> {
            List<User> targetUsers = userRepository.findAllByNotifyPostsTrueAndIdNot(user.getId());

            for (User target : targetUsers) {
                pushNotificationService.sendToUser(
                        target.getId(),
                        "Postare nouă",
                        user.getUsername() + " a postat o poză nouă",
                        "/feed"
                );
            }
        });

        return new PhotoResponse(
                savedPhoto.getId(),
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                savedPhoto.getImageUrl(),
                savedPhoto.getCaption(),
                savedPhoto.getCreatedAt(),
                0,
                false
        );
    }

    public Page<PhotoResponse> getFeed(int page, int size, String currentUserId) {
        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Photo> photos = photoRepository.findAll(pageRequest);

        return photos.map(photo -> {
            long likeCount = photoLikeRepository.countByPhoto(photo);
            boolean isLiked = photoLikeRepository.existsByUserAndPhoto(currentUser, photo);

            return new PhotoResponse(
                    photo.getId(),
                    photo.getUser().getId(),
                    photo.getUser().getUsername(),
                    photo.getUser().getAvatarUrl(),
                    photo.getImageUrl(),
                    photo.getCaption(),
                    photo.getCreatedAt(),
                    likeCount,
                    isLiked
            );
        });
    }

    public List<PhotoResponse> getUserPhotos(String targetUserId, String currentUserId) {
        User targetUser = userRepository.findById(UUID.fromString(targetUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul căutat nu a fost găsit!"));

        User currentUser = userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul curent nu a fost găsit!"));

        List<Photo> userPhotos = photoRepository.findByUserOrderByCreatedAtDesc(targetUser);

        return userPhotos.stream()
                .map(photo -> {
                    long likeCount = photoLikeRepository.countByPhoto(photo);
                    boolean isLiked = photoLikeRepository.existsByUserAndPhoto(currentUser, photo);

                    return new PhotoResponse(
                            photo.getId(),
                            targetUser.getId(),
                            targetUser.getUsername(),
                            targetUser.getAvatarUrl(),
                            photo.getImageUrl(),
                            photo.getCaption(),
                            photo.getCreatedAt(),
                            likeCount,
                            isLiked
                    );
                })
                .toList();
    }

    // --- NOU: EDITARE DESCRIERE (CAPTION) POZĂ ---
    @Transactional
    public PhotoResponse updatePhotoCaption(String photoId, String userId, UpdatePhotoRequest request) {
        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost găsită!"));

        User currentUser = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        if (!photo.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Nu ai permisiunea să editezi această postare!");
        }

        String newCaption = request.caption() != null ? request.caption().trim() : "";
        if (newCaption.length() > 500) {
            throw new RuntimeException("Descrierea nu poate depăși 500 de caractere!");
        }

        photo.setCaption(newCaption);
        Photo savedPhoto = photoRepository.save(photo);

        long likeCount = photoLikeRepository.countByPhoto(savedPhoto);
        boolean isLiked = photoLikeRepository.existsByUserAndPhoto(currentUser, savedPhoto);

        return new PhotoResponse(
                savedPhoto.getId(),
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getAvatarUrl(),
                savedPhoto.getImageUrl(),
                savedPhoto.getCaption(),
                savedPhoto.getCreatedAt(),
                likeCount,
                isLiked
        );
    }

    // --- NOU: ȘTERGERE POZĂ (DB + CLOUDINARY + LIKES + COMMENTS) ---
    @Transactional
    public void deletePhoto(String photoId, String userId) {
        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost găsită!"));

        if (!photo.getUser().getId().toString().equals(userId)) {
            throw new RuntimeException("Nu ai permisiunea să ștergi această postare!");
        }

        // 1. Ștergem întâi like-urile și comentariile asociate pozei (evităm Foreign Key error)
        photoLikeRepository.deleteByPhoto(photo);
        commentRepository.deleteByPhoto(photo);

        // 2. Ștergem fișierul din Cloudinary (dacă are publicId salvat)
        if (photo.getPublicId() != null && !photo.getPublicId().isEmpty()) {
            try {
                cloudinary.uploader().destroy(photo.getPublicId(), ObjectUtils.emptyMap());
            } catch (Exception e) {
                logger.warn("=> WARN: Nu s-a putut șterge poza din Cloudinary (publicId={}): {}", photo.getPublicId(), e.getMessage());
            }
        }

        // 3. Ștergem poza din baza de date
        photoRepository.delete(photo);
    }

    @Transactional
    public String toggleLike(String photoId, String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit!"));

        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost gasita!"));

        Optional<PhotoLike> existingLike = photoLikeRepository.findByUserAndPhoto(user, photo);

        if (existingLike.isPresent()) {
            photoLikeRepository.delete(existingLike.get());
            return "Like sters cu succes!";
        } else {
            PhotoLike newLike = new PhotoLike();
            newLike.setUser(user);
            newLike.setPhoto(photo);
            photoLikeRepository.save(newLike);
            return "Like adaugat cu succes!";
        }
    }

    @Transactional
    public CommentResponse addComment(String photoId, String userId, CommentRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost găsită!"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPhoto(photo);
        comment.setText(request.text().trim());

        Comment savedComment = commentRepository.save(comment);

        return new CommentResponse(
                savedComment.getId(),
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                savedComment.getText(),
                savedComment.getCreatedAt()
        );
    }

    public List<CommentResponse> getCommentsForPhoto(String photoId) {
        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost găsită!"));

        List<Comment> comments = commentRepository.findByPhotoOrderByCreatedAtAsc(photo);

        return comments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getUser().getAvatarUrl(),
                        comment.getText(),
                        comment.getCreatedAt()
                ))
                .toList();
    }

    // --- NOU: EDITARE COMENTARIU PROPRIU ---
    @Transactional
    public CommentResponse updateComment(String commentId, String userId, CommentRequest request) {
        Comment comment = commentRepository.findById(UUID.fromString(commentId))
                .orElseThrow(() -> new RuntimeException("Comentariul nu a fost găsit!"));

        if (!comment.getUser().getId().toString().equals(userId)) {
            throw new RuntimeException("Nu poți edita comentariul altui utilizator!");
        }

        comment.setText(request.text().trim());
        Comment savedComment = commentRepository.save(comment);

        return new CommentResponse(
                savedComment.getId(),
                savedComment.getUser().getId(),
                savedComment.getUser().getUsername(),
                savedComment.getUser().getAvatarUrl(),
                savedComment.getText(),
                savedComment.getCreatedAt()
        );
    }

    // --- NOU: ȘTERGERE COMENTARIU (Autorul comentariului SAU Proprietarul pozei) ---
    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(UUID.fromString(commentId))
                .orElseThrow(() -> new RuntimeException("Comentariul nu a fost găsit!"));

        boolean isCommentAuthor = comment.getUser().getId().toString().equals(userId);
        boolean isPhotoOwner = comment.getPhoto().getUser().getId().toString().equals(userId);

        if (!isCommentAuthor && !isPhotoOwner) {
            throw new RuntimeException("Nu ai permisiunea să ștergi acest comentariu!");
        }

        commentRepository.delete(comment);
    }
}
