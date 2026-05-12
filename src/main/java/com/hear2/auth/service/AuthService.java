package com.hear2.auth.service;

import com.hear2.auth.dto.AuthResponse;
import com.hear2.auth.dto.LoginRequest;
import com.hear2.auth.dto.MeResponse;
import com.hear2.auth.dto.SignupRequest;
import com.hear2.auth.dto.TokenResponse;
import com.hear2.auth.entity.RefreshToken;
import com.hear2.auth.repository.RefreshTokenRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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
        return AuthResponse.from(savedUser, createToken(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }

        return AuthResponse.from(user, createToken(user));
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        return MeResponse.from(user);
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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash refresh token", exception);
        }
    }
}
