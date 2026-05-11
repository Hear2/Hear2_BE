package com.hear2.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationShareStatusRequest {

    @NotNull
    private Long coupleId;

    @NotNull
    private Long userId;

    @NotNull
    private Boolean enabled;
}
