package com.socialapp.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialapp.backend.dto.CommentRequest;
import com.socialapp.backend.dto.CommentResponse;
import com.socialapp.backend.dto.PhotoResponse;
import com.socialapp.backend.entity.Comment;
import com.socialapp.backend.entity.Photo;
import com.socialapp.backend.entity.PhotoLike;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.CommentRepository;
import com.socialapp.backend.repository.PhotoLikeRepository;
import com.socialapp.backend.repository.PhotoRepository;
import com.socialapp.backend.repository.UserRepository;
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

@Service
public class PhotoService {


    private final Cloudinary cloudinary;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PhotoLikeRepository photoLikeRepository;
    private final CommentRepository commentRepository; // <-- Adaugă asta

    public PhotoService(Cloudinary cloudinary, PhotoRepository photoRepository,
                        UserRepository userRepository, PhotoLikeRepository photoLikeRepository,
                        CommentRepository commentRepository) { // <-- Adaugă parametrul aici
        this.cloudinary = cloudinary;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.photoLikeRepository = photoLikeRepository;
        this.commentRepository = commentRepository; // <-- Atribuie-l aici
    }
    @Transactional
    public PhotoResponse uploadPhoto(MultipartFile file, String caption, String userId) throws IOException {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit!"));

        // Trimitere simpla, lasand SDK-ul sa semneze cu API Secret[cite: 1]
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String imageUrl = uploadResult.get("url").toString();

        Photo photo = new Photo();
        photo.setUser(user);
        photo.setImageUrl(imageUrl);
        photo.setCaption(caption);

        Photo savedPhoto = photoRepository.save(photo);

        return new PhotoResponse(
                savedPhoto.getId(),
                user.getId(),
                user.getUsername(),
                savedPhoto.getImageUrl(),
                savedPhoto.getCaption(),
                savedPhoto.getCreatedAt()
        );
    }

    public Page<PhotoResponse> getFeed(int page, int size) {
        // Cream un PageRequest: pagina X, cu Y elemente, sortate descrescator dupa "createdAt"
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // JpaRepository are deja metoda findAll(Pageable) implementata standard
        Page<Photo> photos = photoRepository.findAll(pageRequest);

        // Mapam entitatile Photo in PhotoResponse (DTO-ul nostru)
        return photos.map(photo -> new PhotoResponse(
                photo.getId(),
                photo.getUser().getId(),
                photo.getUser().getUsername(),
                photo.getImageUrl(),
                photo.getCaption(),
                photo.getCreatedAt()
        ));
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
        comment.setText(request.text());

        Comment savedComment = commentRepository.save(comment);

        return new CommentResponse(
                savedComment.getId(),
                user.getId(),
                user.getUsername(),
                savedComment.getText(),
                savedComment.getCreatedAt()
        );
    }

    // 5. Metoda pentru obținerea comentariilor unei fotografii
    public List<CommentResponse> getCommentsForPhoto(String photoId) {
        Photo photo = photoRepository.findById(UUID.fromString(photoId))
                .orElseThrow(() -> new RuntimeException("Fotografia nu a fost găsită!"));

        List<Comment> comments = commentRepository.findByPhotoOrderByCreatedAtAsc(photo);

        return comments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getText(),
                        comment.getCreatedAt()
                ))
                .toList();
    }
}
