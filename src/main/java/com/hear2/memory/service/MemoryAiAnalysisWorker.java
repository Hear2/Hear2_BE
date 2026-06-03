package com.hear2.memory.service;

import com.hear2.memory.entity.Memory;
import com.hear2.memory.entity.MemoryAiTag;
import com.hear2.memory.entity.MemoryPhoto;
import com.hear2.memory.entity.MemoryPhotoAiTag;
import com.hear2.memory.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryAiAnalysisWorker {

    private final MemoryRepository memoryRepository;
    private final MemoryAiAnalysisService memoryAiAnalysisService;
    private final MemoryPhotoStorageService memoryPhotoStorageService;

    @Async
    @Transactional
    public void analyzeQuickMemory(Long memoryId) {
        if (memoryId == null) {
            return;
        }

        Memory memory = memoryRepository.findById(memoryId).orElse(null);
        if (memory == null) {
            log.warn("Memory AI analysis skipped because memory was not found. memoryId={}", memoryId);
            return;
        }

        List<MemoryPhoto> photos = memory.getPhotos();
        if (photos == null || photos.isEmpty()) {
            log.warn(
                    "Memory AI analysis skipped because memory has no photos. memoryId={}, coupleId={}",
                    memory.getId(),
                    memory.getCoupleId()
            );
            return;
        }

        String locationName = memory.getPhotoMetadata() == null ? null : memory.getPhotoMetadata().getLocationName();
        var takenAt = memory.getPhotoMetadata() == null ? null : memory.getPhotoMetadata().getTakenAt();
        MemoryAiAnalysisResult coverAnalysisResult = MemoryAiAnalysisResult.pending();

        log.info(
                "Memory AI analysis started. memoryId={}, coupleId={}, photoCount={}",
                memory.getId(),
                memory.getCoupleId(),
                photos.size()
        );

        for (MemoryPhoto photo : photos) {
            MemoryAiAnalysisResult analysisResult = analyzePhoto(memory, photo, locationName, takenAt);
            applyPhotoAiAnalysis(photo, analysisResult);

            if (photo.getSortOrder() == 0) {
                coverAnalysisResult = analysisResult;
            }
        }

        if (coverAnalysisResult.isCompleted()) {
            memory.replaceAiTags(toAiTags(coverAnalysisResult));
            memory.markAiAnalyzed();
        } else if (coverAnalysisResult.isFailed()) {
            memory.markAiAnalysisFailed();
        }

        log.info(
                "Memory AI analysis finished. memoryId={}, coupleId={}, coverStatus={}",
                memory.getId(),
                memory.getCoupleId(),
                statusText(coverAnalysisResult)
        );
    }

    private MemoryAiAnalysisResult analyzePhoto(
            Memory memory,
            MemoryPhoto photo,
            String locationName,
            java.time.LocalDateTime takenAt
    ) {
        String imageUrl = memoryPhotoStorageService.createReadUrl(photo.getStoredPhotoPath());
        if (!StringUtils.hasText(imageUrl)) {
            log.warn(
                    "Memory AI analysis pending because image URL could not be resolved. memoryId={}, coupleId={}, photoId={}, objectKey={}",
                    memory.getId(),
                    memory.getCoupleId(),
                    photo.getId(),
                    photo.getStoredPhotoPath()
            );
            return MemoryAiAnalysisResult.pending();
        }

        try {
            MemoryAiAnalysisResult result = memoryAiAnalysisService.analyzeImageUrl(
                    imageUrl,
                    memory.getMemo(),
                    locationName,
                    takenAt
            );
            log.info(
                    "Memory photo AI analysis result. memoryId={}, coupleId={}, photoId={}, objectKey={}, status={}, tagCount={}",
                    memory.getId(),
                    memory.getCoupleId(),
                    photo.getId(),
                    photo.getStoredPhotoPath(),
                    statusText(result),
                    result.getTags() == null ? 0 : result.getTags().size()
            );
            return result;
        } catch (RuntimeException e) {
            log.error(
                    "Memory photo AI analysis failed unexpectedly. memoryId={}, coupleId={}, photoId={}, objectKey={}, errorType={}, message={}",
                    memory.getId(),
                    memory.getCoupleId(),
                    photo.getId(),
                    photo.getStoredPhotoPath(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            return MemoryAiAnalysisResult.failed();
        }
    }

    private void applyPhotoAiAnalysis(MemoryPhoto photo, MemoryAiAnalysisResult analysisResult) {
        if (photo == null) {
            return;
        }

        photo.replaceAiTags(toPhotoAiTags(analysisResult));
        if (analysisResult != null && analysisResult.isCompleted()) {
            photo.markAiAnalyzed();
        } else if (analysisResult != null && analysisResult.isFailed()) {
            photo.markAiAnalysisFailed();
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

    private List<MemoryPhotoAiTag> toPhotoAiTags(MemoryAiAnalysisResult analysisResult) {
        if (analysisResult == null || analysisResult.getTags() == null) {
            return List.of();
        }

        return analysisResult.getTags().stream()
                .filter(tag -> tag != null && StringUtils.hasText(tag.tagName()))
                .map(tag -> MemoryPhotoAiTag.ai(tag.tagName().trim(), tag.confidence()))
                .toList();
    }

    private String statusText(MemoryAiAnalysisResult result) {
        if (result == null) {
            return "PENDING";
        }
        if (result.isCompleted()) {
            return "COMPLETED";
        }
        if (result.isFailed()) {
            return "FAILED";
        }
        return "PENDING";
    }
}
