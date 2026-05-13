package com.hear2.report.support;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReportPeriod {

    private ReportType reportType;
    private LocalDate anchorDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodLabel;
    private String title;
}
