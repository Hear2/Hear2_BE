package com.hear2.auth.controller;

import com.hear2.auth.dto.AuthResponse;
import com.hear2.auth.dto.EmailVerificationRequest;
import com.hear2.auth.dto.EmailVerificationResendRequest;
import com.hear2.auth.dto.EmailVerificationResponse;
import com.hear2.auth.dto.GoogleOAuthLoginRequest;
import com.hear2.auth.dto.KakaoOAuthLoginRequest;
import com.hear2.auth.dto.LoginRequest;
import com.hear2.auth.dto.MeResponse;
import com.hear2.auth.dto.PasswordResetConfirmRequest;
import com.hear2.auth.dto.PasswordResetRequest;
import com.hear2.auth.dto.PasswordResetResponse;
import com.hear2.auth.dto.PasswordResetVerifyRequest;
import com.hear2.auth.dto.PasswordResetVerifyResponse;
import com.hear2.auth.dto.ReissueRequest;
import com.hear2.auth.dto.SignupRequest;
import com.hear2.auth.dto.TokenResponse;
import com.hear2.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleOAuthLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/oauth/kakao")
    public AuthResponse loginWithKakao(@Valid @RequestBody KakaoOAuthLoginRequest request) {
        return authService.loginWithKakao(request);
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@RequestBody ReissueRequest request) {
        return authService.reissue(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public PasswordResetResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/verify")
    public PasswordResetVerifyResponse verifyPasswordResetToken(@RequestBody PasswordResetVerifyRequest request) {
        return authService.verifyPasswordResetToken(request);
    }

    @PostMapping("/password-reset/confirm")
    public PasswordResetResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }

    @PostMapping("/email/verify")
    public EmailVerificationResponse verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/email/resend")
    public EmailVerificationResponse resendEmailVerification(@Valid @RequestBody EmailVerificationResendRequest request) {
        return authService.resendEmailVerification(request);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.getMe(userId);
    }
}
