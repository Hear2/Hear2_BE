package com.hear2.memory.service;

public record MemoryPhotoContent(
        byte[] content,
        String contentType
) {
}
