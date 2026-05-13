package com.hear2.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.hear2.report.support.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 리포트 응답")
public class ReportResponse {

    @Schema(description = "리포트 타입", example = "WEEKLY")
    private ReportType reportType;

    @Schema(description = "기준 날짜", example = "2026-05-10")
    private LocalDate anchorDate;

    @Schema(description = "기간 시작일", example = "2026-05-04")
    private LocalDate periodStartDate;

    @Schema(description = "기간 종료일", example = "2026-05-10")
    private LocalDate periodEndDate;

    @Schema(description = "기간 시작일 alias", example = "2026-05-04")
    private LocalDate periodStart;

    @Schema(description = "기간 종료일 alias", example = "2026-05-10")
    private LocalDate periodEnd;

    @Schema(description = "기간 라벨", example = "5/4 (월) - 5/10 (일)")
    private String periodLabel;

    @Schema(description = "리포트 제목", example = "5월 첫째 주 리포트")
    private String title;

    @Schema(description = "리포트 요약 문구. OpenAI 연동 시 AI 생성 문구를 우선 사용하고 실패 시 fallback 문구를 반환")
    private String summary;

    @Schema(description = "차트 제목", example = "감정 흐름")
    private String chartTitle;

    @Schema(description = "차트 보조 설명", example = "날짜별 감정 분석 요약")
    private String chartSubtitle;

    @Schema(description = "날짜별 감정 흐름")
    private List<EmotionFlowPointResponse> emotionFlow;

    @Schema(description = "가장 높은 긍정 비율과 가장 많은 분석 메시지가 동시에 나타난 라벨", example = "토")
    private String peakEmotionLabel;

    @Schema(description = "peakEmotionLabel의 긍정 비율", example = "100")
    private int peakEmotionScore;

    @Schema(description = "해당 주간 커플이 ChatMessage에서 주고받은 전체 채팅 메시지 수", example = "327")
    private long totalConversationCount;

    @Schema(description = "감정 분석 결과가 있는 TEXT 메시지 기준 긍정 비율", example = "82")
    private int positiveRatio;

    @Schema(description = "감정 분석 결과가 있는 TEXT 메시지 수", example = "120")
    private long analyzedConversationCount;

    @Schema(description = "해당 기간 AI 판사 호출 건수", example = "1")
    private long conflictCount;

    @Schema(description = "해당 기간 Memory/Album에 업로드된 사진 수", example = "24")
    private long uploadedPhotoCount;

    @Schema(description = "해당 기간 워드클라우드용 TOP 단어 목록")
    private List<MostUsedWordResponse> mostUsedWords;

    @Schema(description = "1일1답 응답 수. 1일1답 기능 연동 전까지 null 반환", nullable = true, example = "null")
    private Integer oneAnswerResponseCount;

    @Schema(description = "1일1답 전체 개수. 기능 연동 전까지 null 반환", nullable = true, example = "null")
    private Integer oneAnswerTotalCount;

    @Schema(description = "기간 내 우세 감정 타입", example = "HAPPY")
    private String dominantEmotionType;

    @Schema(description = "기간 내 우세 감정 라벨", example = "행복")
    private String dominantEmotionLabel;

    @Schema(description = "기간 내 우세 감정 이모지", example = "😊")
    private String dominantEmotionEmoji;

    @Schema(description = "주간 리포트용 추억 하이라이트", nullable = true)
    private MemoryHighlightResponse memoryHighlight;

    @Schema(description = "월간 리포트용 전월 대비", nullable = true)
    private MonthlyComparisonResponse monthlyComparison;

    @Schema(description = "월간 리포트용 관계 건강도", nullable = true)
    private RelationshipHealthResponse relationshipHealth;

    @Schema(description = "월간 리포트용 AI 추천 목록. OpenAI 연동 실패 시 fallback 추천을 반환", nullable = true)
    private List<MonthlyRecommendationResponse> monthlyRecommendations;

    @Schema(description = "공유 코드", nullable = true, example = "7ce8db13b4b2")
    private String shareCode;

    @Schema(description = "공유 리포트 JSON URL", nullable = true, example = "http://localhost:8080/reports/shared/7ce8db13b4b2")
    private String shareUrl;

    private String shareTitle;
    private String shareMessage;
}
