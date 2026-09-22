package com.socialapp.backend.repository;

import com.socialapp.backend.entity.User;
import com.socialapp.backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    Optional<VerificationCode> findByUserAndCode(User user, String code);
    // Adauga aceasta metoda sub cea existenta
    Optional<VerificationCode> findByUser(User user);
}
