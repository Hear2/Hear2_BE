package com.hear2.judge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class JudgeFastApiRequest {

    private Long coupleId;

    private Long triggerMessageId;

    private Long requestedByUserId;

    private List<JudgeFastApiMessage> messages;
}
