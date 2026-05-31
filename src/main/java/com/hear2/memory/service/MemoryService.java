package com.hear2.memory.service;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.memory.dto.MemoryCreateRequest;
import com.hear2.memory.dto.MemoryCalendarDayResponse;
import com.hear2.memory.dto.MemoryCalendarResponse;
import com.hear2.memory.dto.MemoryImageTagRequest;
import com.hear2.memory.dto.MemoryImageTagResponse;
import com.hear2.memory.dto.MemoryQuickCreateRequest;
import com.hear2.memory.dto.MemoryQuickResponse;
import com.hear2.memory.dto.MemoryQuickUpdateRequest;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.dto.MemoryUpdateRequest;
import com.hear2.memory.dto.MemoryYearAgoResponse;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiTag;
import com.hear2.memory.entity.MemoryPhoto;
import com.hear2.memory.entity.MemoryPhotoMetadata;
import com.hear2.memory.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryPhotoStorageService memoryPhotoStorageService;
    private final MemoryAiAnalysisService memoryAiAnalysisService;
    private final MemoryPhotoMetadataExtractor memoryPhotoMetadataExtractor;
    private final MemoryPhotoSanitizer memoryPhotoSanitizer;
    private final KakaoLocalService kakaoLocalService;
    private final CoupleMemberRepository coupleMemberRepository;

    @Transactional
    public MemoryResponse createMemory(MultipartFile photo, MemoryCreateRequest request, Long currentUserId) {
        validateCreateRequest(request);
        Long coupleId = resolveCoupleId(currentUserId);

        ExtractedMemoryPhotoMetadata extractedMetadata = memoryPhotoMetadataExtractor.extract(photo);
        LocalDateTime takenAt = resolveTakenAt(request, extractedMetadata);
        BigDecimal latitude = resolveLatitude(request, extractedMetadata);
        BigDecimal longitude = resolveLongitude(request, extractedMetadata);
        ResolvedMemoryLocation resolvedLocation = resolveLocation(request, latitude, longitude);
        MemoryPhotoFile sanitizedPhoto = memoryPhotoSanitizer.sanitize(photo);

        MemoryPhotoStorageResult storedPhoto = memoryPhotoStorageService.store(sanitizedPhoto, coupleId);
        Memory memory = Memory.builder()
                .coupleId(coupleId)
                .uploaderId(currentUserId)
                .storedPhotoPath(storedPhoto.storedPhotoPath())
                .originalFileName(storedPhoto.originalFileName())
                .photoContentType(storedPhoto.contentType())
                .photoSize(storedPhoto.size())
                .memo(normalizeMemo(request.getMemo()))
                .memoryDate(resolveMemoryDate(takenAt))
                .build();
        memory.replacePhotos(List.of(toMemoryPhoto(storedPhoto, 0)));

        memory.attachPhotoMetadata(MemoryPhotoMetadata.create(
                takenAt,
                latitude,
                longitude,
                resolvedLocation.locationName(),
                resolvedLocation.placeName(),
                resolvedLocation.addressName()
        ));

        applyAiAnalysis(memory, sanitizedPhoto, request);
        applyUserTags(memory, request.getUserTags());

        return toMemoryResponse(memoryRepository.save(memory));
    }

    @Transactional
    public MemoryQuickResponse createQuickMemory(MemoryQuickCreateRequest request, Long currentUserId) {
        validateQuickCreateRequest(request);
        Long coupleId = resolveCoupleId(currentUserId);

        LocalDateTime capturedAt = toLocalDateTime(request.getCapturedAt());
        ResolvedMemoryLocation resolvedLocation = resolveLocation(request.getLocationName(), request.getLat(), request.getLng());
        List<MemoryPhotoStorageResult> referencedPhotos = resolveStoredPhotoReferences(request).stream()
                .map(memoryPhotoStorageService::referenceExternal)
                .toList();
        MemoryPhotoStorageResult coverPhoto = referencedPhotos.get(0);

        Memory memory = Memory.builder()
                .coupleId(coupleId)
                .uploaderId(currentUserId)
                .storedPhotoPath(coverPhoto.storedPhotoPath())
                .originalFileName(coverPhoto.originalFileName())
                .photoContentType(coverPhoto.contentType())
                .photoSize(coverPhoto.size())
                .memo(null)
                .memoryDate(resolveMemoryDate(capturedAt))
                .build();
        memory.replacePhotos(toMemoryPhotos(referencedPhotos));

        memory.attachPhotoMetadata(MemoryPhotoMetadata.create(
                capturedAt,
                request.getLat(),
                request.getLng(),
                resolvedLocation.locationName(),
                resolvedLocation.placeName(),
                resolvedLocation.addressName()
        ));

        applyAiAnalysis(memory, request, resolvedLocation);
        applyUserTags(memory, request.getUserTags());

        return toQuickResponse(memoryRepository.save(memory));
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> getAlbum(Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);

        return memoryRepository.findByCoupleIdOrderByMemoryDateDescCreatedAtDesc(coupleId)
                .stream()
                .map(this::toMemoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> getMemoriesByDate(LocalDate memoryDate, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        if (memoryDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memoryDate is required");
        }

        return memoryRepository.findByCoupleIdAndMemoryDateOrderByCreatedAtDesc(coupleId, memoryDate)
                .stream()
                .map(this::toMemoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemoryCalendarResponse getCalendar(int year, int month, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        YearMonth yearMonth = validateYearMonth(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Map<LocalDate, List<Memory>> memoriesByDate = memoryRepository
                .findByCoupleIdAndMemoryDateGreaterThanEqualAndMemoryDateLessThanEqualOrderByMemoryDateAscCreatedAtDesc(
                        coupleId,
                        startDate,
                        endDate
                )
                .stream()
                .collect(Collectors.groupingBy(Memory::getMemoryDate));

        List<MemoryCalendarDayResponse> days = memoriesByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> MemoryCalendarDayResponse.builder()
                        .date(entry.getKey())
                        .thumbnails(entry.getValue().stream()
                                .limit(3)
                                .map(this::resolveCoverPhotoUrl)
                                .toList())
                        .memoryCount(entry.getValue().size())
                        .dominantEmoji("😊")
                        .build())
                .toList();

        return MemoryCalendarResponse.builder()
                .year(year)
                .month(month)
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public MemoryYearAgoResponse getYearAgo(Long currentUserId) {
        LocalDate date = LocalDate.now().minusYears(1);
        List<MemoryResponse> items = getMemoriesByDate(date, currentUserId);

        return MemoryYearAgoResponse.builder()
                .exists(!items.isEmpty())
                .items(items)
                .summary(buildYearAgoSummary(items))
                .build();
    }

    @Transactional(readOnly = true)
    public MemoryImageTagResponse analyzeImageTags(MemoryImageTagRequest request, Long currentUserId) {
        validateCurrentUserId(currentUserId);
        if (request == null || !StringUtils.hasText(request.getImageUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl is required");
        }

        MemoryAiAnalysisResult analysisResult = memoryAiAnalysisService.analyzeImageUrl(
                request.getImageUrl(),
                null,
                null,
                null
        );
        List<MemoryAiTag> aiTags = toAiTags(analysisResult);
        List<String> tags = aiTags.stream()
                .map(tag -> "#" + tag.getTagName())
                .toList();

        return MemoryImageTagResponse.builder()
                .tags(tags)
                .scene(null)
                .confidence(resolveAverageConfidence(aiTags))
                .build();
    }

    @Transactional(readOnly = true)
    public MemoryResponse getMemory(Long memoryId, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        return toMemoryResponse(findMemory(coupleId, memoryId));
    }

    @Transactional(readOnly = true)
    public MemoryPhotoContent getMemoryPhoto(Long memoryId, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        Memory memory = findMemory(coupleId, memoryId);

        return memoryPhotoStorageService.load(memory.getStoredPhotoPath(), memory.getPhotoContentType());
    }

    @Transactional
    public MemoryResponse updateMemory(Long memoryId, MemoryUpdateRequest request, Long currentUserId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memory update request is required");
        }

        Long coupleId = resolveCoupleId(currentUserId);
        Memory memory = findMemory(coupleId, memoryId);
        memory.updateMemo(normalizeMemo(request.getMemo()));
        if (request.getUserTags() != null) {
            applyUserTags(memory, request.getUserTags());
        }

        return toMemoryResponse(memory);
    }

    @Transactional
    public MemoryQuickResponse updateQuickMemory(Long memoryId, MemoryQuickUpdateRequest request, Long currentUserId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memory update request is required");
        }

        Long coupleId = resolveCoupleId(currentUserId);
        Memory memory = findMemory(coupleId, memoryId);
        memory.updateMemo(normalizeMemo(request.getNote()));
        if (hasLocationUpdate(request)) {
            updateQuickMemoryLocation(memory, request);
        }
        if (request.getUserTags() != null) {
            applyUserTags(memory, request.getUserTags());
        }

        return toQuickResponse(memory);
    }

    @Transactional
    public void deleteMemory(Long memoryId, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        Memory memory = findMemory(coupleId, memoryId);

        memory.getPhotos().stream()
                .map(photo -> photo.getStoredPhotoPath())
                .filter(path -> !path.equals(memory.getStoredPhotoPath()))
                .forEach(memoryPhotoStorageService::delete);
        memoryPhotoStorageService.delete(memory.getStoredPhotoPath());
        memoryRepository.delete(memory);
    }

    private void applyAiAnalysis(Memory memory, MemoryPhotoFile photo, MemoryCreateRequest request) {
        MemoryAiAnalysisResult analysisResult = memoryAiAnalysisService.analyze(photo, request);
        memory.replaceAiTags(toAiTags(analysisResult));

        if (analysisResult.isCompleted()) {
            memory.markAiAnalyzed();
        } else if (analysisResult.isFailed()) {
            memory.markAiAnalysisFailed();
        }
    }

    private void applyAiAnalysis(
            Memory memory,
            MemoryQuickCreateRequest request,
            ResolvedMemoryLocation resolvedLocation
    ) {
        MemoryAiAnalysisResult analysisResult = memoryAiAnalysisService.analyzeImageUrl(
                resolveAnalysisImageUrl(request),
                null,
                resolvedLocation.locationName(),
                toLocalDateTime(request.getCapturedAt())
        );
        memory.replaceAiTags(toAiTags(analysisResult));

        if (analysisResult.isCompleted()) {
            memory.markAiAnalyzed();
        } else if (analysisResult.isFailed()) {
            memory.markAiAnalysisFailed();
        }
    }

    private List<MemoryAiTag> toAiTags(MemoryAiAnalysisResult analysisResult) {
        if (analysisResult == null || analysisResult.getTags() == null) {
            return List.of();
        }

        return analysisResult.getTags().stream()
                .filter(tag -> tag != null && StringUtils.hasText(tag.tagName()))
                .map(tag -> MemoryAiTag.ai(tag.tagName().trim(), tag.confidence()))
                .toList();
    }

    private BigDecimal resolveAverageConfidence(List<MemoryAiTag> tags) {
        List<BigDecimal> confidences = tags.stream()
                .map(MemoryAiTag::getConfidence)
                .filter(confidence -> confidence != null)
                .toList();
        if (confidences.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal sum = confidences.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(confidences.size()), 4, RoundingMode.HALF_UP);
    }

    private void applyUserTags(Memory memory, List<String> userTags) {
        memory.replaceUserTags(normalizeTags(userTags).stream()
                .map(MemoryAiTag::user)
                .toList());
    }

    private List<MemoryPhoto> toMemoryPhotos(List<MemoryPhotoStorageResult> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }

        ArrayList<MemoryPhoto> memoryPhotos = new ArrayList<>();
        for (int index = 0; index < photos.size(); index++) {
            memoryPhotos.add(toMemoryPhoto(photos.get(index), index));
        }
        return memoryPhotos;
    }

    private MemoryPhoto toMemoryPhoto(MemoryPhotoStorageResult photo, int sortOrder) {
        return MemoryPhoto.create(
                photo.storedPhotoPath(),
                photo.originalFileName(),
                photo.contentType(),
                photo.size(),
                sortOrder
        );
    }

    private MemoryResponse toMemoryResponse(Memory memory) {
        return MemoryResponse.from(memory, storedPhotoPath -> resolvePhotoUrl(storedPhotoPath, memory));
    }

    private MemoryQuickResponse toQuickResponse(Memory memory) {
        return MemoryQuickResponse.from(memory, storedPhotoPath -> resolvePhotoUrl(storedPhotoPath, memory));
    }

    private String resolveCoverPhotoUrl(Memory memory) {
        return resolvePhotoUrl(memory.getStoredPhotoPath(), memory);
    }

    private String resolvePhotoUrl(String storedPhotoPath, Memory memory) {
        String readUrl = memoryPhotoStorageService.createReadUrl(storedPhotoPath);
        if (StringUtils.hasText(readUrl)) {
            return readUrl;
        }
        if (memory != null && storedPhotoPath != null && storedPhotoPath.equals(memory.getStoredPhotoPath())) {
            return MemoryResponse.resolvePhotoUrl(memory);
        }

        return MemoryResponse.resolvePhotoUrl(storedPhotoPath);
    }

    private Memory findMemory(Long coupleId, Long memoryId) {
        validateCoupleId(coupleId);
        if (memoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memoryId is required");
        }

        return memoryRepository.findByIdAndCoupleId(memoryId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "memory not found"));
    }

    private void validateCreateRequest(MemoryCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memory create request is required");
        }
    }

    private void validateQuickCreateRequest(MemoryQuickCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memory quick create request is required");
        }
        if (resolveStoredPhotoReferences(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey, objectKeys, or imageUrl is required");
        }
    }

    private void validateCoupleId(Long coupleId) {
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
    }

    private void validateCurrentUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }
    }

    private Long resolveCoupleId(Long currentUserId) {
        validateCurrentUserId(currentUserId);

        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));

        return member.getCoupleId();
    }

    private LocalDate resolveMemoryDate(LocalDateTime takenAt) {
        if (takenAt == null) {
            return LocalDate.now();
        }

        return takenAt.toLocalDate();
    }

    private LocalDateTime resolveTakenAt(
            MemoryCreateRequest request,
            ExtractedMemoryPhotoMetadata extractedMetadata
    ) {
        if (request.getTakenAt() != null) {
            return request.getTakenAt();
        }

        return extractedMetadata == null ? null : extractedMetadata.takenAt();
    }

    private BigDecimal resolveLatitude(
            MemoryCreateRequest request,
            ExtractedMemoryPhotoMetadata extractedMetadata
    ) {
        if (request.getLatitude() != null) {
            return request.getLatitude();
        }

        return extractedMetadata == null ? null : extractedMetadata.latitude();
    }

    private BigDecimal resolveLongitude(
            MemoryCreateRequest request,
            ExtractedMemoryPhotoMetadata extractedMetadata
    ) {
        if (request.getLongitude() != null) {
            return request.getLongitude();
        }

        return extractedMetadata == null ? null : extractedMetadata.longitude();
    }

    private ResolvedMemoryLocation resolveLocation(
            MemoryCreateRequest request,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String requestedLocationName = request == null ? null : request.getLocationName();
        return resolveLocation(requestedLocationName, latitude, longitude);
    }

    private ResolvedMemoryLocation resolveLocation(
            String requestedLocationName,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String normalizedLocationName = normalizeLocationName(requestedLocationName);
        KakaoLocationNames kakaoLocationNames = kakaoLocalService.resolveLocationNames(latitude, longitude);
        String addressName = kakaoLocationNames == null ? null : kakaoLocationNames.addressName();

        if (normalizedLocationName != null) {
            return new ResolvedMemoryLocation(normalizedLocationName, normalizedLocationName, addressName);
        }

        if (kakaoLocationNames == null) {
            return new ResolvedMemoryLocation(null, null, null);
        }

        return new ResolvedMemoryLocation(
                kakaoLocationNames.displayName(),
                kakaoLocationNames.placeName(),
                kakaoLocationNames.addressName()
        );
    }

    private boolean hasLocationUpdate(MemoryQuickUpdateRequest request) {
        return request.getLocationName() != null || request.getLat() != null || request.getLng() != null;
    }

    private void updateQuickMemoryLocation(Memory memory, MemoryQuickUpdateRequest request) {
        MemoryPhotoMetadata metadata = memory.getPhotoMetadata();
        BigDecimal latitude = request.getLat() != null
                ? request.getLat()
                : metadata == null ? null : metadata.getLatitude();
        BigDecimal longitude = request.getLng() != null
                ? request.getLng()
                : metadata == null ? null : metadata.getLongitude();
        String requestedLocationName = request.getLocationName() != null
                ? request.getLocationName()
                : metadata == null ? null : metadata.getLocationName();
        ResolvedMemoryLocation resolvedLocation = resolveLocation(requestedLocationName, latitude, longitude);

        if (metadata == null) {
            memory.attachPhotoMetadata(MemoryPhotoMetadata.create(
                    null,
                    latitude,
                    longitude,
                    resolvedLocation.locationName(),
                    resolvedLocation.placeName(),
                    resolvedLocation.addressName()
            ));
            return;
        }

        metadata.updateLocation(
                latitude,
                longitude,
                resolvedLocation.locationName(),
                resolvedLocation.placeName(),
                resolvedLocation.addressName()
        );
    }

    private YearMonth validateYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year and month must be valid");
        }
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime capturedAt) {
        if (capturedAt == null) {
            return null;
        }

        return LocalDateTime.ofInstant(capturedAt.toInstant(), ZoneOffset.UTC);
    }

    private String normalizeMemo(String memo) {
        return StringUtils.hasText(memo) ? memo.trim() : null;
    }

    private String resolveStoredPhotoReference(MemoryQuickCreateRequest request) {
        List<String> references = resolveStoredPhotoReferences(request);
        return references.isEmpty() ? null : references.get(0);
    }

    private List<String> resolveStoredPhotoReferences(MemoryQuickCreateRequest request) {
        LinkedHashSet<String> references = new LinkedHashSet<>();

        if (request.getObjectKeys() != null) {
            request.getObjectKeys().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(references::add);
        }
        if (StringUtils.hasText(request.getObjectKey())) {
            references.add(request.getObjectKey().trim());
        }
        if (StringUtils.hasText(request.getImageUrl())) {
            references.add(request.getImageUrl().trim());
        }

        return List.copyOf(references);
    }

    private String resolveAnalysisImageUrl(MemoryQuickCreateRequest request) {
        if (StringUtils.hasText(request.getImageUrl())) {
            return request.getImageUrl().trim();
        }

        return memoryPhotoStorageService.createReadUrl(resolveStoredPhotoReference(request));
    }

    private String normalizeLocationName(String locationName) {
        return StringUtils.hasText(locationName) ? locationName.trim() : null;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
                .filter(StringUtils::hasText)
                .map(tag -> tag.replace("#", "").trim())
                .filter(StringUtils::hasText)
                .distinct()
                .limit(20)
                .toList();
    }

    private String buildYearAgoSummary(List<MemoryResponse> items) {
        if (items.isEmpty()) {
            return null;
        }

        MemoryResponse first = items.get(0);
        String locationName = first.getMetadata() == null ? null : first.getMetadata().getLocationName();
        if (StringUtils.hasText(locationName)) {
            return locationName + "에서의 추억";
        }

        return "1년 전 오늘의 추억 " + items.size() + "개";
    }

    private record ResolvedMemoryLocation(
            String locationName,
            String placeName,
            String addressName
    ) {
    }
}
