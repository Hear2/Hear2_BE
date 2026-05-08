package com.hear2.notification.service;

import com.hear2.notification.dto.FcmTokenRequest;
import com.hear2.notification.dto.FcmTokenResponse;
import com.hear2.notification.entity.FcmToken;
import com.hear2.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public FcmTokenResponse register(FcmTokenRequest request) {
        validateRegisterRequest(request);

        FcmToken fcmToken = fcmTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    existing.registerAgain(request.getUserId(), request.getDeviceId(), request.getPlatform());
                    return existing;
                })
                .orElseGet(() -> FcmToken.builder()
                        .userId(request.getUserId())
                        .token(request.getToken())
                        .deviceId(request.getDeviceId())
                        .platform(request.getPlatform())
                        .active(true)
                        .build());

        return FcmTokenResponse.from(fcmTokenRepository.save(fcmToken));
    }

    @Transactional
    public void unregister(FcmTokenRequest request) {
        if (request == null || !StringUtils.hasText(request.getToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }

        fcmTokenRepository.findByToken(request.getToken())
                .ifPresent(FcmToken::deactivate);
    }

    private void validateRegisterRequest(FcmTokenRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FCM token request is required");
        }
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (!StringUtils.hasText(request.getToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }
    }
}
