package com.hear2.memory.service;

public record KakaoLocationNames(
        String placeName,
        String addressName
) {
    public String displayName() {
        return placeName != null ? placeName : addressName;
    }
}
