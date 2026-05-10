package com.hear2.memory.service;

public record MemoryPhotoFile(
        byte[] content,
        String originalFileName,
        String contentType
) {
    public long size() {
        return content == null ? 0 : content.length;
    }
}
