package com.hear2.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Schema(description = "내 프로필 수정 요청")
public class UserProfileUpdateRequest {

    @JsonIgnore
    @Schema(hidden = true)
    private final Set<String> presentFields = new HashSet<>();

    @Schema(description = "닉네임", example = "예진")
    private String nickname;

    @Schema(description = "생년월일", example = "2000-01-01")
    private LocalDate birthday;

    @Schema(description = "성별", example = "FEMALE")
    private String gender;

    @Schema(description = "한 줄 소개", example = "한 줄 소개")
    private String intro;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @JsonProperty("nickname")
    public void setNickname(String nickname) {
        presentFields.add("nickname");
        this.nickname = nickname;
    }

    @JsonProperty("birthday")
    public void setBirthday(LocalDate birthday) {
        presentFields.add("birthday");
        this.birthday = birthday;
    }

    @JsonProperty("gender")
    public void setGender(String gender) {
        presentFields.add("gender");
        this.gender = gender;
    }

    @JsonProperty("intro")
    public void setIntro(String intro) {
        presentFields.add("intro");
        this.intro = intro;
    }

    @JsonProperty("phone")
    public void setPhone(String phone) {
        presentFields.add("phone");
        this.phone = phone;
    }

    public boolean hasNickname() {
        return presentFields.contains("nickname");
    }

    public boolean hasBirthday() {
        return presentFields.contains("birthday");
    }

    public boolean hasGender() {
        return presentFields.contains("gender");
    }

    public boolean hasIntro() {
        return presentFields.contains("intro");
    }

    public boolean hasPhone() {
        return presentFields.contains("phone");
    }
}
