package com.socialapp.backend.controller;

import com.socialapp.backend.dto.CommentRequest;
import com.socialapp.backend.dto.CommentResponse;
import com.socialapp.backend.dto.PhotoResponse;
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

    // Endpoint: POST /api/photos[cite: 1]
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) throws IOException {

        // Extragem ID-ul utilizatorului curent din contextul de securitate popluat de filtrul JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        PhotoResponse response = photoService.uploadPhoto(file, caption, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PhotoResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Extragem identitatea userului din token-ul JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        // Apelăm metoda actualizată din service
        Page<PhotoResponse> feed = photoService.getFeed(page, size, currentUserId);
        return ResponseEntity.ok(feed);
    }
    
    @PostMapping("/{photoId}/like")
    public ResponseEntity<String> toggleLike(@PathVariable String photoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        String responseMessage = photoService.toggleLike(photoId, userId);
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/{photoId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String photoId,
            @Valid @RequestBody CommentRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        CommentResponse response = photoService.addComment(photoId, userId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint 5: Vezi toate comentariile unei poze
    @GetMapping("/{photoId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String photoId) {
        List<CommentResponse> comments = photoService.getCommentsForPhoto(photoId);
        return ResponseEntity.ok(comments);
    }
}
