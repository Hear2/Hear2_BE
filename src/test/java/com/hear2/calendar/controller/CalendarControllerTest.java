package com.hear2.calendar.controller;

import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarEventVisibility;
import com.hear2.calendar.repository.CalendarEventMemoryLinkRepository;
import com.hear2.calendar.repository.CalendarEventRepository;
import com.hear2.anniversary.entity.Anniversary;
import com.hear2.anniversary.repository.AnniversaryRepository;
import com.hear2.anniversary.repository.HiddenAutoAnniversaryRepository;
import com.hear2.anniversary.support.AnniversaryDdayType;
import com.hear2.anniversary.support.AnniversaryType;
import com.hear2.chat.entity.ChatMessage;
import com.hear2.chat.entity.MessageType;
import com.hear2.chat.repository.ChatMessageRepository;
import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.global.security.JwtProvider;
import com.hear2.memory.entity.Memory;
import com.hear2.memory.repository.MemoryAiTagRepository;
import com.hear2.memory.repository.MemoryRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerTest {

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
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private CalendarEventMemoryLinkRepository calendarEventMemoryLinkRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private MemoryAiTagRepository memoryAiTagRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private HiddenAutoAnniversaryRepository hiddenAutoAnniversaryRepository;

    private User user;
    private User partner;
    private Couple couple;

    @BeforeEach
    void setUp() {
        hiddenAutoAnniversaryRepository.deleteAll();
        anniversaryRepository.deleteAll();
        calendarEventMemoryLinkRepository.deleteAll();
        calendarEventRepository.deleteAll();
        chatMessageRepository.deleteAll();
        memoryAiTagRepository.deleteAll();
        memoryRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("calendar-user@example.com")
                .password("encoded-password")
                .nickname("예진")
                .provider("LOCAL")
                .build());
        partner = userRepository.save(User.builder()
                .email("calendar-partner@example.com")
                .password("encoded-password")
                .nickname("지호")
                .provider("LOCAL")
                .build());
        couple = coupleRepository.save(Couple.builder()
                .coupleCode("CALENDAR")
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
    void createEventUsesAuthenticatedUsersCouple() throws Exception {
        mockMvc.perform(post("/api/v1/calendar/events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "서울숲 데이트",
                                  "target": "SHARED",
                                  "startsAt": "2026-04-12T05:00:00Z",
                                  "endsAt": "2026-04-12T10:00:00Z",
                                  "locationName": "서울숲",
                                  "addressName": "서울시 성동구 서울숲길 273",
                                  "memo": "벚꽃 보고 카페 들르기",
                                  "tags": ["데이트", "봄"],
                                  "remindBeforeMinutes": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("서울숲 데이트"))
                .andExpect(jsonPath("$.data.coupleId").value(couple.getCoupleId()))
                .andExpect(jsonPath("$.data.ownerId").value(user.getUserId()))
                .andExpect(jsonPath("$.data.viewType").value("SHARED"))
                .andExpect(jsonPath("$.data.color.name").value("Pink"));

        assertThat(calendarEventRepository.findAll())
                .singleElement()
                .satisfies(event -> assertThat(event.getCoupleId()).isEqualTo(couple.getCoupleId()));
    }

    @Test
    void getMonthReturnsEventColorsAndMemoryMarker() throws Exception {
        saveEvent("요가", user.getUserId(), CalendarEventVisibility.PERSONAL, 14);
        saveEvent("회식", partner.getUserId(), CalendarEventVisibility.PERSONAL, 17);
        saveEvent("서울숲 데이트", user.getUserId(), CalendarEventVisibility.SHARED, 12);
        saveMemory(LocalDate.of(2026, 4, 12));
        couple.updateStartDate(LocalDate.of(2025, 4, 12));
        coupleRepository.save(couple);

        mockMvc.perform(get("/api/v1/calendar/month")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[11].date").value("2026-04-12"))
                .andExpect(jsonPath("$.data.days[11].memoryMarker.hasMemory").value(true))
                .andExpect(jsonPath("$.data.days[11].memoryMarker.markerIcon").value("HEART"))
                .andExpect(jsonPath("$.data.days[11].anniversaries[*].autoKey", hasItem("YEAR_1")))
                .andExpect(jsonPath("$.data.days[11].events[*].title", hasItem("서울숲 데이트")))
                .andExpect(jsonPath("$.data.days[13].events[*].viewType", hasItem("OWNER")))
                .andExpect(jsonPath("$.data.days[16].events[*].viewType", hasItem("PARTNER")));
    }

    @Test
    void dateDetailReturnsEventsAndMemories() throws Exception {
        CalendarEvent event = saveEvent("서울숲 데이트", user.getUserId(), CalendarEventVisibility.SHARED, 12);
        Memory memory = saveMemory(LocalDate.of(2026, 4, 12));
        anniversaryRepository.save(Anniversary.builder()
                .coupleId(couple.getCoupleId())
                .createdBy(user.getUserId())
                .title("예진 생일")
                .type(AnniversaryType.BIRTHDAY)
                .anniversaryDate(LocalDate.of(2026, 4, 12))
                .ddayType(AnniversaryDdayType.D_MINUS)
                .repeatYearly(true)
                .shared(true)
                .icon("CAKE")
                .color("#FFB75E")
                .notifyDays(List.of(7, 1, 0))
                .build());

        mockMvc.perform(post("/api/v1/calendar/events/{eventId}/memories/{memoryId}", event.getId(), memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedMemories[0].id").value(memory.getId()));

        mockMvc.perform(get("/api/v1/calendar/dates/{date}", "2026-04-12")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[0].title").value("서울숲 데이트"))
                .andExpect(jsonPath("$.data.memories[0].id").value(memory.getId()))
                .andExpect(jsonPath("$.data.anniversaries[*].title", hasItem("예진 생일")));
    }

    @Test
    void recurringEventIsExpandedInMonthAndDateDetail() throws Exception {
        mockMvc.perform(post("/api/v1/calendar/events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "매주 산책",
                                  "target": "SHARED",
                                  "startsAt": "2026-04-07T01:00:00Z",
                                  "endsAt": "2026-04-07T02:00:00Z",
                                  "recurrenceRule": "FREQ=WEEKLY;INTERVAL=1;COUNT=4"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calendar/month")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[6].events[*].title", hasItem("매주 산책")))
                .andExpect(jsonPath("$.data.days[13].events[*].title", hasItem("매주 산책")))
                .andExpect(jsonPath("$.data.days[20].events[*].title", hasItem("매주 산책")))
                .andExpect(jsonPath("$.data.days[27].events[*].title", hasItem("매주 산책")));

        mockMvc.perform(get("/api/v1/calendar/dates/{date}", "2026-04-21")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[0].title").value("매주 산책"))
                .andExpect(jsonPath("$.data.events[0].startsAt").value("2026-04-21T01:00:00Z"));
    }

    @Test
    void linkChatMessageRequiresSameCoupleMessage() throws Exception {
        CalendarEvent event = saveEvent("서울숲 데이트", user.getUserId(), CalendarEventVisibility.SHARED, 12);
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .coupleId(couple.getCoupleId())
                .senderId(user.getUserId())
                .receiverId(partner.getUserId())
                .content("이날 같이 가자")
                .messageType(MessageType.TEXT)
                .build());

        mockMvc.perform(post("/api/v1/calendar/events/{eventId}/chat-messages/{messageId}", event.getId(), message.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedChatMessageId").value(message.getId()));

        mockMvc.perform(delete("/api/v1/calendar/events/{eventId}/chat-message", event.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedChatMessageId").value(nullValue()));
    }

    @Test
    void deleteEventDoesNotDeleteLinkedMemory() throws Exception {
        CalendarEvent event = saveEvent("서울숲 데이트", user.getUserId(), CalendarEventVisibility.SHARED, 12);
        Memory memory = saveMemory(LocalDate.of(2026, 4, 12));

        mockMvc.perform(post("/api/v1/calendar/events/{eventId}/memories/{memoryId}", event.getId(), memory.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/calendar/events/{eventId}", event.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
                .andExpect(status().isOk());

        assertThat(calendarEventRepository.findById(event.getId())).isEmpty();
        assertThat(memoryRepository.findById(memory.getId())).isPresent();
        assertThat(calendarEventMemoryLinkRepository.findAll()).isEmpty();
    }

    private CalendarEvent saveEvent(
            String title,
            Long ownerId,
            CalendarEventVisibility visibility,
            int dayOfMonth
    ) {
        return calendarEventRepository.save(CalendarEvent.builder()
                .coupleId(couple.getCoupleId())
                .ownerId(ownerId)
                .createdBy(user.getUserId())
                .title(title)
                .visibility(visibility)
                .startsAt(LocalDateTime.of(2026, 4, dayOfMonth, 5, 0))
                .endsAt(LocalDateTime.of(2026, 4, dayOfMonth, 10, 0))
                .allDay(false)
                .build());
    }

    private Memory saveMemory(LocalDate memoryDate) {
        return memoryRepository.save(Memory.builder()
                .coupleId(couple.getCoupleId())
                .uploaderId(user.getUserId())
                .storedPhotoPath("s3://memory/" + memoryDate + ".jpg")
                .originalFileName("memory.jpg")
                .photoContentType("image/jpeg")
                .photoSize(1024L)
                .memo("벚꽃 보러 가기")
                .memoryDate(memoryDate)
                .build());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId);
    }
}
