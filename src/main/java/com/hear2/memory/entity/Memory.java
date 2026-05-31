package com.hear2.memory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_memory_couple_date", columnList = "coupleId, memoryDate"),
        @Index(name = "idx_memory_couple_created", columnList = "coupleId, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Memory {

    private static final int STORED_PHOTO_PATH_MAX_LENGTH = 2048;
    private static final int ORIGINAL_FILE_NAME_MAX_LENGTH = 255;
    private static final int CONTENT_TYPE_MAX_LENGTH = 100;
    private static final int MEMO_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(nullable = false, length = STORED_PHOTO_PATH_MAX_LENGTH)
    private String storedPhotoPath;

    @Column(length = ORIGINAL_FILE_NAME_MAX_LENGTH)
    private String originalFileName;

    @Column(nullable = false, length = CONTENT_TYPE_MAX_LENGTH)
    private String photoContentType;

    @Column(nullable = false)
    private Long photoSize;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    @Column(nullable = false)
    private LocalDate memoryDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemoryAiAnalysisStatus aiAnalysisStatus = MemoryAiAnalysisStatus.PENDING;

    @OneToOne(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MemoryPhotoMetadata photoMetadata;

    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoryAiTag> aiTags = new ArrayList<>();

    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<MemoryPhoto> photos = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void attachPhotoMetadata(MemoryPhotoMetadata photoMetadata) {
        if (photoMetadata == null) {
            return;
        }

        photoMetadata.assignMemory(this);
        this.photoMetadata = photoMetadata;
    }

    public void replaceAiTags(List<MemoryAiTag> aiTags) {
        replaceTags(MemoryTagSource.AI, aiTags);
    }

    public void replaceUserTags(List<MemoryAiTag> userTags) {
        replaceTags(MemoryTagSource.USER, userTags);
    }

    public void markAiAnalyzed() {
        this.aiAnalysisStatus = MemoryAiAnalysisStatus.COMPLETED;
    }

    public void markAiAnalysisFailed() {
        this.aiAnalysisStatus = MemoryAiAnalysisStatus.FAILED;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void replacePhotos(List<MemoryPhoto> photos) {
        this.photos.clear();
        if (photos == null) {
            return;
        }

        photos.forEach(this::addPhoto);
    }

    private void addPhoto(MemoryPhoto photo) {
        if (photo == null) {
            return;
        }

        photo.assignMemory(this);
        this.photos.add(photo);
    }

    private void addAiTag(MemoryAiTag aiTag) {
        if (aiTag == null) {
            return;
        }

        aiTag.assignMemory(this);
        this.aiTags.add(aiTag);
    }

    private void replaceTags(MemoryTagSource source, List<MemoryAiTag> tags) {
        this.aiTags.removeIf(tag -> tag.getSource() == source);
        if (tags == null) {
            return;
        }

        tags.forEach(this::addAiTag);
    }
}
