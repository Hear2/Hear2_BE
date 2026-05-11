package com.hear2.couple.service;

import com.hear2.couple.dto.CoupleConnectRequest;
import com.hear2.couple.dto.CoupleStatusResponse;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private static final String OWNER_ROLE = "OWNER";
    private static final String PARTNER_ROLE = "PARTNER";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_GENERATION_MAX_ATTEMPTS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CoupleRepository coupleRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public CoupleStatusResponse createCode(Long userId) {
        validateUser(userId);

        if (coupleMemberRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already has a couple");
        }

        Couple couple = coupleRepository.save(Couple.builder()
                .coupleCode(generateUniqueCode())
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(userId)
                .role(OWNER_ROLE)
                .build());

        return CoupleStatusResponse.from(couple, 1);
    }

    @Transactional
    public CoupleStatusResponse connect(Long userId, CoupleConnectRequest request) {
        validateUser(userId);

        if (coupleMemberRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already has a couple");
        }

        Couple couple = coupleRepository.findByCoupleCode(normalizeCode(request.getCoupleCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple code not found"));

        long memberCount = coupleMemberRepository.countByCoupleId(couple.getCoupleId());
        if (memberCount >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "couple already connected");
        }

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(userId)
                .role(PARTNER_ROLE)
                .build());

        return CoupleStatusResponse.from(couple, memberCount + 1);
    }

    @Transactional(readOnly = true)
    public CoupleStatusResponse getStatus(Long userId) {
        validateUser(userId);

        return coupleMemberRepository.findByUserId(userId)
                .map(coupleMember -> {
                    Couple couple = coupleRepository.findById(coupleMember.getCoupleId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
                    long memberCount = coupleMemberRepository.countByCoupleId(couple.getCoupleId());
                    return CoupleStatusResponse.from(couple, memberCount);
                })
                .orElseGet(CoupleStatusResponse::disconnected);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!coupleRepository.existsByCoupleCode(code)) {
                return code;
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to generate couple code");
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private String normalizeCode(String coupleCode) {
        return coupleCode.trim().toUpperCase(Locale.ROOT);
    }
}
