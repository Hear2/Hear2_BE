package com.hear2.judge.service;

import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.judge.dto.JudgeFeedbackRequest;
import com.hear2.judge.dto.JudgeFeedbackResponse;
import com.hear2.judge.dto.JudgeFeedbackSummary;
import com.hear2.judge.entity.JudgeFeedback;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.repository.JudgeFeedbackRepository;
import com.hear2.judge.repository.JudgeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeFeedbackService {

    private final ChatParticipantResolver chatParticipantResolver;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final JudgeFeedbackRepository judgeFeedbackRepository;

    @Transactional
    public JudgeFeedbackResponse saveFeedback(Long currentUserId, Long judgeHistoryId, JudgeFeedbackRequest request) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }
        if (judgeHistoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "judgeHistoryId is required");
        }
        if (request == null || request.getSatisfied() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "satisfied is required");
        }

        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);
        JudgeHistory judgeHistory = judgeHistoryRepository.findById(judgeHistoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "judge history not found"));
        if (!context.coupleId().equals(judgeHistory.getCoupleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "judge history does not belong to your couple");
        }

        String normalizedFeedbackText = normalizeFeedbackText(request.getFeedbackText());
        JudgeFeedback feedback = judgeFeedbackRepository.findByJudgeHistoryIdAndUserId(judgeHistoryId, currentUserId)
                .map(existing -> {
                    existing.update(request.getSatisfied(), normalizedFeedbackText);
                    return existing;
                })
                .orElseGet(() -> JudgeFeedback.builder()
                        .judgeHistory(judgeHistory)
                        .userId(currentUserId)
                        .satisfied(request.getSatisfied())
                        .feedbackText(normalizedFeedbackText)
                        .build());

        JudgeFeedback saved = judgeFeedbackRepository.save(feedback);
        return JudgeFeedbackResponse.builder()
                .judgeHistoryId(saved.getJudgeHistory().getId())
                .satisfied(saved.getSatisfied())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<Long, JudgeFeedbackSummary> findFeedbackByJudgeHistoryIdsAndUserId(List<Long> judgeHistoryIds, Long userId) {
        if (userId == null || judgeHistoryIds == null || judgeHistoryIds.isEmpty()) {
            return Map.of();
        }

        return judgeFeedbackRepository.findByJudgeHistoryIdInAndUserId(judgeHistoryIds, userId).stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getJudgeHistory().getId(),
                        feedback -> JudgeFeedbackSummary.builder()
                                .feedbackSubmitted(true)
                                .satisfied(feedback.getSatisfied())
                                .feedbackText(feedback.getFeedbackText())
                                .build()
                ));
    }

    private String normalizeFeedbackText(String feedbackText) {
        if (!StringUtils.hasText(feedbackText)) {
            return null;
        }
        return feedbackText.trim();
    }
}
