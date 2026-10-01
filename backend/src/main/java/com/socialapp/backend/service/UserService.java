package com.socialapp.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialapp.backend.dto.UpdateBioRequest;
import com.socialapp.backend.dto.UserProfileResponse;
import com.socialapp.backend.dto.UserResponse;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.repository.PhotoLikeRepository;
import com.socialapp.backend.repository.PhotoRepository;
import com.socialapp.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final PhotoLikeRepository photoLikeRepository;
    private final Cloudinary cloudinary;

    public UserService(UserRepository userRepository,
                       PhotoRepository photoRepository,
                       PhotoLikeRepository photoLikeRepository,
                       Cloudinary cloudinary) {
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
        this.photoLikeRepository = photoLikeRepository;
        this.cloudinary = cloudinary;
    }

    public List<UserResponse> getAllUsersExcept(String currentUserId) {
        UUID currentId = UUID.fromString(currentUserId);

        return userRepository.findByIdNot(currentId).stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getAvatarUrl() // <-- NOU
                ))
                .collect(Collectors.toList());
    }

    // --- 1. OBȚINERE PROFIL + STATISTICI ---
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        return buildProfileResponse(user);
    }

    // --- 2. ACTUALIZARE BIO ---
    @Transactional
    public UserProfileResponse updateBio(String userId, UpdateBioRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        String newBio = request.bio() != null ? request.bio().trim() : "";
        if (newBio.length() > 300) {
            throw new RuntimeException("Descrierea (bio) nu poate depăși 300 de caractere!");
        }

        user.setBio(newBio);
        User savedUser = userRepository.save(user);

        return buildProfileResponse(savedUser);
    }

    // --- 3. UPLOAD / SCHIMBARE POZĂ DE PROFIL ---
    @Transactional
    public UserProfileResponse updateAvatar(String userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Te rugăm să selectezi o imagine!");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit!"));

        // Dacă userul avea deja o poză de profil în Cloudinary, o ștergem pe cea veche
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isEmpty()) {
            try {
                cloudinary.uploader().destroy(user.getAvatarPublicId(), ObjectUtils.emptyMap());
            } catch (Exception e) {
                logger.warn("=> WARN: Nu s-a putut șterge vechea poză de profil din Cloudinary: {}", e.getMessage());
            }
        }

        // Uploadăm noua poză în Cloudinary (folosim secure_url pentru HTTPS)
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String secureUrl = uploadResult.get("secure_url").toString();
        String publicId = uploadResult.get("public_id").toString();

        user.setAvatarUrl(secureUrl);
        user.setAvatarPublicId(publicId);
        User savedUser = userRepository.save(user);

        return buildProfileResponse(savedUser);
    }

    // Metodă ajutătoare care calculează statisticile și construiește DTO-ul
    private UserProfileResponse buildProfileResponse(User user) {
        long postsCount = photoRepository.countByUser(user);
        long totalLikesReceived = photoLikeRepository.countByPhoto_User(user);

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                postsCount,
                totalLikesReceived
        );
    }
}

