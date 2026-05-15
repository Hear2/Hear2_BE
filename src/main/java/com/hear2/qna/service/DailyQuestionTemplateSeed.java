package com.hear2.qna.service;

import com.hear2.qna.entity.DailyQuestionTemplate;
import com.hear2.qna.repository.DailyQuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DailyQuestionTemplateSeed implements ApplicationRunner {

    private final DailyQuestionTemplateRepository dailyQuestionTemplateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(1, "상대방의 어떤 점이 가장 사랑스러운가요?");
        seed(2, "함께 가보고 싶은 장소는 어디인가요?");
        seed(3, "상대방에게 오늘 꼭 전하고 싶은 말은 무엇인가요?");
        seed(4, "서로 닮아가고 있다고 느끼는 부분은 무엇인가요?");
        seed(5, "상대방이 나를 가장 기쁘게 하는 순간은 언제인가요?");
    }

    private void seed(int dayIndex, String question) {
        if (dailyQuestionTemplateRepository.existsByDayIndex(dayIndex)) {
            return;
        }

        dailyQuestionTemplateRepository.save(DailyQuestionTemplate.builder()
                .dayIndex(dayIndex)
                .question(question)
                .build());
    }
}
