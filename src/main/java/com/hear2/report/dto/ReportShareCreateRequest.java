package com.hear2.report.dto;

import com.hear2.report.support.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportShareCreateRequest {

    private Long coupleId;
    private Long requesterId;
    private Long receiverId;
    private String partnerName;
    private ReportType reportType;
    private LocalDate anchorDate;
}
