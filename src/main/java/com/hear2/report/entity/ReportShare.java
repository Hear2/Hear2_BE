package com.hear2.report.entity;

import com.hear2.report.support.ReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_share", indexes = {
        @Index(name = "idx_report_share_code", columnList = "shareCode", unique = true),
        @Index(name = "idx_report_share_couple_created", columnList = "coupleId, createdAt")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReportShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String shareCode;

    @Column(nullable = false)
    private Long coupleId;

    private Long requesterId;

    private Long receiverId;

    @Column(length = 60)
    private String partnerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType reportType;

    @Column(nullable = false)
    private LocalDate anchorDate;

    @Column(nullable = false)
    private LocalDate periodStartDate;

    @Column(nullable = false)
    private LocalDate periodEndDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
