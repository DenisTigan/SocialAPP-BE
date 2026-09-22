package com.socialapp.backend.controller;

import com.socialapp.backend.dto.LoginRequest;
import com.socialapp.backend.dto.RegisterRequest;
import com.socialapp.backend.dto.ResendCodeRequest;
import com.socialapp.backend.dto.VerifyRequest;
import com.socialapp.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Endpoint: POST /api/auth/register[cite: 1]
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String responseMessage = authService.registerUser(request);
        return ResponseEntity.ok(responseMessage);
    }

    // Endpoint: POST /api/auth/verify[cite: 1]
    @PostMapping("/verify")
    public ResponseEntity<String> verify(@Valid @RequestBody VerifyRequest request) {
        String responseMessage = authService.verifyCode(request);
        return ResponseEntity.ok(responseMessage);
    }
    // Endpoint: POST /api/auth/resend-code[cite: 1]
    @PostMapping("/resend-code")
    public ResponseEntity<String> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        String responseMessage = authService.resendVerificationCode(request);
        return ResponseEntity.ok(responseMessage);
    }

    // Endpoint: POST /api/auth/login[cite: 1]
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(token);
    }

}
