package com.hear2.auth.service;

public record EmailVerificationEmailRequestedEvent(String email, String verificationToken) {
}
