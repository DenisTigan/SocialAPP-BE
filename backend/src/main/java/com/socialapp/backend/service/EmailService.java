package com.socialapp.backend.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${MAIL_FROM}")
    private String senderEmail;

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        String url = "https://api.brevo.com/v3/smtp/email";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        // Construim structura de date cerută de documentația Brevo API
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("email", senderEmail, "name", "SocialApp Team"));
        body.put("to", List.of(Map.of("email", toEmail)));
        body.put("subject", "Codul tau de verificare");
        body.put("htmlContent", "<html><body><h3>Salut!</h3><p>Codul tău de verificare pentru activarea contului este: <strong>" + verificationCode + "</strong></p></body></html>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Facem apelul POST (trece prin portul standard HTTPS, deci Render nu îl blochează)
        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

}
