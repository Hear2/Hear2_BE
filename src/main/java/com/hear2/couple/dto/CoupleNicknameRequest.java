package com.hear2.couple.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleNicknameRequest {

    @NotBlank
    @Size(max = 50)
    private String nickname;
}
