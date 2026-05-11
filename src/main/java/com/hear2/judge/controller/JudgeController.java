package com.hear2.judge.controller;

import com.hear2.global.error.ApiErrorResponse;
import com.hear2.judge.dto.ConflictPatternResponse;
import com.hear2.judge.dto.JudgeHistoryResponse;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Deprecated
    @Operation(summary = "AI 판사 판결 이력 조회(호환용)", description = "기존 경로 호환용입니다. 실제 조회 권한은 로그인된 사용자의 커플 기준으로 검증합니다.")
    @ApiResponse(responseCode = "200", description = "판결 이력 조회 성공")
    @GetMapping("/couples/{coupleId}/histories")
    public List<JudgeHistoryResponse> getHistories(
            Authentication authentication,
            @Parameter(description = "커플 ID", example = "1", required = true)
            @PathVariable Long coupleId
    ) {
        return judgeService.getHistories(currentUserId(authentication), coupleId);
    }

    @Deprecated
    @Operation(summary = "반복 갈등 패턴 조회(호환용)", description = "기존 경로 호환용입니다. 실제 조회 권한은 로그인된 사용자의 커플 기준으로 검증합니다.")
    @ApiResponse(responseCode = "200", description = "반복 갈등 패턴 조회 성공")
    @GetMapping("/couples/{coupleId}/patterns")
    public List<ConflictPatternResponse> getPatterns(
            Authentication authentication,
            @Parameter(description = "커플 ID", example = "1", required = true)
            @PathVariable Long coupleId
    ) {
        return judgeService.getPatterns(currentUserId(authentication), coupleId);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        return (Long) authentication.getPrincipal();
    }
}
