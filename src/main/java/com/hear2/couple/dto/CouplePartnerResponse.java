package com.hear2.couple.dto;

import com.hear2.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouplePartnerResponse {

    private Long userId;

    private String nickname;

    private String profileImage;

    public static CouplePartnerResponse from(User user) {
        return CouplePartnerResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();
    }
}
