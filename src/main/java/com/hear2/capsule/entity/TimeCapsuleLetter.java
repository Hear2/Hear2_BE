package com.hear2.capsule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TimeCapsuleLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long capsuleId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 5000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime writtenAt;
}
