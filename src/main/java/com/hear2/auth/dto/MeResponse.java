package com.hear2.auth.dto;

import com.hear2.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.function.Function;

@Getter
@Builder
public class MeResponse {

    private Long userId;

    private String email;

    private String nickname;

    private String profileImage;

    private String provider;

    private LocalDate birthday;

    private String gender;

    private String intro;

    private String phone;

    private Long coupleId;

    public static MeResponse from(User user) {
        return from(user, null);
    }

    public static MeResponse from(User user, Long coupleId) {
        return from(user, coupleId, null);
    }

    public static MeResponse from(User user, Long coupleId, Function<String, String> profileImageResolver) {
        return MeResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(resolveProfileImage(user.getProfileImage(), profileImageResolver))
                .provider(user.getProvider())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .intro(user.getIntro())
                .phone(user.getPhone())
                .coupleId(coupleId)
                .build();
    }

    private static String resolveProfileImage(String profileImage, Function<String, String> profileImageResolver) {
        if (profileImageResolver == null) {
            return profileImage;
        }
        return profileImageResolver.apply(profileImage);
    }
}
