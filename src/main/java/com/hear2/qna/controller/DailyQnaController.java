package com.hear2.qna.controller;

import com.hear2.qna.dto.DailyAnswerRequest;
import com.hear2.qna.dto.DailyAnswerResponse;
import com.hear2.qna.dto.DailyQuestionDetailResponse;
import com.hear2.qna.dto.DailyQuestionHistoryResponse;
import com.hear2.qna.dto.TodayQuestionResponse;
import com.hear2.qna.service.DailyQnaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Daily Q&A")
@RestController
@RequestMapping("/api/v1/qna")
@RequiredArgsConstructor
public class DailyQnaController {

    private final DailyQnaService dailyQnaService;

    @Operation(summary = "오늘의 질문 조회")
    @GetMapping("/today")
    public TodayQuestionResponse today(Authentication authentication) {
        return dailyQnaService.getToday(currentUserId(authentication));
    }

    @Operation(summary = "오늘의 질문 답변 저장")
    @PostMapping("/today/answer")
    public DailyAnswerResponse answerToday(
            Authentication authentication,
            @Valid @RequestBody DailyAnswerRequest request
    ) {
        return dailyQnaService.answerToday(currentUserId(authentication), request.answer());
    }

    @Operation(summary = "질문 기록 목록 조회")
    @GetMapping("/history")
    public DailyQuestionHistoryResponse history(Authentication authentication) {
        return dailyQnaService.getHistory(currentUserId(authentication));
    }

    @Operation(summary = "특정 질문 상세 조회")
    @GetMapping("/{questionId}")
    public DailyQuestionDetailResponse detail(
            Authentication authentication,
            @PathVariable Long questionId
    ) {
        return dailyQnaService.getDetail(currentUserId(authentication), questionId);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
