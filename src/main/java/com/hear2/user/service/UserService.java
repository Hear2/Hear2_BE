package com.hear2.user.service;

import com.hear2.auth.dto.MeResponse;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.user.dto.UserProfileUpdateRequest;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public MeResponse updateMe(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        UserProfileUpdateRequest safeRequest = request == null ? new UserProfileUpdateRequest() : request;
        user.updateProfile(
                safeRequest.hasNickname() ? normalizeNickname(safeRequest.getNickname()) : user.getNickname(),
                safeRequest.hasBirthday() ? safeRequest.getBirthday() : user.getBirthday(),
                safeRequest.hasGender() ? normalizeNullable(safeRequest.getGender()) : user.getGender(),
                safeRequest.hasIntro() ? normalizeNullable(safeRequest.getIntro()) : user.getIntro(),
                safeRequest.hasPhone() ? normalizeNullable(safeRequest.getPhone()) : user.getPhone()
        );

        Long coupleId = coupleMemberRepository.findByUserId(userId)
                .map(coupleMember -> coupleMember.getCoupleId())
                .orElse(null);
        return MeResponse.from(user, coupleId);
    }

    private String normalizeNickname(String nickname) {
        String normalized = nickname == null ? null : nickname.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
