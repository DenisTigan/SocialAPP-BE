package com.socialapp.backend.service;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Cod de verificare cont");
        message.setText("Salut,\n\nCodul tău de verificare este: " + code +
                "\n\nAcest cod este valabil 10 minute.");

        mailSender.send(message);
    }

}
