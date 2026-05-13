package com.hear2.report.service;

import com.hear2.notification.service.FcmNotificationService;
import com.hear2.report.dto.ReportResponse;
import com.hear2.report.dto.ReportShareCreateRequest;
import com.hear2.report.dto.ReportShareResponse;
import com.hear2.report.entity.ReportShare;
import com.hear2.report.repository.ReportShareRepository;
import com.hear2.report.support.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportShareService {

    private final ReportService reportService;
    private final ReportShareRepository reportShareRepository;
    private final FcmNotificationService fcmNotificationService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public ReportShareResponse createShare(ReportShareCreateRequest request) {
        validateCreateRequest(request);

        ReportType reportType = request.getReportType() == null ? ReportType.WEEKLY : request.getReportType();
        LocalDate anchorDate = request.getAnchorDate() == null ? LocalDate.now() : request.getAnchorDate();
        ReportResponse report = reportService.getReport(request.getCoupleId(), reportType, anchorDate);
        String shareCode = generateShareCode();
        String shareUrl = buildShareUrl(shareCode);

        ReportShare reportShare = ReportShare.builder()
                .shareCode(shareCode)
                .coupleId(request.getCoupleId())
                .requesterId(request.getRequesterId())
                .receiverId(request.getReceiverId())
                .partnerName(trimToNull(request.getPartnerName()))
                .reportType(report.getReportType())
                .anchorDate(report.getAnchorDate())
                .periodStartDate(report.getPeriodStartDate())
                .periodEndDate(report.getPeriodEndDate())
                .snapshotJson(writeSnapshot(report))
                .build();

        ReportShare savedShare = reportShareRepository.save(reportShare);
        boolean notificationRequested = request.getReceiverId() != null;

        if (notificationRequested) {
            fcmNotificationService.sendReportShare(
                    request.getReceiverId(),
                    request.getCoupleId(),
                    "리포트가 도착했어요",
                    buildNotificationBody(report, request.getPartnerName()),
                    shareCode,
                    shareUrl
            );
        }

        return ReportShareResponse.builder()
                .shareCode(savedShare.getShareCode())
                .shareUrl(shareUrl)
                .title(report.getTitle())
                .summary(report.getSummary())
                .notificationRequested(notificationRequested)
                .createdAt(savedShare.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportResponse getSharedReport(String shareCode) {
        ReportShare reportShare = findShare(shareCode);
        return enrichSharedReport(readSnapshot(reportShare.getSnapshotJson()), reportShare);
    }

    private ReportShare findShare(String shareCode) {
        if (!StringUtils.hasText(shareCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shareCode is required");
        }

        return reportShareRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "shared report not found"));
    }

    private String writeSnapshot(ReportResponse report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize report snapshot", ex);
        }
    }

    private ReportResponse readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, ReportResponse.class);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to deserialize report snapshot", ex);
        }
    }

    private ReportResponse enrichSharedReport(ReportResponse report, ReportShare reportShare) {
        return report.toBuilder()
                .shareCode(reportShare.getShareCode())
                .shareUrl(buildShareUrl(reportShare.getShareCode()))
                .build();
    }

    private String buildNotificationBody(ReportResponse report, String partnerName) {
        if (StringUtils.hasText(partnerName)) {
            return partnerName.trim() + "님과 함께 볼 " + report.getTitle() + "가 준비됐어요.";
        }
        return report.getTitle() + "를 확인해보세요.";
    }

    private String buildShareUrl(String shareCode) {
        return appBaseUrl.replaceAll("/$", "") + "/reports/shared/" + shareCode;
    }

    private String generateShareCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateCreateRequest(ReportShareCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "report share request is required");
        }
        if (request.getCoupleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
    }
}
