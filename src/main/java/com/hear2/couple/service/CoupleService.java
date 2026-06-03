package com.hear2.couple.service;

import com.hear2.couple.dto.CoupleConnectRequest;
import com.hear2.couple.dto.CoupleNicknameRequest;
import com.hear2.couple.dto.CoupleNicknamesResponse;
import com.hear2.couple.dto.CouplePartnerResponse;
import com.hear2.couple.dto.CoupleStartDateRequest;
import com.hear2.couple.dto.CoupleStatusResponse;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleCode;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.entity.CoupleNickname;
import com.hear2.couple.repository.CoupleCodeRepository;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleNicknameRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final CoupleCodeRepository coupleCodeRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final CoupleNicknameRepository coupleNicknameRepository;
    private final UserRepository userRepository;

    @Transactional
    public CoupleStatusResponse createCode(Long userId) {
        validateUser(userId);

        if (coupleMemberRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already has a couple");
        }

        CoupleCode coupleCode = coupleCodeRepository.save(CoupleCode.builder()
                .code(generateUniqueCode())
                .issuerUserId(userId)
                .build());

        return CoupleStatusResponse.pending(coupleCode.getCode());
    }

    @Transactional
    public CoupleStatusResponse connect(Long userId, CoupleConnectRequest request) {
        validateUser(userId);

        if (coupleMemberRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already has a couple");
        }

        CoupleCode coupleCode = coupleCodeRepository.findByCode(normalizeCode(request.getCoupleCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple code not found"));

        if (coupleCode.isUsed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "couple code already used");
        }
        if (coupleCode.getIssuerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot connect with own couple code");
        }
        if (coupleMemberRepository.existsByUserId(coupleCode.getIssuerUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "couple code already used");
        }

        Couple couple = coupleRepository.save(Couple.builder()
                .coupleCode(coupleCode.getCode())
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(coupleCode.getIssuerUserId())
                .role(OWNER_ROLE)
                .build());
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(userId)
                .role(PARTNER_ROLE)
                .build());
        markActiveCodesUsed(coupleCode.getIssuerUserId(), couple.getCoupleId());
        markActiveCodesUsed(userId, couple.getCoupleId());

        return buildStatusResponse(couple, userId);
    }

    @Transactional(readOnly = true)
    public CoupleStatusResponse getStatus(Long userId) {
        validateUser(userId);

        return coupleMemberRepository.findByUserId(userId)
                .map(coupleMember -> {
                    Couple couple = coupleRepository.findById(coupleMember.getCoupleId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
                    return buildStatusResponse(couple, userId);
                })
                .orElseGet(() -> coupleCodeRepository.findTopByIssuerUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId)
                        .map(coupleCode -> CoupleStatusResponse.pending(coupleCode.getCode()))
                        .orElseGet(CoupleStatusResponse::disconnected));
    }

    @Transactional
    public CoupleStatusResponse updateStartDate(Long userId, CoupleStartDateRequest request) {
        validateUser(userId);
        if (request == null || request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate is required");
        }
        if (request.getStartDate().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be in the future");
        }

        CoupleMember member = findCoupleMember(userId);
        Couple couple = coupleRepository.findById(member.getCoupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
        couple.updateStartDate(request.getStartDate());
        return buildStatusResponse(couple, userId);
    }

    @Transactional(readOnly = true)
    public CoupleNicknamesResponse getNicknames(Long userId) {
        validateUser(userId);
        CoupleMember member = findCoupleMember(userId);

        return CoupleNicknamesResponse.from(
                coupleNicknameRepository.findByCoupleIdOrderByGiverUserIdAsc(member.getCoupleId())
        );
    }

    @Transactional
    public CoupleNicknamesResponse updateNickname(Long userId, CoupleNicknameRequest request) {
        validateUser(userId);
        CoupleMember member = findCoupleMember(userId);
        String nickname = normalizeNickname(request.getNickname());

        CoupleNickname coupleNickname = coupleNicknameRepository
                .findByCoupleIdAndGiverUserId(member.getCoupleId(), userId)
                .orElseGet(() -> CoupleNickname.builder()
                        .coupleId(member.getCoupleId())
                        .giverUserId(userId)
                        .build());
        coupleNickname.updateNickname(nickname);
        coupleNicknameRepository.save(coupleNickname);

        return CoupleNicknamesResponse.from(
                coupleNicknameRepository.findByCoupleIdOrderByGiverUserIdAsc(member.getCoupleId())
        );
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }
    }

    private CoupleMember findCoupleMember(Long userId) {
        return coupleMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));
    }

    private CoupleStatusResponse buildStatusResponse(Couple couple, Long userId) {
        long memberCount = coupleMemberRepository.countByCoupleId(couple.getCoupleId());
        CouplePartnerResponse partner = memberCount >= 2 ? findPartner(couple.getCoupleId(), userId) : null;
        return CoupleStatusResponse.from(couple, memberCount, partner);
    }

    private CouplePartnerResponse findPartner(Long coupleId, Long userId) {
        return coupleMemberRepository.findFirstByCoupleIdAndUserIdNot(coupleId, userId)
                .flatMap(coupleMember -> userRepository.findById(coupleMember.getUserId()))
                .map(CouplePartnerResponse::from)
                .orElse(null);
    }

    private String normalizeNickname(String nickname) {
        String normalized = nickname == null ? null : nickname.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname is required");
        }
        return normalized;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!coupleCodeRepository.existsByCode(code) && !coupleRepository.existsByCoupleCode(code)) {
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

    private void markActiveCodesUsed(Long userId, Long coupleId) {
        coupleCodeRepository.findByIssuerUserIdAndUsedAtIsNull(userId)
                .forEach(coupleCode -> coupleCode.markUsed(coupleId));
    }
}
