package com.hear2.memory.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemoryUpdateRequest {

    @Size(max = 1000)
    private String memo;
}
