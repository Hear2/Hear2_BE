package com.hear2.memory.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.memory.dto.MemoryCreateRequest;
import com.hear2.memory.dto.MemoryResponse;
import com.hear2.memory.dto.MemoryUpdateRequest;
import com.hear2.memory.service.MemoryPhotoContent;
import com.hear2.memory.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
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
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
@Tag(name = "Memory Album", description = "추억 앨범 조회, 수정, 삭제 API. 신규 사진 업로드는 Memory Quick + Media presigned URL 흐름을 우선 사용합니다.")
public class MemoryController {

    private final MemoryService memoryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(
            summary = "[호환용] 추억 사진 직접 업로드",
            description = """
                    기존 테스트/호환용 multipart 직접 업로드 API입니다.
                    신규 프론트 구현에서는 이 API를 메인으로 쓰지 말고,
                    1) POST /api/v1/media/presigned-url 로 uploadUrl/objectKey 발급
                    2) 프론트가 uploadUrl에 직접 PUT 업로드
                    3) POST /api/v1/memory/quick 에 objectKey 전달
                    흐름을 사용하세요.

                    이 API는 로그인된 사용자의 커플 정보를 기준으로 사진을 저장합니다.
                    커플 ID와 업로더 ID는 토큰에서 자동 적용되며, 촬영 시간/좌표는 요청값 또는 사진 EXIF에서 가져옵니다.
                    """
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemoryResponse> createMemory(
            Authentication authentication,
            @Parameter(description = "업로드할 사진 파일", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestPart("photo") MultipartFile photo,
            @Parameter(
                    description = "추억 메타데이터 JSON 문자열. 사진 EXIF만 사용할 경우 {} 를 보내면 됩니다.",
                    required = true,
                    schema = @Schema(
                            type = "string",
                            example = "{\"memo\":\"명지대에서 찍은 사진\",\"takenAt\":\"2026-05-11T10:30:00\",\"latitude\":37.2221,\"longitude\":127.1875,\"locationName\":\"명지대학교 자연캠퍼스\",\"userTags\":[\"우리둘이\",\"특별한날\"]}"
                    )
            )
            @RequestPart("request") String request
    ) {
        return ApiResponse.success(memoryService.createMemory(photo, parseCreateRequest(request), currentUserId(authentication)));
    }

    @Operation(summary = "내 추억 앨범 조회", description = "로그인된 사용자의 커플 기준으로 추억 앨범을 최신순으로 조회합니다.")
    @GetMapping
    public ApiResponse<List<MemoryResponse>> getAlbum(Authentication authentication) {
        return ApiResponse.success(memoryService.getAlbum(currentUserId(authentication)));
    }

    @Operation(summary = "날짜별 내 추억 조회", description = "로그인된 사용자의 커플 기준으로 특정 날짜의 추억을 조회합니다.")
    @GetMapping("/dates/{memoryDate}")
    public ApiResponse<List<MemoryResponse>> getMemoriesByDate(
            Authentication authentication,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PathVariable LocalDate memoryDate
    ) {
        return ApiResponse.success(memoryService.getMemoriesByDate(memoryDate, currentUserId(authentication)));
    }

    @Operation(summary = "추억 상세 조회", description = "로그인된 사용자의 커플 앨범 안에서 memoryId에 해당하는 추억을 조회합니다.")
    @GetMapping("/items/{memoryId}")
    public ApiResponse<MemoryResponse> getMemory(
            Authentication authentication,
            @PathVariable Long memoryId
    ) {
        return ApiResponse.success(memoryService.getMemory(memoryId, currentUserId(authentication)));
    }

    @Operation(summary = "추억 사진 조회", description = "로그인된 사용자의 커플 앨범 안에서 memoryId에 해당하는 원본 사진 바이트를 조회합니다.")
    @GetMapping("/items/{memoryId}/photo")
    public ResponseEntity<byte[]> getMemoryPhoto(
            Authentication authentication,
            @PathVariable Long memoryId
    ) {
        MemoryPhotoContent photo = memoryService.getMemoryPhoto(memoryId, currentUserId(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(photo.content());
    }

    @Operation(summary = "추억 메모 수정", description = "로그인된 사용자의 커플 앨범 안에서 memoryId에 해당하는 추억 메모를 수정합니다.")
    @PatchMapping("/items/{memoryId}")
    public ApiResponse<MemoryResponse> updateMemory(
            Authentication authentication,
            @PathVariable Long memoryId,
            @Valid @RequestBody MemoryUpdateRequest request
    ) {
        return ApiResponse.success(memoryService.updateMemory(memoryId, request, currentUserId(authentication)));
    }

    @Operation(summary = "추억 삭제", description = "로그인된 사용자의 커플 앨범 안에서 memoryId에 해당하는 추억과 사진을 삭제합니다.")
    @DeleteMapping("/items/{memoryId}")
    public ApiResponse<Void> deleteMemory(
            Authentication authentication,
            @PathVariable Long memoryId
    ) {
        memoryService.deleteMemory(memoryId, currentUserId(authentication));
        return ApiResponse.success(null, "memory deleted");
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }

        return userId;
    }

    private MemoryCreateRequest parseCreateRequest(String request) {
        String requestJson = StringUtils.hasText(request) ? request : "{}";

        try {
            MemoryCreateRequest createRequest = objectMapper.readValue(requestJson, MemoryCreateRequest.class);
            validateCreateRequest(createRequest);
            return createRequest;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request must be valid JSON");
        }
    }

    private void validateCreateRequest(MemoryCreateRequest request) {
        Set<ConstraintViolation<MemoryCreateRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
