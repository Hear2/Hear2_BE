package com.hear2.capsule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_time_capsule_media_capsule", columnList = "capsuleId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TimeCapsuleMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long capsuleId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TimeCapsuleMediaType mediaType;

    @Column(nullable = false, length = 2048)
    private String objectKey;

    @Column(length = 255)
    private String caption;

    private LocalDateTime capturedAt;

    @Column(nullable = false)
    private int orderIndex;
}
