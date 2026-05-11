package com.hear2.emotion.controller;

import com.hear2.emotion.dto.EmotionAnalysisRequest;
import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.service.EmotionAnalysisService;
import com.hear2.global.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/emotions")
@RequiredArgsConstructor
@Tag(name = "Emotion Analysis", description = "메시지 감정 분석과 리스크 판정 API")
public class EmotionController {

    private final EmotionAnalysisService emotionAnalysisService;

    @Operation(
            summary = "메시지 감정 분석",
            description = "로그인된 사용자의 커플에 속한 메시지 ID를 기준으로 감정 분석 결과를 반환합니다. 저장된 분석이 있으면 재사용하고, 없으면 FastAPI 감정 분석 서버로 전달해 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "감정 분석 성공",
                    content = @Content(schema = @Schema(implementation = EmotionAnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "분석할 messageId 누락",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/analyze")
    public EmotionAnalysisResponse analyze(
            Authentication authentication,
            @RequestBody EmotionAnalysisRequest request
    ) {
        return emotionAnalysisService.analyzeMessageForUser(currentUserId(authentication), request.getMessageId());
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
