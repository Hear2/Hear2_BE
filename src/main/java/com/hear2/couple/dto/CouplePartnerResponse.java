package com.hear2.couple.dto;

import com.hear2.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Function;

@Getter
@Builder
public class CouplePartnerResponse {

    private Long userId;

    private String nickname;

    private String profileImage;

    public static CouplePartnerResponse from(User user) {
        return from(user, null);
    }

    public static CouplePartnerResponse from(User user, Function<String, String> profileImageResolver) {
        return CouplePartnerResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImage(resolveProfileImage(user.getProfileImage(), profileImageResolver))
                .build();
    }

    private static String resolveProfileImage(String profileImage, Function<String, String> profileImageResolver) {
        if (profileImageResolver == null) {
            return profileImage;
        }
        return profileImageResolver.apply(profileImage);
    }
}
