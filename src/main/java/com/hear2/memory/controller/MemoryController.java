package com.hear2.memory.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.memory.dto.MemoryCreateRequest;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.dto.MemoryUpdateRequest;
import com.hear2.memory.service.MemoryPhotoContent;
import com.hear2.memory.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemoryResponse> createMemory(
            Authentication authentication,
            @RequestPart("photo") MultipartFile photo,
            @Valid @RequestPart("request") MemoryCreateRequest request
    ) {
        return ApiResponse.success(memoryService.createMemory(photo, request, currentUserId(authentication)));
    }

    @GetMapping("/couples/{coupleId}")
    public ApiResponse<List<MemoryResponse>> getAlbum(
            Authentication authentication,
            @PathVariable Long coupleId
    ) {
        return ApiResponse.success(memoryService.getAlbum(coupleId, currentUserId(authentication)));
    }

    @GetMapping("/couples/{coupleId}/dates/{memoryDate}")
    public ApiResponse<List<MemoryResponse>> getMemoriesByDate(
            Authentication authentication,
            @PathVariable Long coupleId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PathVariable LocalDate memoryDate
    ) {
        return ApiResponse.success(memoryService.getMemoriesByDate(coupleId, memoryDate, currentUserId(authentication)));
    }

    @GetMapping("/couples/{coupleId}/items/{memoryId}")
    public ApiResponse<MemoryResponse> getMemory(
            Authentication authentication,
            @PathVariable Long coupleId,
            @PathVariable Long memoryId
    ) {
        return ApiResponse.success(memoryService.getMemory(coupleId, memoryId, currentUserId(authentication)));
    }

    @GetMapping("/couples/{coupleId}/items/{memoryId}/photo")
    public ResponseEntity<byte[]> getMemoryPhoto(
            Authentication authentication,
            @PathVariable Long coupleId,
            @PathVariable Long memoryId
    ) {
        MemoryPhotoContent photo = memoryService.getMemoryPhoto(coupleId, memoryId, currentUserId(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(photo.content());
    }

    @PatchMapping("/couples/{coupleId}/items/{memoryId}")
    public ApiResponse<MemoryResponse> updateMemory(
            Authentication authentication,
            @PathVariable Long coupleId,
            @PathVariable Long memoryId,
            @Valid @RequestBody MemoryUpdateRequest request
    ) {
        return ApiResponse.success(memoryService.updateMemory(coupleId, memoryId, request, currentUserId(authentication)));
    }

    @DeleteMapping("/couples/{coupleId}/items/{memoryId}")
    public ApiResponse<Void> deleteMemory(
            Authentication authentication,
            @PathVariable Long coupleId,
            @PathVariable Long memoryId
    ) {
        memoryService.deleteMemory(coupleId, memoryId, currentUserId(authentication));
        return ApiResponse.success(null, "memory deleted");
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }
}
