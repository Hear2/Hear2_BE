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

    private Long partnerUserId;

    private String requestedByName;

    private String partnerName;

    private List<JudgeFastApiMessage> messages;
}
