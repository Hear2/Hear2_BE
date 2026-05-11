package com.hear2.memory.service;

public record MemoryPhotoStorageResult(
        String storedPhotoPath,
        String originalFileName,
        String contentType,
        long size
) {
}
