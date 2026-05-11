package com.hear2.memory.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(indexes = {
        @Index(name = "idx_memory_ai_tag_memory", columnList = "memory_id"),
        @Index(name = "idx_memory_ai_tag_name", columnList = "tagName")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryAiTag {

    private static final int TAG_NAME_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_id", nullable = false)
    private Memory memory;

    @Column(nullable = false, length = TAG_NAME_MAX_LENGTH)
    private String tagName;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemoryTagSource source;

    private MemoryAiTag(String tagName, BigDecimal confidence, MemoryTagSource source) {
        this.tagName = tagName;
        this.confidence = confidence;
        this.source = source;
    }

    public static MemoryAiTag ai(String tagName, BigDecimal confidence) {
        return new MemoryAiTag(tagName, confidence, MemoryTagSource.AI);
    }

    public void assignMemory(Memory memory) {
        this.memory = memory;
    }
}
