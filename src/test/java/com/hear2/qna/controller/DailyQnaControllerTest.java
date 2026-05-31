package com.hear2.qna.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hear2.character.repository.CharacterExpHistoryRepository;
import com.hear2.character.repository.CoupleCharacterRepository;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.qna.dto.TodayQuestionResponse;
import com.hear2.qna.entity.DailyAnswer;
import com.hear2.qna.entity.DailyQuestion;
import com.hear2.qna.entity.DailyQuestionTemplate;
import com.hear2.qna.repository.DailyAnswerRepository;
import com.hear2.qna.repository.DailyQuestionRepository;
import com.hear2.qna.repository.DailyQuestionTemplateRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DailyQnaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private DailyQuestionRepository dailyQuestionRepository;

    @Autowired
    private DailyAnswerRepository dailyAnswerRepository;

    @Autowired
    private DailyQuestionTemplateRepository dailyQuestionTemplateRepository;

    @Autowired
    private CoupleCharacterRepository coupleCharacterRepository;

    @Autowired
    private CharacterExpHistoryRepository characterExpHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user;
    private User partner;
    private Couple couple;

    @BeforeEach
    void setUp() {
        characterExpHistoryRepository.deleteAll();
        coupleCharacterRepository.deleteAll();
        dailyAnswerRepository.deleteAll();
        dailyQuestionRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("qna-user@example.com")
                .password("encoded-password")
                .nickname("qna-user")
                .provider("LOCAL")
                .build());

        partner = userRepository.save(User.builder()
                .email("qna-partner@example.com")
                .password("encoded-password")
                .nickname("qna-partner")
                .provider("LOCAL")
                .build());

        couple = coupleRepository.save(Couple.builder()
                .coupleCode("QNA00001")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(user.getUserId())
                .role("OWNER")
                .build());

        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(couple.getCoupleId())
                .userId(partner.getUserId())
                .role("MEMBER")
                .build());
    }

    @Test
    void getTodayKeepsPastQuestionWhenLatestQuestionHasNoAnswers() throws Exception {
        DailyQuestion pastQuestion = dailyQuestionRepository.save(DailyQuestion.builder()
                .coupleId(couple.getCoupleId())
                .templateId(template(1).getTemplateId())
                .questionDate(LocalDate.now().minusDays(1))
                .build());

        TodayQuestionResponse response = getToday();

        assertThat(response.day()).isEqualTo(1);
        assertThat(response.questionId()).isEqualTo(pastQuestion.getQuestionId());
        assertThat(dailyQuestionRepository.countByCoupleId(couple.getCoupleId())).isEqualTo(1);
    }

    @Test
    void getTodayKeepsPastQuestionWhenFirstAnswerIsCreatedToday() throws Exception {
        DailyQuestion pastQuestion = dailyQuestionRepository.save(DailyQuestion.builder()
                .coupleId(couple.getCoupleId())
                .templateId(template(1).getTemplateId())
                .questionDate(LocalDate.now().minusDays(1))
                .build());

        postAnswer(user.getUserId(), "answered today");

        mockMvc.perform(get("/api/v1/qna/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value(1))
                .andExpect(jsonPath("$.questionId").value(pastQuestion.getQuestionId()));

        assertThat(dailyQuestionRepository.countByCoupleId(couple.getCoupleId())).isEqualTo(1);
    }

    @Test
    void getTodayCreatesNextQuestionWhenLatestPastQuestionHasAnswerBeforeToday() throws Exception {
        DailyQuestion pastQuestion = dailyQuestionRepository.save(DailyQuestion.builder()
                .coupleId(couple.getCoupleId())
                .templateId(template(1).getTemplateId())
                .questionDate(LocalDate.now().minusDays(2))
                .build());
        dailyAnswerRepository.save(DailyAnswer.builder()
                .questionId(pastQuestion.getQuestionId())
                .userId(user.getUserId())
                .answer("answered yesterday")
                .answeredAt(LocalDateTime.now().minusDays(1))
                .build());

        TodayQuestionResponse response = getToday();

        assertThat(response.day()).isEqualTo(2);
        assertThat(response.questionId()).isNotEqualTo(pastQuestion.getQuestionId());
        assertThat(dailyQuestionRepository.countByCoupleId(couple.getCoupleId())).isEqualTo(2);

        DailyQuestion todayQuestion = dailyQuestionRepository.findById(response.questionId()).orElseThrow();
        assertThat(todayQuestion.getQuestionDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void getTodayReusesQuestionAlreadyAssignedToday() throws Exception {
        DailyQuestion todayQuestion = dailyQuestionRepository.save(DailyQuestion.builder()
                .coupleId(couple.getCoupleId())
                .templateId(template(1).getTemplateId())
                .questionDate(LocalDate.now())
                .build());

        TodayQuestionResponse response = getToday();

        assertThat(response.day()).isEqualTo(1);
        assertThat(response.questionId()).isEqualTo(todayQuestion.getQuestionId());
        assertThat(dailyQuestionRepository.countByCoupleId(couple.getCoupleId())).isEqualTo(1);
    }

    @Test
    void partnerAnswerIsVisibleOnlyAfterBothAnsweredAndBothAnsweredStateIsRecorded() throws Exception {
        DailyQuestion todayQuestion = dailyQuestionRepository.save(DailyQuestion.builder()
                .coupleId(couple.getCoupleId())
                .templateId(template(1).getTemplateId())
                .questionDate(LocalDate.now())
                .build());

        postAnswer(user.getUserId(), "my answer");

        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(10L));
        assertThat(characterExpHistoryRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/qna/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(partner.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(todayQuestion.getQuestionId()))
                .andExpect(jsonPath("$.partnerAnswered").value(true))
                .andExpect(jsonPath("$.partnerAnswer").doesNotExist())
                .andExpect(jsonPath("$.bothAnswered").value(false));

        postAnswer(partner.getUserId(), "partner answer");

        mockMvc.perform(get("/api/v1/qna/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerAnswer").value("partner answer"))
                .andExpect(jsonPath("$.bothAnswered").value(true));

        DailyQuestion answeredQuestion = dailyQuestionRepository.findById(todayQuestion.getQuestionId()).orElseThrow();
        LocalDateTime bothAnsweredAt = answeredQuestion.getBothAnsweredAt();
        assertThat(answeredQuestion.getBothAnswered()).isTrue();
        assertThat(bothAnsweredAt).isNotNull();
        assertThat(answeredQuestion.getQnaRewardGranted()).isTrue();
        assertThat(answeredQuestion.getQnaRewardGrantedAt()).isNotNull();
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(50L));
        assertThat(characterExpHistoryRepository.count()).isEqualTo(3);

        postAnswer(user.getUserId(), "edited answer");

        DailyQuestion editedQuestion = dailyQuestionRepository.findById(todayQuestion.getQuestionId()).orElseThrow();
        assertThat(editedQuestion.getBothAnsweredAt()).isEqualTo(bothAnsweredAt);
        assertThat(editedQuestion.getQnaRewardGranted()).isTrue();
        assertThat(coupleCharacterRepository.findByCoupleId(couple.getCoupleId()))
                .hasValueSatisfying(character -> assertThat(character.getExp()).isEqualTo(50L));
        assertThat(characterExpHistoryRepository.count()).isEqualTo(3);
    }

    @Test
    void answerTodayRejectsMoreThanFiveHundredCharacters() throws Exception {
        String longAnswer = "a".repeat(501);

        mockMvc.perform(post("/api/v1/qna/today/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"%s"}
                                """.formatted(longAnswer)))
                .andExpect(status().isBadRequest());

        assertThat(dailyAnswerRepository.findAll()).isEmpty();
    }

    private void postAnswer(Long userId, String answer) throws Exception {
        mockMvc.perform(post("/api/v1/qna/today/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answer":"%s"}
                                """.formatted(answer)))
                .andExpect(status().isOk());
    }

    private TodayQuestionResponse getToday() throws Exception {
        return getToday(user.getUserId());
    }

    private TodayQuestionResponse getToday(Long userId) throws Exception {
        String content = mockMvc.perform(get("/api/v1/qna/today")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(content, TodayQuestionResponse.class);
    }

    private DailyQuestionTemplate template(int dayIndex) {
        return dailyQuestionTemplateRepository.findByDayIndex(dayIndex).orElseThrow();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
