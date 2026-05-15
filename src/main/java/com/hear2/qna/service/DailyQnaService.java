package com.hear2.qna.service;

import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.qna.dto.DailyAnswerResponse;
import com.hear2.qna.dto.DailyQuestionDetailResponse;
import com.hear2.qna.dto.DailyQuestionHistoryItemResponse;
import com.hear2.qna.dto.DailyQuestionHistoryResponse;
import com.hear2.qna.dto.DailyQuestionStatus;
import com.hear2.qna.dto.TodayQuestionResponse;
import com.hear2.qna.entity.DailyAnswer;
import com.hear2.qna.entity.DailyQuestion;
import com.hear2.qna.entity.DailyQuestionTemplate;
import com.hear2.qna.repository.DailyAnswerRepository;
import com.hear2.qna.repository.DailyQuestionRepository;
import com.hear2.qna.repository.DailyQuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyQnaService {

    private static final int TEMPLATE_CYCLE_DAYS = 100;
    private static final int ANSWER_MAX_LENGTH = 500;
    private static final int ANSWER_REWARD_POINTS = 50;

    private final DailyQuestionTemplateRepository dailyQuestionTemplateRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final DailyAnswerRepository dailyAnswerRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public TodayQuestionResponse getToday(Long userId) {
        CoupleContext context = getCoupleContext(userId);
        LocalDate today = LocalDate.now();
        DailyQuestionAssignment assignment = getOrCreateActiveQuestion(context.couple().getCoupleId(), today);
        DailyQuestion dailyQuestion = assignment.dailyQuestion();
        DailyQuestionTemplate template = getTemplate(dailyQuestion.getTemplateId());

        AnswerView answers = getAnswerView(dailyQuestion, userId, context.partnerUserId());

        return new TodayQuestionResponse(
                assignment.day(),
                dailyQuestion.getQuestionId(),
                template.getQuestion(),
                answers.myAnswer().map(DailyAnswer::getAnswer).orElse(null),
                answers.myAnswer().map(DailyAnswer::getAnsweredAt).orElse(null),
                answers.partnerAnswer().isPresent(),
                answers.visiblePartnerAnswer(),
                answers.bothAnswered(),
                calculateStreak(context.couple().getCoupleId(), userId, today),
                ANSWER_REWARD_POINTS
        );
    }

    @Transactional
    public DailyAnswerResponse answerToday(Long userId, String answer) {
        String normalizedAnswer = validateAndNormalizeAnswer(answer);
        CoupleContext context = getCoupleContext(userId);
        LocalDate today = LocalDate.now();
        DailyQuestion dailyQuestion = getOrCreateActiveQuestion(context.couple().getCoupleId(), today).dailyQuestion();

        Optional<DailyAnswer> existingAnswer = dailyAnswerRepository.findByQuestionIdAndUserId(
                dailyQuestion.getQuestionId(),
                userId
        );

        if (existingAnswer.isPresent()) {
            existingAnswer.get().updateAnswer(normalizedAnswer);
        } else {
            dailyAnswerRepository.save(DailyAnswer.builder()
                    .questionId(dailyQuestion.getQuestionId())
                    .userId(userId)
                    .answer(normalizedAnswer)
                    .build());
        }

        updateBothAnsweredIfNeeded(dailyQuestion);
        AnswerView answers = getAnswerView(dailyQuestion, userId, context.partnerUserId());

        return new DailyAnswerResponse(
                true,
                answers.bothAnswered(),
                calculateStreak(context.couple().getCoupleId(), userId, today),
                existingAnswer.isPresent() ? 0 : ANSWER_REWARD_POINTS
        );
    }

    @Transactional(readOnly = true)
    public DailyQuestionHistoryResponse getHistory(Long userId) {
        CoupleContext context = getCoupleContext(userId);
        LocalDate today = LocalDate.now();
        List<DailyQuestion> questions = dailyQuestionRepository.findByCoupleIdOrderByQuestionDateDescQuestionIdDesc(
                context.couple().getCoupleId()
        );

        if (questions.isEmpty()) {
            return new DailyQuestionHistoryResponse(List.of());
        }

        Map<Long, String> questionTexts = getQuestionTexts(questions);
        Map<Long, DailyAnswer> myAnswers = getAnswersByQuestionId(questions, userId);
        Map<Long, DailyAnswer> partnerAnswers = context.partnerUserId() == null
                ? Map.of()
                : getAnswersByQuestionId(questions, context.partnerUserId());
        Map<Long, Long> dayByQuestionId = getAssignedDays(questions);

        List<DailyQuestionHistoryItemResponse> items = questions.stream()
                .map(question -> new DailyQuestionHistoryItemResponse(
                        dayByQuestionId.get(question.getQuestionId()),
                        question.getQuestionId(),
                        questionTexts.get(question.getTemplateId()),
                        getStatus(
                                question.getQuestionDate(),
                                today,
                                myAnswers.containsKey(question.getQuestionId()),
                                partnerAnswers.containsKey(question.getQuestionId())
                        )
                ))
                .toList();

        return new DailyQuestionHistoryResponse(items);
    }

    @Transactional(readOnly = true)
    public DailyQuestionDetailResponse getDetail(Long userId, Long questionId) {
        CoupleContext context = getCoupleContext(userId);
        DailyQuestion dailyQuestion = dailyQuestionRepository.findByQuestionIdAndCoupleId(
                        questionId,
                        context.couple().getCoupleId()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "daily question not found"));
        DailyQuestionTemplate template = getTemplate(dailyQuestion.getTemplateId());

        AnswerView answers = getAnswerView(dailyQuestion, userId, context.partnerUserId());
        long day = getAssignedDay(context.couple().getCoupleId(), dailyQuestion.getQuestionId());

        return new DailyQuestionDetailResponse(
                day,
                dailyQuestion.getQuestionId(),
                template.getQuestion(),
                dailyQuestion.getQuestionDate(),
                answers.myAnswer().map(DailyAnswer::getAnswer).orElse(null),
                answers.visiblePartnerAnswer(),
                answers.bothAnswered()
        );
    }

    private CoupleContext getCoupleContext(Long userId) {
        CoupleMember myMember = coupleMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not connected"));
        Couple couple = coupleRepository.findById(myMember.getCoupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple not found"));
        Long partnerUserId = coupleMemberRepository.findFirstByCoupleIdAndUserIdNot(couple.getCoupleId(), userId)
                .map(CoupleMember::getUserId)
                .orElse(null);

        return new CoupleContext(couple, partnerUserId);
    }

    private DailyQuestionAssignment getOrCreateActiveQuestion(Long coupleId, LocalDate today) {
        long assignedQuestionCount = dailyQuestionRepository.countByCoupleId(coupleId);
        Optional<DailyQuestion> latestQuestion =
                dailyQuestionRepository.findFirstByCoupleIdOrderByQuestionDateDescQuestionIdDesc(coupleId);

        if (latestQuestion.isEmpty()) {
            return createQuestion(coupleId, today, 1);
        }

        DailyQuestion latest = latestQuestion.get();
        if (latest.getQuestionDate().equals(today)) {
            return new DailyQuestionAssignment(latest, assignedQuestionCount);
        }

        if (!dailyAnswerRepository.existsByQuestionIdAndAnsweredAtBefore(
                latest.getQuestionId(),
                today.atStartOfDay()
        )) {
            return new DailyQuestionAssignment(latest, assignedQuestionCount);
        }

        return createQuestion(coupleId, today, assignedQuestionCount + 1);
    }

    private DailyQuestionAssignment createQuestion(Long coupleId, LocalDate questionDate, long day) {
        DailyQuestion dailyQuestion = dailyQuestionRepository.findByCoupleIdAndQuestionDate(coupleId, questionDate)
                .orElseGet(() -> {
                    DailyQuestionTemplate template = getTemplateForDay(day);
                    return dailyQuestionRepository.save(DailyQuestion.builder()
                            .coupleId(coupleId)
                            .templateId(template.getTemplateId())
                            .questionDate(questionDate)
                            .build());
                });

        return new DailyQuestionAssignment(dailyQuestion, day);
    }

    private DailyQuestionTemplate getTemplateForDay(long day) {
        int dayIndex = (int) (((day - 1) % TEMPLATE_CYCLE_DAYS) + 1);
        return dailyQuestionTemplateRepository.findByDayIndex(dayIndex)
                .or(() -> dailyQuestionTemplateRepository.findFirstByOrderByDayIndexAsc())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "daily question template not found"
                ));
    }

    private DailyQuestionTemplate getTemplate(Long templateId) {
        return dailyQuestionTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "daily question template not found"
                ));
    }

    private Map<Long, String> getQuestionTexts(List<DailyQuestion> questions) {
        Set<Long> templateIds = questions.stream()
                .map(DailyQuestion::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return dailyQuestionTemplateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(
                        DailyQuestionTemplate::getTemplateId,
                        DailyQuestionTemplate::getQuestion
                ));
    }

    private Map<Long, DailyAnswer> getAnswersByQuestionId(List<DailyQuestion> questions, Long userId) {
        List<Long> questionIds = questions.stream()
                .map(DailyQuestion::getQuestionId)
                .toList();

        return dailyAnswerRepository.findByQuestionIdInAndUserId(questionIds, userId).stream()
                .collect(Collectors.toMap(DailyAnswer::getQuestionId, Function.identity()));
    }

    private long getAssignedDay(Long coupleId, Long questionId) {
        return getAssignedDays(dailyQuestionRepository.findByCoupleIdOrderByQuestionDateDescQuestionIdDesc(coupleId))
                .getOrDefault(questionId, 1L);
    }

    private Map<Long, Long> getAssignedDays(List<DailyQuestion> questionsDesc) {
        List<DailyQuestion> questionsAsc = new ArrayList<>(questionsDesc);
        Collections.reverse(questionsAsc);

        long day = 1;
        Map<Long, Long> assignedDays = new java.util.HashMap<>();
        for (DailyQuestion question : questionsAsc) {
            assignedDays.put(question.getQuestionId(), day++);
        }
        return assignedDays;
    }

    private AnswerView getAnswerView(DailyQuestion dailyQuestion, Long userId, Long partnerUserId) {
        Optional<DailyAnswer> myAnswer = dailyAnswerRepository.findByQuestionIdAndUserId(
                dailyQuestion.getQuestionId(),
                userId
        );
        Optional<DailyAnswer> partnerAnswer = partnerUserId == null
                ? Optional.empty()
                : dailyAnswerRepository.findByQuestionIdAndUserId(dailyQuestion.getQuestionId(), partnerUserId);
        boolean bothAnswered = Boolean.TRUE.equals(dailyQuestion.getBothAnswered())
                || (myAnswer.isPresent() && partnerAnswer.isPresent());
        String visiblePartnerAnswer = bothAnswered ? partnerAnswer.map(DailyAnswer::getAnswer).orElse(null) : null;

        return new AnswerView(myAnswer, partnerAnswer, bothAnswered, visiblePartnerAnswer);
    }

    private void updateBothAnsweredIfNeeded(DailyQuestion dailyQuestion) {
        if (Boolean.TRUE.equals(dailyQuestion.getBothAnswered())) {
            return;
        }
        if (dailyAnswerRepository.countDistinctUserIdsByQuestionId(dailyQuestion.getQuestionId()) >= 2) {
            dailyQuestion.markBothAnswered(LocalDateTime.now());
        }
    }

    private DailyQuestionStatus getStatus(
            LocalDate questionDate,
            LocalDate today,
            boolean myAnswered,
            boolean partnerAnswered
    ) {
        if (questionDate.equals(today)) {
            return DailyQuestionStatus.TODAY;
        }
        if (myAnswered && partnerAnswered) {
            return DailyQuestionStatus.BOTH_ANSWERED;
        }
        if (myAnswered) {
            return DailyQuestionStatus.MY_ANSWER_ONLY;
        }
        if (partnerAnswered) {
            return DailyQuestionStatus.PARTNER_ANSWER_ONLY;
        }
        return DailyQuestionStatus.UNANSWERED;
    }

    private String validateAndNormalizeAnswer(String answer) {
        String normalizedAnswer = answer == null ? "" : answer.trim();
        if (normalizedAnswer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer must not be blank");
        }
        if (normalizedAnswer.length() > ANSWER_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer must be 500 characters or less");
        }
        return normalizedAnswer;
    }

    private int calculateStreak(Long coupleId, Long userId, LocalDate today) {
        List<DailyQuestion> questions = dailyQuestionRepository.findByCoupleIdOrderByQuestionDateDescQuestionIdDesc(
                coupleId
        );
        if (questions.isEmpty()) {
            return 0;
        }

        Map<LocalDate, DailyQuestion> questionByDate = questions.stream()
                .collect(Collectors.toMap(
                        DailyQuestion::getQuestionDate,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        Set<Long> answeredQuestionIds = dailyAnswerRepository.findByQuestionIdInAndUserId(
                        questions.stream().map(DailyQuestion::getQuestionId).toList(),
                        userId
                ).stream()
                .map(DailyAnswer::getQuestionId)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = today;
        while (true) {
            DailyQuestion question = questionByDate.get(cursor);
            if (question == null || !answeredQuestionIds.contains(question.getQuestionId())) {
                return streak;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
    }

    private record CoupleContext(Couple couple, Long partnerUserId) {
    }

    private record DailyQuestionAssignment(DailyQuestion dailyQuestion, long day) {
    }

    private record AnswerView(
            Optional<DailyAnswer> myAnswer,
            Optional<DailyAnswer> partnerAnswer,
            boolean bothAnswered,
            String visiblePartnerAnswer
    ) {
    }
}
