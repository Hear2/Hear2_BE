package com.hear2.memory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(indexes = {
        @Index(name = "idx_memory_photo_memory_order", columnList = "memory_id, sortOrder")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemoryPhoto {

    private static final int STORED_PHOTO_PATH_MAX_LENGTH = 2048;
    private static final int ORIGINAL_FILE_NAME_MAX_LENGTH = 255;
    private static final int CONTENT_TYPE_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "memory_id", nullable = false)
    private Memory memory;

    @Column(nullable = false, length = STORED_PHOTO_PATH_MAX_LENGTH)
    private String storedPhotoPath;

    @Column(length = ORIGINAL_FILE_NAME_MAX_LENGTH)
    private String originalFileName;

    @Column(nullable = false, length = CONTENT_TYPE_MAX_LENGTH)
    private String photoContentType;

    @Column(nullable = false)
    private Long photoSize;

    @Column(nullable = false)
    private int sortOrder;

    public static MemoryPhoto create(
            String storedPhotoPath,
            String originalFileName,
            String photoContentType,
            Long photoSize,
            int sortOrder
    ) {
        return MemoryPhoto.builder()
                .storedPhotoPath(storedPhotoPath)
                .originalFileName(originalFileName)
                .photoContentType(photoContentType)
                .photoSize(photoSize)
                .sortOrder(sortOrder)
                .build();
    }

    public void assignMemory(Memory memory) {
        this.memory = memory;
    }
}
