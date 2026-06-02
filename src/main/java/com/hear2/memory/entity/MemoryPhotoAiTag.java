package com.hear2.memory.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(indexes = {
        @Index(name = "idx_memory_photo_ai_tag_photo", columnList = "memory_photo_id"),
        @Index(name = "idx_memory_photo_ai_tag_name", columnList = "tagName")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPhotoAiTag {

    private static final int TAG_NAME_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "memory_photo_id", nullable = false)
    private MemoryPhoto photo;

    @Column(nullable = false, length = TAG_NAME_MAX_LENGTH)
    private String tagName;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    private MemoryPhotoAiTag(String tagName, BigDecimal confidence) {
        this.tagName = tagName;
        this.confidence = confidence;
    }

    public static MemoryPhotoAiTag ai(String tagName, BigDecimal confidence) {
        return new MemoryPhotoAiTag(tagName, confidence);
    }

    public void assignPhoto(MemoryPhoto photo) {
        this.photo = photo;
    }
}
