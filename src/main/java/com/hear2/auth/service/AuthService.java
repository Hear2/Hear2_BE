package com.hear2.auth.service;

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
import com.hear2.auth.entity.EmailVerificationToken;
import com.hear2.auth.entity.PasswordResetToken;
import com.hear2.auth.entity.RefreshToken;
import com.hear2.auth.repository.EmailVerificationTokenRepository;
import com.hear2.auth.repository.PasswordResetTokenRepository;
import com.hear2.auth.repository.RefreshTokenRepository;
import com.hear2.auth.oauth.GoogleOAuthClient;
import com.hear2.auth.oauth.GoogleOAuthUserInfo;
import com.hear2.auth.oauth.KakaoOAuthClient;
import com.hear2.auth.oauth.KakaoOAuthUserInfo;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.global.mail.EmailSendException;
import com.hear2.global.mail.EmailService;
import com.hear2.global.security.JwtProvider;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final long PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES = 30;
    private static final long EMAIL_VERIFICATION_TOKEN_EXPIRATION_MINUTES = 30;
    private static final String KAKAO_PROVIDER = "KAKAO";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final GoogleOAuthClient googleOAuthClient;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .provider(request.getProvider())
                .build();

        User savedUser = userRepository.save(user);
        String verificationToken = saveEmailVerificationToken(savedUser);
        publishEmailVerificationEmailRequested(savedUser.getEmail(), verificationToken);

        return AuthResponse.from(savedUser, createToken(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "email verification required");
        }

        return AuthResponse.from(user, createToken(user));
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleOAuthLoginRequest request) {
        GoogleOAuthUserInfo googleUser = googleOAuthClient.verifyIdToken(request.getIdToken());

        if (!googleUser.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "google email is not verified");
        }

        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> userRepository.save(createGoogleUser(googleUser)));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.verifyEmail();
        }

        return AuthResponse.from(user, createToken(user));
    }

    @Transactional
    public AuthResponse loginWithKakao(KakaoOAuthLoginRequest request) {
        KakaoOAuthUserInfo kakaoUser = kakaoOAuthClient.getUserInfo(request.getAccessToken());

        if (!kakaoUser.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "kakao email is not verified");
        }

        User user = findOrCreateKakaoUser(kakaoUser);

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.verifyEmail();
        }

        return AuthResponse.from(user, createToken(user));
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        Long coupleId = coupleMemberRepository.findByUserId(userId)
                .map(coupleMember -> coupleMember.getCoupleId())
                .orElse(null);

        return MeResponse.from(user, coupleId);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    String resetToken = savePasswordResetToken(user);
                    sendPasswordResetEmail(user.getEmail(), resetToken);
                });

        return PasswordResetResponse.success();
    }

    @Transactional(readOnly = true)
    public PasswordResetVerifyResponse verifyPasswordResetToken(PasswordResetVerifyRequest request) {
        if (!StringUtils.hasText(request.getToken())) {
            return PasswordResetVerifyResponse.of(false);
        }

        boolean valid = passwordResetTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .map(token -> LocalDateTime.now().isBefore(token.getExpiresAt()))
                .orElse(false);

        return PasswordResetVerifyResponse.of(valid);
    }

    @Transactional
    public PasswordResetResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(this::invalidPasswordResetToken);

        if (!LocalDateTime.now().isBefore(resetToken.getExpiresAt())) {
            throw invalidPasswordResetToken();
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(this::invalidPasswordResetToken);
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        passwordResetTokenRepository.delete(resetToken);

        return PasswordResetResponse.success();
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(EmailVerificationRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(this::invalidEmailVerificationToken);

        if (!LocalDateTime.now().isBefore(verificationToken.getExpiresAt())) {
            throw invalidEmailVerificationToken();
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(this::invalidEmailVerificationToken);
        user.verifyEmail();
        emailVerificationTokenRepository.delete(verificationToken);

        return EmailVerificationResponse.success();
    }

    @Transactional
    public EmailVerificationResponse resendEmailVerification(EmailVerificationResendRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already verified");
        }

        String verificationToken = saveEmailVerificationToken(user);
        publishEmailVerificationEmailRequested(user.getEmail(), verificationToken);

        return EmailVerificationResponse.success();
    }

    @Transactional(readOnly = true)
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            throw unauthorized();
        }

        Long userId = jwtProvider.getRefreshTokenUserId(refreshToken);
        RefreshToken savedToken = refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(this::unauthorized);

        if (!savedToken.getUserId().equals(userId) || !LocalDateTime.now().isBefore(savedToken.getExpiresAt())) {
            throw unauthorized();
        }

        return TokenResponse.builder()
                .accessToken(jwtProvider.createAccessToken(userId))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    private TokenResponse createToken(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());
        saveRefreshToken(user.getUserId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    private User createGoogleUser(GoogleOAuthUserInfo googleUser) {
        return User.builder()
                .email(googleUser.email())
                .password(passwordEncoder.encode(createOpaqueToken()))
                .nickname(resolveGoogleNickname(googleUser))
                .profileImage(StringUtils.hasText(googleUser.picture()) ? googleUser.picture() : null)
                .provider("GOOGLE")
                .emailVerified(true)
                .build();
    }

    private String resolveGoogleNickname(GoogleOAuthUserInfo googleUser) {
        if (StringUtils.hasText(googleUser.name())) {
            return googleUser.name();
        }

        String email = googleUser.email();
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private User findOrCreateKakaoUser(KakaoOAuthUserInfo kakaoUser) {
        return userRepository.findByProviderAndProviderId(KAKAO_PROVIDER, kakaoUser.providerId())
                .orElseGet(() -> findExistingKakaoUserByEmail(kakaoUser)
                        .orElseGet(() -> userRepository.save(createKakaoUser(kakaoUser))));
    }

    private java.util.Optional<User> findExistingKakaoUserByEmail(KakaoOAuthUserInfo kakaoUser) {
        if (!StringUtils.hasText(kakaoUser.email())) {
            return java.util.Optional.empty();
        }

        return userRepository.findByEmail(kakaoUser.email())
                .map(user -> {
                    if (!StringUtils.hasText(user.getProviderId())) {
                        user.linkProviderId(kakaoUser.providerId());
                    }
                    return user;
                });
    }

    private User createKakaoUser(KakaoOAuthUserInfo kakaoUser) {
        return User.builder()
                .email(resolveKakaoEmail(kakaoUser))
                .password(passwordEncoder.encode(createOpaqueToken()))
                .nickname(resolveKakaoNickname(kakaoUser))
                .profileImage(StringUtils.hasText(kakaoUser.profileImage()) ? kakaoUser.profileImage() : null)
                .provider(KAKAO_PROVIDER)
                .providerId(kakaoUser.providerId())
                .emailVerified(true)
                .build();
    }

    private String resolveKakaoNickname(KakaoOAuthUserInfo kakaoUser) {
        if (StringUtils.hasText(kakaoUser.nickname())) {
            return kakaoUser.nickname();
        }

        if (!StringUtils.hasText(kakaoUser.email())) {
            return kakaoUser.providerId();
        }

        String email = kakaoUser.email();
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String resolveKakaoEmail(KakaoOAuthUserInfo kakaoUser) {
        if (StringUtils.hasText(kakaoUser.email())) {
            return kakaoUser.email();
        }
        return "kakao_" + kakaoUser.providerId() + "@kakao.local";
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpirationSeconds());

        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
                .orElseGet(() -> RefreshToken.builder()
                        .userId(userId)
                        .build());
        savedToken.rotate(tokenHash, expiresAt);

        refreshTokenRepository.save(savedToken);
    }

    private String savePasswordResetToken(User user) {
        String resetToken = createOpaqueToken();
        String tokenHash = hashToken(resetToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES);

        PasswordResetToken savedToken = passwordResetTokenRepository.findByUserId(user.getUserId())
                .orElseGet(() -> PasswordResetToken.builder()
                        .userId(user.getUserId())
                        .build());
        savedToken.rotate(tokenHash, expiresAt);

        passwordResetTokenRepository.save(savedToken);
        return resetToken;
    }

    private String saveEmailVerificationToken(User user) {
        String verificationToken = createOpaqueToken();
        String tokenHash = hashToken(verificationToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_TOKEN_EXPIRATION_MINUTES);

        EmailVerificationToken savedToken = emailVerificationTokenRepository.findByUserId(user.getUserId())
                .orElseGet(() -> EmailVerificationToken.builder()
                        .userId(user.getUserId())
                        .build());
        savedToken.rotate(tokenHash, expiresAt);

        emailVerificationTokenRepository.save(savedToken);
        return verificationToken;
    }

    private void sendPasswordResetEmail(String email, String resetToken) {
        try {
            emailService.sendPasswordResetEmail(email, resetToken);
        } catch (EmailSendException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to send password reset email", exception);
        }
    }

    private void publishEmailVerificationEmailRequested(String email, String verificationToken) {
        eventPublisher.publishEvent(new EmailVerificationEmailRequestedEvent(email, verificationToken));
    }

    private String createOpaqueToken() {
        byte[] randomBytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash token", exception);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
    }

    private ResponseStatusException invalidPasswordResetToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid password reset token");
    }

    private ResponseStatusException invalidEmailVerificationToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email verification token");
    }
}
