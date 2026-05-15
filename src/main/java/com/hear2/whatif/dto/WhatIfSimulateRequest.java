package com.hear2.whatif.dto;

import com.hear2.whatif.support.WhatIfCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WhatIfSimulateRequest {

    @NotBlank(message = "question must not be blank")
    @Size(max = 300, message = "question must be 300 characters or less")
    private String question;

    private WhatIfCategory category;
}
