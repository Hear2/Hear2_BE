package com.hear2.character.service;

import com.hear2.character.dto.CharacterResponse;
import com.hear2.character.entity.CoupleCharacter;
import com.hear2.character.repository.CoupleCharacterRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final String DEFAULT_CHARACTER_NAME = "콩이";

    private final CoupleCharacterRepository coupleCharacterRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public CharacterResponse getCharacter(Long userId) {
        Long coupleId = resolveCoupleId(userId);
        CoupleCharacter character = coupleCharacterRepository.findByCoupleId(coupleId)
                .orElseGet(() -> createDefaultCharacter(coupleId));

        return CharacterResponse.from(character);
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
