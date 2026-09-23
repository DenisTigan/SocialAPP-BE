package com.socialapp.backend.service;


import com.socialapp.backend.dto.*;
import com.socialapp.backend.entity.User;
import com.socialapp.backend.entity.VerificationCode;
import com.socialapp.backend.repository.UserRepository;
import com.socialapp.backend.repository.VerificationCodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       VerificationCodeRepository verificationCodeRepository,
                       EmailService emailService,
                       JwtService jwtService) { // <-- Adaugat aici
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeRepository = verificationCodeRepository;
        this.emailService = emailService;
        this.jwtService = jwtService; // <-- Atribuit aici
    }

    @Transactional
    public String registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email-ul este deja folosit!");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username-ul este deja folosit!");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(false);

        // 1. Salvam utilizatorul
        userRepository.save(user);

        // 2. Generam un cod numeric de 6 cifre aleator[cite: 1]
        String code = String.format("%06d", new Random().nextInt(999999));

        // 3. Cream entitatea pentru codul de verificare cu expirare in 10 minute[cite: 1]
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        verificationCodeRepository.save(verificationCode);

        // 4. Trimitem emailul (folosind Gmail SMTP)[cite: 1]
        emailService.sendVerificationEmail(user.getEmail(), code);

        return "Utilizator inregistrat cu succes! Verifica adresa de email pentru codul de activare.";
    }

    @Transactional
    public String verifyCode(VerifyRequest request) {
        // 1. Gasim utilizatorul dupa email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit!"));

        // Daca este deja activat, nu mai are sens sa continuam
        if (user.isEnabled()) {
            return "Contul este deja activat!";
        }

        // 2. Cautam codul in baza de date
        VerificationCode verificationCode = verificationCodeRepository.findByUserAndCode(user, request.code())
                .orElseThrow(() -> new RuntimeException("Cod invalid sau inexistent pentru acest utilizator!"));

        // 3. Verificam daca a expirat (comparand cu timpul curent)[cite: 1]
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Codul de verificare a expirat!");
        }

        // 4. Totul este corect! Activam userul[cite: 1]
        user.setEnabled(true);
        userRepository.save(user);

        // 5. Stergem codul din baza de date pentru a nu mai putea fi folosit a doua oara[cite: 1]
        verificationCodeRepository.delete(verificationCode);

        return "Contul a fost activat cu succes! Acum te poti autentifica.";
    }

    @Transactional
    public String resendVerificationCode(ResendCodeRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit!"));

        if (user.isEnabled()) {
            return "Contul este deja activat!";
        }

        // Căutăm dacă există deja un cod (expirat sau nu) și îl ștergem
        verificationCodeRepository.findByUser(user)
                .ifPresent(verificationCodeRepository::delete);

        // Generăm un cod nou[cite: 1]
        String newCode = String.format("%06d", new Random().nextInt(999999));

        // Creăm și salvăm noua entitate cu o nouă valabilitate de 10 minute[cite: 1]
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCode(newCode);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        verificationCodeRepository.save(verificationCode);

        // Trimitem noul cod pe email[cite: 1]
        emailService.sendVerificationEmail(user.getEmail(), newCode);

        return "Un nou cod de verificare a fost trimis pe email!";
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Gasim utilizatorul
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Email sau parola incorecta!"));

        // 2. Verificam parola folosind BCrypt
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Email sau parola incorecta!");
        }

        // 3. Verificam daca a activat contul
        if (!user.isEnabled()) {
            throw new RuntimeException("Contul nu este verificat! Te rugam sa introduci codul primit pe email.");
        }

        // 4. Generam token-ul si returnam obiectul complet
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername()
        );
    }
}
