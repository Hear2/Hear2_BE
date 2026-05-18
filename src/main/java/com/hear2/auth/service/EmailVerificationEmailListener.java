package com.hear2.auth.service;

import com.hear2.global.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendEmailVerificationEmail(EmailVerificationEmailRequestedEvent event) {
        try {
            emailService.sendEmailVerificationEmail(event.email(), event.verificationToken());
        } catch (Exception exception) {
            log.warn("Failed to send verification email. email={}", event.email(), exception);
        }
    }
}
