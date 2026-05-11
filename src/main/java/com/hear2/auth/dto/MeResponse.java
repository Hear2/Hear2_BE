package com.hear2.auth.dto;

import com.hear2.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

    private Long userId;

    private String email;

    private String nickname;

    private String profileImage;

    private String provider;

    public static MeResponse from(User user) {
        return MeResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .build();
    }
}
