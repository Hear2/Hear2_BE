package com.hear2.couple.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleConnectRequest {

    @NotBlank
    private String coupleCode;
}
