package com.hear2.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportShareResponse {

    private String shareCode;
    private String shareUrl;
    private String title;
    private String summary;
    private boolean notificationRequested;
    private LocalDateTime createdAt;
}
