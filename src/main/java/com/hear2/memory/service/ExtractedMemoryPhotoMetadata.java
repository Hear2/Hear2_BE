package com.hear2.memory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExtractedMemoryPhotoMetadata(
        LocalDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
