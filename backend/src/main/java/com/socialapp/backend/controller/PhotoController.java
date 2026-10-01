package com.socialapp.backend.controller;

import com.socialapp.backend.dto.CommentRequest;
import com.socialapp.backend.dto.CommentResponse;
import com.socialapp.backend.dto.PhotoResponse;
import com.socialapp.backend.dto.UpdatePhotoRequest;
import com.socialapp.backend.service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {


    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    // 1. Upload poză: POST /api/photos
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        PhotoResponse response = photoService.uploadPhoto(file, caption, userId);
        return ResponseEntity.ok(response);
    }

    // 2. Feed: GET /api/photos/feed
    @GetMapping("/feed")
    public ResponseEntity<Page<PhotoResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        Page<PhotoResponse> feed = photoService.getFeed(page, size, currentUserId);
        return ResponseEntity.ok(feed);
    }

    // 3. NOU: Editare descriere poză: PUT /api/photos/{photoId}
    @PutMapping("/{photoId}")
    public ResponseEntity<PhotoResponse> updatePhotoCaption(
            @PathVariable String photoId,
            @RequestBody UpdatePhotoRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        return ResponseEntity.ok(photoService.updatePhotoCaption(photoId, userId, request));
    }

    // 4. NOU: Ștergere poză: DELETE /api/photos/{photoId}
    @DeleteMapping("/{photoId}")
    public ResponseEntity<String> deletePhoto(@PathVariable String photoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        photoService.deletePhoto(photoId, userId);
        return ResponseEntity.ok("Postarea a fost ștearsă cu succes!");
    }

    // 5. Like / Unlike: POST /api/photos/{photoId}/like
    @PostMapping("/{photoId}/like")
    public ResponseEntity<String> toggleLike(@PathVariable String photoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        String responseMessage = photoService.toggleLike(photoId, userId);
        return ResponseEntity.ok(responseMessage);
    }

    // 6. Adaugă comentariu: POST /api/photos/{photoId}/comments
    @PostMapping("/{photoId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String photoId,
            @Valid @RequestBody CommentRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        CommentResponse response = photoService.addComment(photoId, userId, request);
        return ResponseEntity.ok(response);
    }

    // 7. Vezi comentariile unei poze: GET /api/photos/{photoId}/comments
    @GetMapping("/{photoId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String photoId) {
        List<CommentResponse> comments = photoService.getCommentsForPhoto(photoId);
        return ResponseEntity.ok(comments);
    }

    // 8. NOU: Editare comentariu: PUT /api/photos/comments/{commentId}
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        return ResponseEntity.ok(photoService.updateComment(commentId, userId, request));
    }

    // 9. NOU: Ștergere comentariu: DELETE /api/photos/comments/{commentId}
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        photoService.deleteComment(commentId, userId);
        return ResponseEntity.ok("Comentariul a fost șters cu succes!");
    }
}
