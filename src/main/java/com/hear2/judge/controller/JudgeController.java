package com.hear2.judge.controller;

import com.hear2.global.error.ApiErrorResponse;
import com.hear2.judge.dto.ConflictPatternResponse;
import com.hear2.judge.dto.JudgeFeedbackRequest;
import com.hear2.judge.dto.JudgeFeedbackResponse;
import com.hear2.judge.dto.JudgeHistoryResponse;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.service.JudgeFeedbackService;
import com.hear2.judge.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/judge")
@Tag(name = "AI Judge", description = "갈등 상황 AI 판결 및 판결 이력 API")
public class JudgeController {

    private final JudgeService judgeService;
    private final JudgeFeedbackService judgeFeedbackService;

    @Operation(
            summary = "AI 판사 호출",
            description = "부정 감정 점수가 높거나 WARNING/DANGER 리스크인 메시지를 트리거로 최근 대화를 GPT-4o에 전달하고, 채팅창 카드로 렌더링할 판결 응답을 반환하며 이력을 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 판결 성공",
                    content = @Content(schema = @Schema(implementation = JudgeResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 판결 호출 불가 리스크 단계",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI 판결 서버 호출 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<JudgeResponse> judge(
            Authentication authentication,
            @RequestBody JudgeRequest request
    ) {
        return ResponseEntity.ok(judgeService.judge(currentUserId(authentication), request));
    }

    @Operation(summary = "내 AI 판사 판결 이력 조회", description = "로그인된 사용자의 커플 기준으로 저장된 AI 판결 이력을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "판결 이력 조회 성공")
    @GetMapping("/histories")
    public List<JudgeHistoryResponse> getHistories(
            Authentication authentication
    ) {
        return judgeService.getHistories(currentUserId(authentication));
    }

    @Operation(summary = "내 반복 갈등 패턴 조회", description = "로그인된 사용자의 커플 기준으로 저장된 AI 판결 이력을 갈등 유형별로 집계합니다.")
    @ApiResponse(responseCode = "200", description = "반복 갈등 패턴 조회 성공")
    @GetMapping("/patterns")
    public List<ConflictPatternResponse> getPatterns(
            Authentication authentication
    ) {
        return judgeService.getPatterns(currentUserId(authentication));
    }

    @Operation(
            summary = "AI 판결문 피드백 저장",
            description = "현재 로그인 사용자가 자신의 커플 판결문에 대해 만족 여부와 선택 의견을 저장합니다. 같은 판결문에 다시 제출하면 기존 피드백을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "피드백 저장 성공",
                    content = @Content(schema = @Schema(implementation = JudgeFeedbackResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "다른 커플의 판결문",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "판결문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{judgeHistoryId}/feedback")
    public JudgeFeedbackResponse saveFeedback(
            Authentication authentication,
            @PathVariable Long judgeHistoryId,
            @Valid @RequestBody JudgeFeedbackRequest request
    ) {
        return judgeFeedbackService.saveFeedback(currentUserId(authentication), judgeHistoryId, request);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
