package com.hear2.calendar.service;

import com.hear2.calendar.dto.CalendarPlaceSearchResponse;
import com.hear2.calendar.dto.CalendarPlaceSearchResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CalendarPlaceSearchService {

    private static final String KAKAO_AUTH_PREFIX = "KakaoAK ";
    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 15;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;

    public CalendarPlaceSearchService(
            ObjectMapper objectMapper,
            @Value("${kakao.rest-api-key:}") String restApiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey;
    }

    public CalendarPlaceSearchResponse search(String query, Integer size) {
        String normalizedQuery = normalizeQuery(query);
        if (!StringUtils.hasText(restApiKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kakao REST API key is not configured");
        }

        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_SEARCH_PATH)
                            .queryParam("query", normalizedQuery)
                            .queryParam("page", DEFAULT_PAGE)
                            .queryParam("size", normalizeSize(size))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_AUTH_PREFIX + restApiKey)
                    .retrieve()
                    .body(String.class);

            return CalendarPlaceSearchResponse.builder()
                    .query(normalizedQuery)
                    .places(parsePlaces(response))
                    .build();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Failed to search Kakao places. query={}", normalizedQuery, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao place search failed", exception);
        }
    }

    private String normalizeQuery(String query) {
        String normalizedQuery = query == null ? null : query.trim();
        if (!StringUtils.hasText(normalizedQuery)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        if (normalizedQuery.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is too long");
        }
        return normalizedQuery;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be positive");
        }
        return Math.min(size, MAX_SIZE);
    }

    private List<CalendarPlaceSearchResultResponse> parsePlaces(String response) {
        if (!StringUtils.hasText(response)) {
            return List.of();
        }

        JsonNode documents = objectMapper.readTree(response).path("documents");
        if (!documents.isArray() || documents.isEmpty()) {
            return List.of();
        }

        List<CalendarPlaceSearchResultResponse> places = new ArrayList<>();
        for (JsonNode document : documents) {
            places.add(CalendarPlaceSearchResultResponse.builder()
                    .id(text(document, "id"))
                    .placeName(text(document, "place_name"))
                    .addressName(text(document, "address_name"))
                    .roadAddressName(text(document, "road_address_name"))
                    .latitude(decimal(document, "y"))
                    .longitude(decimal(document, "x"))
                    .categoryName(text(document, "category_name"))
                    .phone(text(document, "phone"))
                    .placeUrl(text(document, "place_url"))
                    .build());
        }
        return places;
    }

    private String text(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asString("");
        return StringUtils.hasText(value) ? value : null;
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }
}
