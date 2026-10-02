package com.socialapp.backend.controller;

import com.socialapp.backend.dto.PhotoResponse;
import com.socialapp.backend.dto.UpdateBioRequest;
import com.socialapp.backend.dto.UserProfileResponse;
import com.socialapp.backend.dto.UserResponse;
import com.socialapp.backend.service.PhotoService;
import com.socialapp.backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PhotoService photoService;

    public UserController(UserService userService, PhotoService photoService) {
        this.userService = userService;
        this.photoService = photoService;
    }

    // 1. GET /api/users (Toți userii, mai puțin cel logat)
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        List<UserResponse> users = userService.getAllUsersExcept(currentUserId);
        return ResponseEntity.ok(users);
    }

    // 2. NOU: GET /api/users/online (Set cu UUID-urile tuturor userilor online acum)
    @GetMapping("/online")
    public ResponseEntity<Set<UUID>> getOnlineUsers() {
        return ResponseEntity.ok(userService.getOnlineUserIds());
    }

    // 3. GET /api/users/me (Profilul utilizatorului logat)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        return ResponseEntity.ok(userService.getUserProfile(currentUserId));
    }

    // 4. GET /api/users/{userId}/profile (Profilul oricărui utilizator după ID)
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    // 5. PUT /api/users/me/bio (Actualizare descriere/bio)
    @PutMapping("/me/bio")
    public ResponseEntity<UserProfileResponse> updateBio(@RequestBody UpdateBioRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        return ResponseEntity.ok(userService.updateBio(currentUserId, request));
    }

    // 6. POST /api/users/me/avatar (Upload poză de profil)
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        return ResponseEntity.ok(userService.updateAvatar(currentUserId, file));
    }

    // 7. GET /api/users/{userId}/photos (Toate pozele postate de un user, pentru pagina de profil)
    @GetMapping("/{userId}/photos")
    public ResponseEntity<List<PhotoResponse>> getUserPhotos(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        return ResponseEntity.ok(photoService.getUserPhotos(userId, currentUserId));
    }
}
