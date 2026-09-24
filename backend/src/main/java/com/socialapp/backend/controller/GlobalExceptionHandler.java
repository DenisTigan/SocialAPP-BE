package com.socialapp.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Prinde erorile de logica aruncate intenționat de noi (ex: "Email deja folosit")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // Prinde orice eroare neașteptată (ex: pică baza de date sau mail-ul)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        // Afișează eroarea completă cu roșu în logurile de pe Render
        ex.printStackTrace();

        // Trimite mesajul către frontend (în tab-ul Network)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("A apărut o eroare internă: " + ex.getMessage());
    }
}
