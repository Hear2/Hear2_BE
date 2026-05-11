package com.hear2.memory.service;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.memory.dto.MemoryCreateRequest;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.dto.MemoryUpdateRequest;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiTag;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

        memory.attachPhotoMetadata(MemoryPhotoMetadata.create(
                takenAt,
                latitude,
                longitude,
                resolvedLocation.locationName(),
                resolvedLocation.placeName(),
                resolvedLocation.addressName()
        ));

        applyAiAnalysis(memory, sanitizedPhoto, request);

        return MemoryResponse.from(memoryRepository.save(memory));
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> getAlbum(Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);

        return memoryRepository.findByCoupleIdOrderByMemoryDateDescCreatedAtDesc(coupleId)
                .stream()
                .map(MemoryResponse::from)
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
                .map(MemoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemoryResponse getMemory(Long memoryId, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        return MemoryResponse.from(findMemory(coupleId, memoryId));
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

        return MemoryResponse.from(memory);
    }

    @Transactional
    public void deleteMemory(Long memoryId, Long currentUserId) {
        Long coupleId = resolveCoupleId(currentUserId);
        Memory memory = findMemory(coupleId, memoryId);

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

    private List<MemoryAiTag> toAiTags(MemoryAiAnalysisResult analysisResult) {
        if (analysisResult == null || analysisResult.getTags() == null) {
            return List.of();
        }

        return analysisResult.getTags().stream()
                .filter(tag -> tag != null && StringUtils.hasText(tag.tagName()))
                .map(tag -> MemoryAiTag.ai(tag.tagName().trim(), tag.confidence()))
                .toList();
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
        String requestedLocationName = normalizeLocationName(request.getLocationName());
        KakaoLocationNames kakaoLocationNames = kakaoLocalService.resolveLocationNames(latitude, longitude);
        String addressName = kakaoLocationNames == null ? null : kakaoLocationNames.addressName();

        if (requestedLocationName != null) {
            return new ResolvedMemoryLocation(requestedLocationName, requestedLocationName, addressName);
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

    private String normalizeMemo(String memo) {
        return StringUtils.hasText(memo) ? memo.trim() : null;
    }

    private String normalizeLocationName(String locationName) {
        return StringUtils.hasText(locationName) ? locationName.trim() : null;
    }

    private record ResolvedMemoryLocation(
            String locationName,
            String placeName,
            String addressName
    ) {
    }
}
