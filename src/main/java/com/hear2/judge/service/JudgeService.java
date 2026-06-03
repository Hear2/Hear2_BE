package com.hear2.judge.service;

import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.chat.service.ChatParticipantResolver;
import com.hear2.emotion.entity.EmotionAnalysis;
import com.hear2.emotion.enums.RiskLevel;
import com.hear2.emotion.repository.EmotionAnalysisRepository;
import com.hear2.judge.client.JudgeAnalysisClient;
import com.hear2.judge.dto.ConflictPatternResponse;
import com.hear2.judge.dto.JudgeFeedbackSummary;
import com.hear2.judge.dto.JudgeFastApiMessage;
import com.hear2.judge.dto.JudgeFastApiRequest;
import com.hear2.judge.dto.JudgeHistoryResponse;
import com.hear2.judge.dto.JudgeRequest;
import com.hear2.judge.dto.JudgeResponse;
import com.hear2.judge.entity.JudgeHistory;
import com.hear2.judge.enums.ConflictType;
import com.hear2.judge.enums.JudgeTone;
import com.hear2.judge.repository.JudgeHistoryRepository;
import com.hear2.judge.support.JudgeParticipantFormatter;
import com.hear2.judge.support.JudgeTriggerPolicy;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeService {

    private final ChatMessageRepository chatMessageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final JudgeHistoryRepository judgeHistoryRepository;
    private final JudgeAnalysisClient judgeAnalysisClient;
    private final ChatParticipantResolver chatParticipantResolver;
    private final JudgeFeedbackService judgeFeedbackService;
    private final UserRepository userRepository;

    @Transactional
    public JudgeResponse judge(Long currentUserId, JudgeRequest request) {
        validateJudgeRequest(request);
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);

        ChatMessage triggerMessage = chatMessageRepository.findById(request.getTriggerMessageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "trigger message not found"));
        if (!context.coupleId().equals(triggerMessage.getCoupleId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "trigger message does not belong to your couple");
        }

        EmotionAnalysis triggerEmotion = findTriggerEmotionAnalysis(triggerMessage.getId());
        RiskLevel triggerRiskLevel = triggerEmotion.getRiskLevel();
        if (!JudgeTriggerPolicy.isJudgeAvailable(triggerRiskLevel, triggerEmotion.getNegativeScore())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI judge is available only for high negative score or WARNING/DANGER risk messages");
        }

        List<ChatMessage> recentMessages = findRecentTextMessages(context.coupleId());
        if (recentMessages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no text messages to judge");
        }

        Map<Long, EmotionAnalysis> emotionsByMessageId = findEmotionsByMessageId(recentMessages);
        List<JudgeFastApiMessage> fastApiMessages = recentMessages.stream()
                .map(message -> toFastApiMessage(message, emotionsByMessageId.get(message.getId())))
                .toList();
        ParticipantNames participantNames = resolveParticipantNames(context);

        JudgeFastApiRequest fastApiRequest = JudgeFastApiRequest.builder()
                .coupleId(context.coupleId())
                .triggerMessageId(request.getTriggerMessageId())
                .requestedByUserId(context.senderId())
                .partnerUserId(context.receiverId())
                .requestedByName(participantNames.sharedUserName())
                .partnerName(participantNames.sharedPartnerName())
                .messages(fastApiMessages)
                .build();

        JudgeResponse response = judgeAnalysisClient.requestJudgement(fastApiRequest);
        ConflictType conflictType = response.resolvedConflictType();
        JudgeTone judgeTone = response.resolvedJudgeTone();

        JudgeHistory savedHistory = judgeHistoryRepository.save(
                JudgeHistory.builder()
                        .coupleId(context.coupleId())
                        .triggerMessageId(request.getTriggerMessageId())
                        .requestedByUserId(context.senderId())
                        .partnerUserId(context.receiverId())
                        .triggerRiskLevel(triggerRiskLevel)
                        .summaryA(JudgeParticipantFormatter.normalizeRequesterSummary(
                                response.getSummaryA(),
                                participantNames.sharedUserName()
                        ))
                        .summaryB(JudgeParticipantFormatter.normalizePartnerSummary(
                                response.getSummaryB(),
                                participantNames.sharedPartnerName()
                        ))
                        .judgement(response.getJudgement())
                        .solution(response.getSolution())
                        .reconciliationMessage(response.getRequestedReconciliationMessage())
                        .requestedReconciliationMessage(response.getRequestedReconciliationMessage())
                        .partnerReconciliationMessage(response.getPartnerReconciliationMessage())
                        .conflictType(conflictType)
                        .judgeTone(judgeTone)
                        .build()
        );
        Long sameConflictCount = judgeHistoryRepository.countByCoupleIdAndConflictType(
                context.coupleId(),
                conflictType
        );

        return JudgeResponse.from(
                savedHistory,
                sameConflictCount,
                currentUserId,
                participantNames.userName(),
                participantNames.partnerName(),
                participantNames.sharedUserName(),
                participantNames.sharedPartnerName()
        );
    }

    @Transactional(readOnly = true)
    public List<JudgeHistoryResponse> getHistories(Long currentUserId) {
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);
        return findHistories(context, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ConflictPatternResponse> getPatterns(Long currentUserId) {
        ChatParticipantResolver.ChatRoomContext context = chatParticipantResolver.resolve(currentUserId);
        return findPatterns(context.coupleId());
    }

    private List<JudgeHistoryResponse> findHistories(ChatParticipantResolver.ChatRoomContext context, Long currentUserId) {
        ParticipantNames participantNames = resolveParticipantNames(context);
        List<JudgeHistory> histories = judgeHistoryRepository.findByCoupleIdOrderByCreatedAtDesc(context.coupleId());
        Map<Long, JudgeFeedbackSummary> feedbackByHistoryId = judgeFeedbackService.findFeedbackByJudgeHistoryIdsAndUserId(
                histories.stream().map(JudgeHistory::getId).toList(),
                currentUserId
        );
        Map<Long, User> usersById = loadUsersById(histories, context);

        return histories
                .stream()
                .map(history -> {
                    SharedParticipantNames sharedParticipantNames = resolveSharedParticipantNames(history, usersById);
                    return JudgeHistoryResponse.from(
                            history,
                            feedbackByHistoryId.getOrDefault(history.getId(), JudgeFeedbackSummary.empty()),
                            currentUserId,
                            participantNames.userName(),
                            participantNames.partnerName(),
                            sharedParticipantNames.summaryAName(),
                            sharedParticipantNames.summaryBName()
                    );
                })
                .toList();
    }

    private ParticipantNames resolveParticipantNames(ChatParticipantResolver.ChatRoomContext context) {
        Map<Long, User> usersById = loadUsersByIds(List.of(context.senderId(), context.receiverId()));

        String userName = resolveDisplayName(usersById.get(context.senderId()), "나");
        String partnerName = resolveDisplayName(usersById.get(context.receiverId()), "상대방");
        String sharedUserName = resolveDisplayName(usersById.get(context.senderId()), "한 사람");
        String sharedPartnerName = resolveDisplayName(usersById.get(context.receiverId()), "다른 한 사람");
        return new ParticipantNames(userName, partnerName, sharedUserName, sharedPartnerName);
    }

    private Map<Long, User> loadUsersById(
            List<JudgeHistory> histories,
            ChatParticipantResolver.ChatRoomContext context
    ) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(context.senderId());
        userIds.add(context.receiverId());
        histories.stream()
                .map(JudgeHistory::getRequestedByUserId)
                .filter(id -> id != null)
                .forEach(userIds::add);
        histories.stream()
                .map(JudgeHistory::getPartnerUserId)
                .filter(id -> id != null)
                .forEach(userIds::add);
        return loadUsersByIds(userIds);
    }

    private Map<Long, User> loadUsersByIds(Iterable<Long> userIds) {
        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    private SharedParticipantNames resolveSharedParticipantNames(JudgeHistory history, Map<Long, User> usersById) {
        String summaryAName = resolveDisplayName(usersById.get(history.getRequestedByUserId()), "한 사람");
        String summaryBName = resolveDisplayName(usersById.get(history.getPartnerUserId()), "다른 한 사람");
        return new SharedParticipantNames(summaryAName, summaryBName);
    }

    private String resolveDisplayName(User user, String fallback) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        return fallback;
    }

    private List<ConflictPatternResponse> findPatterns(Long coupleId) {
        Map<ConflictType, List<JudgeHistory>> historiesByType = judgeHistoryRepository
                .findByCoupleIdOrderByCreatedAtDesc(coupleId)
                .stream()
                .collect(Collectors.groupingBy(history -> history.getConflictType() == null
                        ? ConflictType.OTHER
                        : history.getConflictType()));

        return historiesByType.entrySet()
                .stream()
                .map(entry -> ConflictPatternResponse.builder()
                        .conflictType(entry.getKey())
                        .count((long) entry.getValue().size())
                        .latestCreatedAt(entry.getValue().stream()
                                .map(JudgeHistory::getCreatedAt)
                                .max(Comparator.naturalOrder())
                                .orElse(null))
                        .build())
                .sorted(Comparator
                        .comparing(ConflictPatternResponse::getCount).reversed()
                        .thenComparing(ConflictPatternResponse::getLatestCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void validateJudgeRequest(JudgeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "judge request is required");
        }
        if (request.getTriggerMessageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "triggerMessageId is required");
        }
    }

    private EmotionAnalysis findTriggerEmotionAnalysis(Long triggerMessageId) {
        return emotionAnalysisRepository.findByMessageId(triggerMessageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "trigger message has no risk analysis"));
    }

    public static boolean isJudgeAvailable(RiskLevel riskLevel) {
        return JudgeTriggerPolicy.isJudgeAvailable(riskLevel, null);
    }

    private List<ChatMessage> findRecentTextMessages(Long coupleId) {
        List<ChatMessage> recentMessages = new ArrayList<>(
                chatMessageRepository.findTop20ByCoupleIdOrderByCreatedAtDesc(coupleId)
        );
        recentMessages.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        return recentMessages.stream()
                .filter(message -> message.getMessageType() == MessageType.TEXT)
                .filter(message -> StringUtils.hasText(message.getContent()))
                .toList();
    }

    private Map<Long, EmotionAnalysis> findEmotionsByMessageId(List<ChatMessage> messages) {
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        return emotionAnalysisRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.toMap(
                        analysis -> analysis.getMessage().getId(),
                        Function.identity()
                ));
    }

    private JudgeFastApiMessage toFastApiMessage(ChatMessage message, EmotionAnalysis emotion) {
        return JudgeFastApiMessage.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .emotionType(emotion == null ? null : emotion.getEmotionType())
                .emotionScore(emotion == null ? null : emotion.getEmotionScore())
                .negativeScore(emotion == null ? null : emotion.getNegativeScore())
                .emotionEmoji(emotion == null ? null : emotion.getEmotionEmoji())
                .riskLevel(emotion == null ? null : emotion.getRiskLevel())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private record ParticipantNames(
            String userName,
            String partnerName,
            String sharedUserName,
            String sharedPartnerName
    ) {
    }

    private record SharedParticipantNames(String summaryAName, String summaryBName) {
    }
}
