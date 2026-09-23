package com.socialapp.backend.controller;

import com.socialapp.backend.dto.UserResponse;
import com.socialapp.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint: GET /api/users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // Extragem ID-ul utilizatorului care face cererea din token-ul JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        List<UserResponse> users = userService.getAllUsersExcept(currentUserId);
        return ResponseEntity.ok(users);
    }
}
