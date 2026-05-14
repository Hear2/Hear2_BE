package com.hear2.capsule.service;

import com.hear2.capsule.dto.TimeCapsuleCreateRequest;
import com.hear2.capsule.dto.TimeCapsuleDetailResponse;
import com.hear2.capsule.dto.TimeCapsuleLetterResponse;
import com.hear2.capsule.dto.TimeCapsuleListResponse;
import com.hear2.capsule.dto.TimeCapsulePhotoResponse;
import com.hear2.capsule.dto.TimeCapsuleShareCardResponse;
import com.hear2.capsule.dto.TimeCapsuleSummaryResponse;
import com.hear2.capsule.entity.TimeCapsule;
import com.hear2.capsule.entity.TimeCapsuleLetter;
import com.hear2.capsule.entity.TimeCapsuleMedia;
import com.hear2.capsule.entity.TimeCapsuleMediaType;
import com.hear2.capsule.entity.TimeCapsuleSnapshot;
import com.hear2.capsule.entity.TimeCapsuleStatus;
import com.hear2.capsule.repository.TimeCapsuleLetterRepository;
import com.hear2.capsule.repository.TimeCapsuleMediaRepository;
import com.hear2.capsule.repository.TimeCapsuleRepository;
import com.hear2.capsule.repository.TimeCapsuleSnapshotRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TimeCapsuleService {

    private static final int MAX_PHOTO_COUNT = 20;
    private static final DateTimeFormatter SHARE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final TimeCapsuleRepository timeCapsuleRepository;
    private final TimeCapsuleLetterRepository timeCapsuleLetterRepository;
    private final TimeCapsuleMediaRepository timeCapsuleMediaRepository;
    private final TimeCapsuleSnapshotRepository timeCapsuleSnapshotRepository;
    private final TimeCapsuleSnapshotService timeCapsuleSnapshotService;
    private final TimeCapsuleMediaAccessService timeCapsuleMediaAccessService;
    private final CoupleMemberRepository coupleMemberRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TimeCapsuleDetailResponse createCapsule(TimeCapsuleCreateRequest request, Long currentUserId) {
        validateCreateRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        LocalDateTime now = utcNow();
        LocalDateTime openAt = toUtcLocalDateTime(request.getOpenAt());

        TimeCapsule capsule = TimeCapsule.builder()
                .coupleId(context.coupleId())
                .name(request.getName().trim())
                .coverStyle(request.getCoverStyle())
                .openAt(openAt)
                .sealedAt(now)
                .status(TimeCapsuleStatus.SEALED)
                .optionsJson(writeOptions(request))
                .build();
        TimeCapsule savedCapsule = timeCapsuleRepository.saveAndFlush(capsule);

        timeCapsuleLetterRepository.save(TimeCapsuleLetter.builder()
                .capsuleId(savedCapsule.getId())
                .authorId(context.userId())
                .text(request.getLetter().trim())
                .writtenAt(now)
                .build());
        savePhotos(savedCapsule.getId(), request.getPhotoObjectKeys());
        timeCapsuleSnapshotRepository.save(
                timeCapsuleSnapshotService.createSnapshot(savedCapsule.getId(), context.coupleId(), now)
        );

        return toDetail(savedCapsule, false);
    }

    @Transactional
    public TimeCapsuleListResponse getCapsules(String status, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        String normalizedStatus = normalizeStatus(status);
        LocalDateTime now = utcNow();

        List<TimeCapsule> capsules = switch (normalizedStatus) {
            case "sealed" -> timeCapsuleRepository.findByCoupleIdAndStatusOrderByOpenAtDesc(
                    context.coupleId(),
                    TimeCapsuleStatus.SEALED
            );
            case "open" -> timeCapsuleRepository.findByCoupleIdAndStatusOrderByOpenAtDesc(
                    context.coupleId(),
                    TimeCapsuleStatus.OPEN
            );
            case "all" -> timeCapsuleRepository.findByCoupleIdOrderByOpenAtDesc(context.coupleId());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be sealed, open, or all");
        };
        capsules.forEach(capsule -> openIfDue(capsule, now));

        return TimeCapsuleListResponse.builder()
                .sealed(capsules.stream()
                        .filter(capsule -> capsule.getStatus() == TimeCapsuleStatus.SEALED)
                        .map(this::toSummary)
                        .toList())
                .open(capsules.stream()
                        .filter(capsule -> capsule.getStatus() == TimeCapsuleStatus.OPEN)
                        .map(this::toSummary)
                        .toList())
                .build();
    }

    @Transactional
    public TimeCapsuleDetailResponse getCapsule(Long capsuleId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        TimeCapsule capsule = findCapsule(capsuleId, context.coupleId());
        openIfDue(capsule, utcNow());

        return toDetail(capsule, capsule.getStatus() == TimeCapsuleStatus.OPEN);
    }

    @Transactional
    public TimeCapsuleShareCardResponse getShareCard(Long capsuleId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        TimeCapsule capsule = findCapsule(capsuleId, context.coupleId());
        openIfDue(capsule, utcNow());
        if (capsule.getStatus() != TimeCapsuleStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "sealed capsule cannot be shared");
        }

        int photoCount = timeCapsuleMediaRepository.findByCapsuleIdOrderByOrderIndexAsc(capsule.getId()).size();
        return buildShareCard(capsule, photoCount);
    }

    @Transactional
    public int openExpiredCapsules() {
        LocalDateTime now = utcNow();
        List<TimeCapsule> expiredCapsules = timeCapsuleRepository.findByStatusAndOpenAtLessThanEqual(
                TimeCapsuleStatus.SEALED,
                now
        );
        expiredCapsules.forEach(capsule -> capsule.open(now));

        return expiredCapsules.size();
    }

    private void savePhotos(Long capsuleId, List<String> photoObjectKeys) {
        if (photoObjectKeys == null || photoObjectKeys.isEmpty()) {
            return;
        }
        for (int index = 0; index < photoObjectKeys.size(); index++) {
            String objectKey = normalizeObjectKey(photoObjectKeys.get(index));
            timeCapsuleMediaRepository.save(TimeCapsuleMedia.builder()
                    .capsuleId(capsuleId)
                    .mediaType(TimeCapsuleMediaType.PHOTO)
                    .objectKey(objectKey)
                    .orderIndex(index)
                    .build());
        }
    }

    private TimeCapsuleDetailResponse toDetail(TimeCapsule capsule, boolean includeSealedContent) {
        TimeCapsuleLetterResponse letter = null;
        List<TimeCapsulePhotoResponse> photos = List.of();
        TimeCapsuleShareCardResponse shareCard = null;
        TimeCapsuleSnapshot snapshot = timeCapsuleSnapshotRepository.findByCapsuleId(capsule.getId()).orElse(null);

        if (includeSealedContent) {
            List<TimeCapsuleMedia> media = timeCapsuleMediaRepository.findByCapsuleIdOrderByOrderIndexAsc(capsule.getId());
            letter = timeCapsuleLetterRepository.findByCapsuleId(capsule.getId())
                    .map(this::toLetter)
                    .orElse(null);
            photos = media.stream()
                    .map(this::toPhoto)
                    .toList();
            shareCard = buildShareCard(capsule, media.size());
        }

        return TimeCapsuleDetailResponse.builder()
                .id(capsule.getId())
                .name(capsule.getName())
                .status(capsule.getStatus())
                .coverStyle(capsule.getCoverStyle())
                .sealedAt(capsule.getSealedAt())
                .openAt(capsule.getOpenAt())
                .openedAt(capsule.getOpenedAt())
                .letter(letter)
                .photos(photos)
                .thenVsNow(includeSealedContent
                        ? timeCapsuleSnapshotService.compare(capsule.getCoupleId(), snapshot, utcNow())
                        : null)
                .shareCard(shareCard)
                .build();
    }

    private TimeCapsuleSummaryResponse toSummary(TimeCapsule capsule) {
        return TimeCapsuleSummaryResponse.builder()
                .id(capsule.getId())
                .name(capsule.getName())
                .status(capsule.getStatus())
                .coverStyle(capsule.getCoverStyle())
                .openAt(capsule.getOpenAt())
                .sealedAt(capsule.getSealedAt())
                .openedAt(capsule.getOpenedAt())
                .dDay(calculateDDay(capsule))
                .build();
    }

    private TimeCapsuleLetterResponse toLetter(TimeCapsuleLetter letter) {
        return TimeCapsuleLetterResponse.builder()
                .authorId(letter.getAuthorId())
                .authorName(null)
                .text(letter.getText())
                .writtenAt(letter.getWrittenAt())
                .build();
    }

    private TimeCapsulePhotoResponse toPhoto(TimeCapsuleMedia photo) {
        return TimeCapsulePhotoResponse.builder()
                .id(photo.getId())
                .url(timeCapsuleMediaAccessService.createReadUrl(photo.getObjectKey()))
                .objectKey(photo.getObjectKey())
                .caption(photo.getCaption())
                .capturedAt(photo.getCapturedAt())
                .orderIndex(photo.getOrderIndex())
                .build();
    }

    private TimeCapsuleShareCardResponse buildShareCard(TimeCapsule capsule, int photoCount) {
        return TimeCapsuleShareCardResponse.builder()
                .badgeText("HEAR2 TIME CAPSULE")
                .headline(capsule.getName() + "이 열렸어요")
                .subheadline("우리가 봉인해 둔 마음이 도착했어요")
                .dateText("봉인: " + formatDate(capsule.getSealedAt()) + " -> 오픈: " + formatDate(capsule.getOpenAt()))
                .photoCount(photoCount)
                .gradientStartColor(resolveGradientStartColor(capsule))
                .gradientEndColor(resolveGradientEndColor(capsule))
                .highlightText(photoCount > 0
                        ? "편지와 사진 " + photoCount + "장이 함께 열렸어요"
                        : "편지가 열렸어요")
                .footerMessage("Hear2 Time Capsule")
                .build();
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(SHARE_DATE_FORMATTER);
    }

    private String resolveGradientStartColor(TimeCapsule capsule) {
        return switch (capsule.getCoverStyle()) {
            case LETTER -> "#6D5BFF";
            case GIFT -> "#FF6B8B";
            case FLOWER -> "#FF8BB7";
            case SPACE -> "#302B8F";
            case CHERRY -> "#FF4F93";
        };
    }

    private String resolveGradientEndColor(TimeCapsule capsule) {
        return switch (capsule.getCoverStyle()) {
            case LETTER -> "#FF4F93";
            case GIFT -> "#FFB36F";
            case FLOWER -> "#B69CFF";
            case SPACE -> "#7B61FF";
            case CHERRY -> "#FFD1E4";
        };
    }

    private long calculateDDay(TimeCapsule capsule) {
        if (capsule.getStatus() == TimeCapsuleStatus.OPEN) {
            return 0;
        }

        return Math.max(0, ChronoUnit.DAYS.between(utcNow().toLocalDate(), capsule.getOpenAt().toLocalDate()));
    }

    private void openIfDue(TimeCapsule capsule, LocalDateTime now) {
        if (capsule.canOpen(now)) {
            capsule.open(now);
        }
    }

    private TimeCapsule findCapsule(Long capsuleId, Long coupleId) {
        if (capsuleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capsuleId is required");
        }

        return timeCapsuleRepository.findByIdAndCoupleId(capsuleId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "time capsule not found"));
    }

    private CoupleContext resolveCoupleContext(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "connected couple is required"));
        return new CoupleContext(member.getCoupleId(), currentUserId);
    }

    private void validateCreateRequest(TimeCapsuleCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "time capsule request is required");
        }
        if (!StringUtils.hasText(request.getLetter())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "letter is required");
        }
        if (request.getPhotoObjectKeys() != null && request.getPhotoObjectKeys().size() > MAX_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoObjectKeys can contain up to 20 items");
        }
        if (request.getOpenAt() != null && !request.getOpenAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "openAt must be a future time");
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoObjectKeys cannot contain blank value");
        }
        String normalized = objectKey.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoObjectKeys must be object keys, not public URLs");
        }
        if (normalized.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid photo object key");
        }

        return normalized;
    }

    private String writeOptions(TimeCapsuleCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getOptions());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "options must be valid JSON", ex);
        }
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toLowerCase(Locale.ROOT) : "all";
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime utcNow() {
        return OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime();
    }

    private record CoupleContext(Long coupleId, Long userId) {
    }
}
