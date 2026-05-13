package com.hear2.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String PASSWORD_RESET_SUBJECT = "Hear2 비밀번호 재설정 인증 토큰";
    private static final String PASSWORD_RESET_BODY_FORMAT = "Hear2 비밀번호 재설정 인증 토큰입니다: %s";

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    public void sendPasswordResetEmail(String to, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(mailUsername);
        message.setSubject(PASSWORD_RESET_SUBJECT);
        message.setText(PASSWORD_RESET_BODY_FORMAT.formatted(resetToken));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new EmailSendException("failed to send password reset email", exception);
        }
    }

    public void sendEmailVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(mailUsername);
        message.setSubject("Hear2 이메일 인증 토큰");
        message.setText("Hear2 이메일 인증 토큰입니다: %s".formatted(token));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            exception.printStackTrace();
            throw new EmailSendException("failed to send email verification email", exception);
        }
    }
}
