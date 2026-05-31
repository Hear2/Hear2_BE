package com.hear2.character.service;

import com.hear2.character.dto.CharacterExpGrantResult;
import com.hear2.character.dto.CharacterExpHistoryItemResponse;
import com.hear2.character.dto.CharacterExpHistoryTodayResponse;
import com.hear2.character.dto.CharacterNameUpdateRequest;
import com.hear2.character.dto.CharacterResponse;
import com.hear2.character.entity.CharacterExpHistory;
import com.hear2.character.entity.CoupleCharacter;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.repository.CoupleCharacterRepository;
import com.hear2.character.support.CharacterExpSourceType;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final long DAILY_EXP_LIMIT = 150L;
    private static final String DEFAULT_CHARACTER_NAME = "\uCF69\uC774";

    private final CoupleCharacterRepository coupleCharacterRepository;
    private final CharacterExpHistoryRepository characterExpHistoryRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public CharacterResponse getCharacter(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        CoupleCharacter character = getOrCreateCharacter(coupleId);

        return CharacterResponse.from(character);
    }

    @Transactional
    public CharacterResponse updateCharacterName(Long userId, CharacterNameUpdateRequest request) {
        Long coupleId = resolveCoupleId(userId);
        CoupleCharacter character = getOrCreateCharacter(coupleId);
        character.changeName(request.name());

        return CharacterResponse.from(character);
    }

    @Transactional(readOnly = true)
    public CharacterExpHistoryTodayResponse getTodayExpHistory(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        List<CharacterExpHistoryItemResponse> items = characterExpHistoryRepository
                .findByCoupleIdAndEarnedDateOrderByCreatedAtAsc(coupleId, LocalDate.now())
                .stream()
                .map(CharacterExpHistoryItemResponse::from)
                .toList();

        return CharacterExpHistoryTodayResponse.from(items);
    }

    @Transactional
    public CharacterExpGrantResult grantExp(
            Long coupleId,
            CharacterExpSourceType sourceType,
            String sourceId,
            long requestedExp
    ) {
        if (requestedExp <= 0) {
            return new CharacterExpGrantResult(false, requestedExp, 0L, false, false);
        }
        if (characterExpHistoryRepository.existsBySourceTypeAndSourceId(sourceType, sourceId)) {
            return CharacterExpGrantResult.duplicated(requestedExp);
        }

        LocalDate today = LocalDate.now();
        long earnedExpToday = characterExpHistoryRepository.sumExpAmountByCoupleIdAndEarnedDate(coupleId, today);
        long remainingExpToday = Math.max(DAILY_EXP_LIMIT - earnedExpToday, 0L);
        long grantedExp = Math.min(requestedExp, remainingExpToday);

        if (grantedExp <= 0) {
            return CharacterExpGrantResult.limitedOut(requestedExp);
        }

        CoupleCharacter character = getOrCreateCharacter(coupleId);
        character.addExp(grantedExp);

        characterExpHistoryRepository.save(CharacterExpHistory.builder()
                .coupleId(coupleId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .expAmount(grantedExp)
                .earnedDate(today)
                .build());

        return CharacterExpGrantResult.granted(requestedExp, grantedExp);
    }

    private CoupleCharacter getOrCreateCharacter(Long coupleId) {
        return coupleCharacterRepository.findByCoupleId(coupleId)
                .orElseGet(() -> createDefaultCharacter(coupleId));
    }

    private CoupleCharacter createDefaultCharacter(Long coupleId) {
        try {
            return coupleCharacterRepository.save(CoupleCharacter.builder()
                    .coupleId(coupleId)
                    .name(DEFAULT_CHARACTER_NAME)
                    .exp(0L)
                    .build());
        } catch (DataIntegrityViolationException e) {
            return coupleCharacterRepository.findByCoupleId(coupleId)
                    .orElseThrow(() -> e);
        }
    }

    private Long resolveCoupleId(Long userId) {
        CoupleMember member = coupleMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not connected"));
        return member.getCoupleId();
    }
}
