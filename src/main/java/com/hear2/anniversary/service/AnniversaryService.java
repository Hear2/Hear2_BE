package com.hear2.anniversary.service;

import com.hear2.anniversary.dto.AnniversaryCreateRequest;
import com.hear2.anniversary.dto.AnniversaryListResponse;
import com.hear2.anniversary.dto.AnniversaryResponse;
import com.hear2.anniversary.dto.AnniversaryStartDateRequest;
import com.hear2.anniversary.dto.AnniversaryUpdateRequest;
import com.hear2.anniversary.entity.Anniversary;
import com.hear2.anniversary.entity.HiddenAutoAnniversary;
import com.hear2.anniversary.repository.AnniversaryRepository;
import com.hear2.anniversary.repository.HiddenAutoAnniversaryRepository;
import com.hear2.anniversary.support.AnniversaryDdayType;
import com.hear2.anniversary.support.AnniversaryType;
import com.hear2.anniversary.support.AutoAnniversaryType;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnniversaryService {

    private static final int DEFAULT_YEAR_ANNIVERSARY_LIMIT = 10;
    private static final int YEAR_ANNIVERSARY_EXTRA_AFTER_CURRENT = 3;
    private static final List<Integer> AUTO_DAY_MILESTONES = List.of(100, 200, 300, 500, 1000);
    private static final Set<Integer> ALLOWED_NOTIFY_DAYS = Set.of(0, 1, 3, 7);

    private final CoupleMemberRepository coupleMemberRepository;
    private final CoupleRepository coupleRepository;
    private final AnniversaryRepository anniversaryRepository;
    private final HiddenAutoAnniversaryRepository hiddenAutoAnniversaryRepository;

    @Transactional
    public AnniversaryListResponse updateStartDate(AnniversaryStartDateRequest request, Long currentUserId) {
        if (request == null || request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate is required");
        }
        if (request.getStartDate().isAfter(today())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be in the future");
        }

        CoupleContext context = resolveCoupleContext(currentUserId);
        context.couple().updateStartDate(request.getStartDate());
        return getAnniversaries(currentUserId);
    }

    @Transactional(readOnly = true)
    public AnniversaryListResponse getAnniversaries(Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        LocalDate today = today();
        List<AnniversaryResponse> anniversaries = findVisibleAnniversaries(context, today).stream()
                .sorted(anniversaryComparator(today))
                .toList();

        return AnniversaryListResponse.builder()
                .startDate(context.couple().getStartDate())
                .daysTogether(resolveDaysTogether(context.couple().getStartDate(), today))
                .anniversaries(anniversaries)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnniversaryResponse> getUpcoming(Integer limit, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        LocalDate today = today();
        int normalizedLimit = normalizeLimit(limit);

        return findVisibleAnniversaries(context, today).stream()
                .filter(anniversary -> !anniversary.displayDate().isBefore(today))
                .sorted(Comparator
                        .comparing(AnniversaryResponse::displayDate)
                        .thenComparing(AnniversaryResponse::title))
                .limit(normalizedLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnniversaryResponse> getAnniversariesBetween(
            LocalDate startDate,
            LocalDate endDate,
            Long currentUserId
    ) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }

        CoupleContext context = resolveCoupleContext(currentUserId);
        LocalDate today = today();
        return findVisibleAnniversariesBetween(context, startDate, endDate, today).stream()
                .filter(anniversary -> !anniversary.displayDate().isBefore(startDate))
                .filter(anniversary -> !anniversary.displayDate().isAfter(endDate))
                .sorted(Comparator
                        .comparing(AnniversaryResponse::displayDate)
                        .thenComparing(AnniversaryResponse::title))
                .toList();
    }

    @Transactional
    public AnniversaryResponse createAnniversary(AnniversaryCreateRequest request, Long currentUserId) {
        validateCreateRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        Anniversary anniversary = anniversaryRepository.save(Anniversary.builder()
                .coupleId(context.coupleId())
                .createdBy(context.userId())
                .title(normalizeRequired(request.getTitle(), "title is required"))
                .type(request.getType())
                .anniversaryDate(request.getDate())
                .ddayType(normalizeDdayType(request.getDdayType()))
                .repeatYearly(request.isRepeatYearly())
                .shared(request.isShared())
                .icon(normalizeIcon(request.getIcon(), request.getType()))
                .color(normalizeColor(request.getColor()))
                .notifyDays(normalizeNotifyDays(request.getNotifyDays()))
                .build());

        return toCustomResponse(anniversary, today(), context.userId());
    }

    @Transactional
    public AnniversaryResponse updateAnniversary(Long anniversaryId, AnniversaryUpdateRequest request, Long currentUserId) {
        validateUpdateRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        Anniversary anniversary = findEditableAnniversary(context, anniversaryId);
        anniversary.update(
                normalizeRequired(request.getTitle(), "title is required"),
                request.getType(),
                request.getDate(),
                normalizeDdayType(request.getDdayType()),
                request.isRepeatYearly(),
                request.isShared(),
                normalizeIcon(request.getIcon(), request.getType()),
                normalizeColor(request.getColor()),
                normalizeNotifyDays(request.getNotifyDays())
        );

        return toCustomResponse(anniversary, today(), context.userId());
    }

    @Transactional
    public void deleteAnniversary(Long anniversaryId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        Anniversary anniversary = findEditableAnniversary(context, anniversaryId);
        anniversaryRepository.delete(anniversary);
    }

    @Transactional
    public AnniversaryListResponse hideAutoAnniversary(String autoKey, Long currentUserId) {
        String normalizedKey = normalizeAutoKey(autoKey);
        CoupleContext context = resolveCoupleContext(currentUserId);
        ensureAutoKeyExists(context, normalizedKey);

        hiddenAutoAnniversaryRepository.findByCoupleIdAndUserIdAndAutoKey(
                        context.coupleId(),
                        context.userId(),
                        normalizedKey
                )
                .orElseGet(() -> hiddenAutoAnniversaryRepository.save(HiddenAutoAnniversary.builder()
                        .coupleId(context.coupleId())
                        .userId(context.userId())
                        .autoKey(normalizedKey)
                        .build()));

        return getAnniversaries(currentUserId);
    }

    @Transactional
    public AnniversaryListResponse showAutoAnniversary(String autoKey, Long currentUserId) {
        String normalizedKey = normalizeAutoKey(autoKey);
        CoupleContext context = resolveCoupleContext(currentUserId);
        hiddenAutoAnniversaryRepository.deleteByCoupleIdAndUserIdAndAutoKey(
                context.coupleId(),
                context.userId(),
                normalizedKey
        );

        return getAnniversaries(currentUserId);
    }

    private List<AnniversaryResponse> findVisibleAnniversaries(CoupleContext context, LocalDate today) {
        List<AnniversaryResponse> anniversaries = new ArrayList<>();
        anniversaries.addAll(generateAutoAnniversaries(context, today));
        anniversaries.addAll(anniversaryRepository.findByCoupleIdOrderByAnniversaryDateAscIdAsc(context.coupleId())
                .stream()
                .filter(anniversary -> anniversary.isShared() || Objects.equals(anniversary.getCreatedBy(), context.userId()))
                .map(anniversary -> toCustomResponse(anniversary, today, context.userId()))
                .toList());
        return anniversaries;
    }

    private List<AnniversaryResponse> findVisibleAnniversariesBetween(
            CoupleContext context,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today
    ) {
        List<AnniversaryResponse> anniversaries = new ArrayList<>();
        anniversaries.addAll(generateAutoAnniversaries(context, today));
        anniversaries.addAll(anniversaryRepository.findByCoupleIdOrderByAnniversaryDateAscIdAsc(context.coupleId())
                .stream()
                .filter(anniversary -> anniversary.isShared() || Objects.equals(anniversary.getCreatedBy(), context.userId()))
                .flatMap(anniversary -> toCustomResponsesBetween(anniversary, startDate, endDate, today, context.userId()).stream())
                .toList());
        return anniversaries;
    }

    private List<AnniversaryResponse> generateAutoAnniversaries(CoupleContext context, LocalDate today) {
        LocalDate startDate = context.couple().getStartDate();
        if (startDate == null) {
            return List.of();
        }

        Set<String> hiddenKeys = hiddenAutoAnniversaryRepository
                .findByCoupleIdAndUserId(context.coupleId(), context.userId())
                .stream()
                .map(HiddenAutoAnniversary::getAutoKey)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        List<AnniversaryResponse> anniversaries = new ArrayList<>();
        addAutoAnniversary(
                anniversaries,
                hiddenKeys,
                "LOVE_DAY",
                "사귄 날",
                AnniversaryType.LOVE_DAY,
                AutoAnniversaryType.LOVE_DAY,
                startDate,
                AnniversaryDdayType.D_PLUS,
                "HEART",
                "#FF4D8D",
                today
        );

        for (Integer milestone : AUTO_DAY_MILESTONES) {
            addAutoAnniversary(
                    anniversaries,
                    hiddenKeys,
                    "DAY_" + milestone,
                    milestone + "일",
                    AnniversaryType.ANNIVERSARY,
                    toDayAutoType(milestone),
                    startDate.plusDays(milestone - 1L),
                    AnniversaryDdayType.D_MINUS,
                    milestone == 100 ? "HUNDRED" : "PARTY",
                    "#A87BFF",
                    today
            );
        }

        int yearLimit = resolveYearAnniversaryLimit(startDate, today);
        for (int year = 1; year <= yearLimit; year++) {
            addAutoAnniversary(
                    anniversaries,
                    hiddenKeys,
                    "YEAR_" + year,
                    year + "주년",
                    AnniversaryType.ANNIVERSARY,
                    AutoAnniversaryType.YEAR,
                    startDate.plusYears(year),
                    AnniversaryDdayType.D_MINUS,
                    "PARTY",
                    "#A87BFF",
                    today
            );
        }

        return anniversaries;
    }

    private void addAutoAnniversary(
            List<AnniversaryResponse> anniversaries,
            Set<String> hiddenKeys,
            String autoKey,
            String title,
            AnniversaryType type,
            AutoAnniversaryType autoGeneratedType,
            LocalDate date,
            AnniversaryDdayType ddayType,
            String icon,
            String color,
            LocalDate today
    ) {
        if (hiddenKeys.contains(autoKey)) {
            return;
        }

        anniversaries.add(toResponse(
                null,
                autoKey,
                title,
                type,
                date,
                date,
                ddayType,
                false,
                true,
                icon,
                color,
                List.of(),
                true,
                autoGeneratedType,
                null,
                false,
                true,
                today
        ));
    }

    private AnniversaryResponse toCustomResponse(Anniversary anniversary, LocalDate today, Long currentUserId) {
        return toResponse(
                anniversary.getId(),
                null,
                anniversary.getTitle(),
                anniversary.getType(),
                anniversary.getAnniversaryDate(),
                resolveDisplayDate(anniversary, today),
                anniversary.getDdayType(),
                anniversary.isRepeatYearly(),
                anniversary.isShared(),
                anniversary.getIcon(),
                anniversary.getColor(),
                anniversary.getNotifyDays(),
                false,
                null,
                anniversary.getCreatedBy(),
                Objects.equals(anniversary.getCreatedBy(), currentUserId),
                false,
                today
        );
    }

    private List<AnniversaryResponse> toCustomResponsesBetween(
            Anniversary anniversary,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today,
            Long currentUserId
    ) {
        if (!anniversary.isRepeatYearly()) {
            if (anniversary.getAnniversaryDate().isBefore(startDate)
                    || anniversary.getAnniversaryDate().isAfter(endDate)) {
                return List.of();
            }

            return List.of(toCustomResponse(anniversary, today, currentUserId));
        }

        List<AnniversaryResponse> responses = new ArrayList<>();
        for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
            LocalDate displayDate = withYearClamped(anniversary.getAnniversaryDate(), year);
            if (displayDate.isBefore(startDate) || displayDate.isAfter(endDate)) {
                continue;
            }

            responses.add(toCustomResponse(anniversary, displayDate, today, currentUserId));
        }
        return responses;
    }

    private AnniversaryResponse toResponse(
            Long id,
            String autoKey,
            String title,
            AnniversaryType type,
            LocalDate date,
            LocalDate displayDate,
            AnniversaryDdayType ddayType,
            boolean repeatYearly,
            boolean shared,
            String icon,
            String color,
            List<Integer> notifyDays,
            boolean autoGenerated,
            AutoAnniversaryType autoGeneratedType,
            Long createdBy,
            boolean editable,
            boolean hideable,
            LocalDate today
    ) {
        int dday = resolveDday(displayDate, ddayType, today);
        return AnniversaryResponse.builder()
                .id(id)
                .autoKey(autoKey)
                .title(title)
                .type(type)
                .date(date)
                .displayDate(displayDate)
                .ddayType(ddayType)
                .dday(dday)
                .ddayLabel(formatDday(dday, ddayType))
                .repeatYearly(repeatYearly)
                .shared(shared)
                .icon(icon)
                .color(color)
                .notifyDays(notifyDays == null ? List.of() : notifyDays)
                .autoGenerated(autoGenerated)
                .autoGeneratedType(autoGeneratedType)
                .createdBy(createdBy)
                .editable(editable)
                .hideable(hideable)
                .build();
    }

    private LocalDate resolveDisplayDate(Anniversary anniversary, LocalDate today) {
        if (!anniversary.isRepeatYearly()) {
            return anniversary.getAnniversaryDate();
        }

        LocalDate thisYearDate = withYearClamped(anniversary.getAnniversaryDate(), today.getYear());
        if (!thisYearDate.isBefore(today)) {
            return thisYearDate;
        }
        return withYearClamped(anniversary.getAnniversaryDate(), today.getYear() + 1);
    }

    private AnniversaryResponse toCustomResponse(
            Anniversary anniversary,
            LocalDate displayDate,
            LocalDate today,
            Long currentUserId
    ) {
        return toResponse(
                anniversary.getId(),
                null,
                anniversary.getTitle(),
                anniversary.getType(),
                anniversary.getAnniversaryDate(),
                displayDate,
                anniversary.getDdayType(),
                anniversary.isRepeatYearly(),
                anniversary.isShared(),
                anniversary.getIcon(),
                anniversary.getColor(),
                anniversary.getNotifyDays(),
                false,
                null,
                anniversary.getCreatedBy(),
                Objects.equals(anniversary.getCreatedBy(), currentUserId),
                false,
                today
        );
    }

    private LocalDate withYearClamped(LocalDate date, int year) {
        int day = Math.min(date.getDayOfMonth(), YearMonth.of(year, date.getMonth()).lengthOfMonth());
        return LocalDate.of(year, date.getMonth(), day);
    }

    private int resolveDday(LocalDate date, AnniversaryDdayType ddayType, LocalDate today) {
        if (ddayType == AnniversaryDdayType.D_PLUS) {
            return (int) ChronoUnit.DAYS.between(date, today) + 1;
        }

        return (int) ChronoUnit.DAYS.between(today, date);
    }

    private String formatDday(int dday, AnniversaryDdayType ddayType) {
        if (ddayType == AnniversaryDdayType.D_PLUS) {
            return "D+" + dday;
        }
        if (dday == 0) {
            return "D-DAY";
        }
        return dday > 0 ? "D-" + dday : "D+" + Math.abs(dday);
    }

    private Comparator<AnniversaryResponse> anniversaryComparator(LocalDate today) {
        return Comparator
                .comparing((AnniversaryResponse anniversary) -> anniversary.displayDate().isBefore(today))
                .thenComparing(AnniversaryResponse::displayDate)
                .thenComparing(AnniversaryResponse::title);
    }

    private int resolveYearAnniversaryLimit(LocalDate startDate, LocalDate today) {
        long yearsTogether = Math.max(0L, ChronoUnit.YEARS.between(startDate, today));
        return Math.max(DEFAULT_YEAR_ANNIVERSARY_LIMIT, (int) yearsTogether + YEAR_ANNIVERSARY_EXTRA_AFTER_CURRENT);
    }

    private AutoAnniversaryType toDayAutoType(int milestone) {
        return switch (milestone) {
            case 100 -> AutoAnniversaryType.DAY_100;
            case 200 -> AutoAnniversaryType.DAY_200;
            case 300 -> AutoAnniversaryType.DAY_300;
            case 500 -> AutoAnniversaryType.DAY_500;
            case 1000 -> AutoAnniversaryType.DAY_1000;
            default -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "unsupported auto milestone");
        };
    }

    private Anniversary findEditableAnniversary(CoupleContext context, Long anniversaryId) {
        if (anniversaryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anniversaryId is required");
        }

        Anniversary anniversary = anniversaryRepository.findByIdAndCoupleId(anniversaryId, context.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "anniversary not found"));
        if (!Objects.equals(anniversary.getCreatedBy(), context.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only creator can edit this anniversary");
        }
        return anniversary;
    }

    private void ensureAutoKeyExists(CoupleContext context, String autoKey) {
        boolean exists = generateAutoAnniversaries(context, today()).stream()
                .anyMatch(anniversary -> Objects.equals(anniversary.autoKey(), autoKey));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "auto anniversary not found");
        }
    }

    private CoupleContext resolveCoupleContext(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));
        Couple couple = coupleRepository.findById(member.getCoupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
        return new CoupleContext(member.getCoupleId(), currentUserId, couple);
    }

    private void validateCreateRequest(AnniversaryCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anniversary request is required");
        }
        validateAnniversaryDate(request.getDate());
    }

    private void validateUpdateRequest(AnniversaryUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anniversary request is required");
        }
        validateAnniversaryDate(request.getDate());
    }

    private void validateAnniversaryDate(LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }
    }

    private AnniversaryDdayType normalizeDdayType(AnniversaryDdayType ddayType) {
        return ddayType == null ? AnniversaryDdayType.D_MINUS : ddayType;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeIcon(String icon, AnniversaryType type) {
        if (StringUtils.hasText(icon)) {
            return icon.trim();
        }
        return switch (type == null ? AnniversaryType.CUSTOM : type) {
            case LOVE_DAY -> "HEART";
            case BIRTHDAY -> "CAKE";
            case CHRISTMAS -> "TREE";
            case TRIP -> "TRIP";
            case PHOTO_DAY -> "CAMERA";
            default -> "SPARKLE";
        };
    }

    private String normalizeColor(String color) {
        return StringUtils.hasText(color) ? color.trim() : "#FF4D8D";
    }

    private List<Integer> normalizeNotifyDays(List<Integer> notifyDays) {
        if (notifyDays == null) {
            return List.of();
        }

        return notifyDays.stream()
                .filter(Objects::nonNull)
                .filter(ALLOWED_NOTIFY_DAYS::contains)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(4)
                .toList();
    }

    private String normalizeAutoKey(String autoKey) {
        String normalized = normalize(autoKey);
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "autoKey is required");
        }
        return normalized.toUpperCase();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer resolveDaysTogether(LocalDate startDate, LocalDate today) {
        if (startDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(startDate, today) + 1;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be positive");
        }
        return Math.min(limit, 50);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private record CoupleContext(Long coupleId, Long userId, Couple couple) {
    }
}
