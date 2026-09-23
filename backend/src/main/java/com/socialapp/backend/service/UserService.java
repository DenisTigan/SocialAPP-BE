package com.socialapp.backend.service;

import com.socialapp.backend.dto.UserResponse;
import com.socialapp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsersExcept(String currentUserId) {
        UUID currentId = UUID.fromString(currentUserId);

        // Preluăm toți userii cu excepția celui logat
        return userRepository.findByIdNot(currentId).stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername()
                ))
                .collect(Collectors.toList());
    }
}

