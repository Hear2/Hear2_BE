package com.hear2.location.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KakaoLocationNameResolver implements LocationNameResolver {

    private static final String KAKAO_AUTH_PREFIX = "KakaoAK ";
    private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";
    private static final String CATEGORY_SEARCH_PATH = "/v2/local/search/category.json";
    private static final String SORT_BY_DISTANCE = "distance";
    private static final String[] PLACE_CATEGORY_GROUP_CODES = {
            "AT4", "CT1", "SC4", "FD6", "CE7", "AD5", "MT1", "CS2",
            "PK6", "SW8", "BK9", "HP8", "PM9", "PO3", "AC5", "AG2"
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;
    private final int radiusMeters;

    public KakaoLocationNameResolver(
            ObjectMapper objectMapper,
            @Value("${kakao.rest-api-key:}") String restApiKey,
            @Value("${kakao.local.radius-meters:1000}") int radiusMeters
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey;
        this.radiusMeters = radiusMeters;
    }

    @Override
    public ResolvedLocationNames resolve(Double latitude, Double longitude) {
        if (!StringUtils.hasText(restApiKey) || latitude == null || longitude == null) {
            return new ResolvedLocationNames(null, null);
        }

        String addressName = resolveAddressName(latitude, longitude);
        String placeName = resolvePlaceName(latitude, longitude);

        if (!StringUtils.hasText(placeName) && !StringUtils.hasText(addressName)) {
            return new ResolvedLocationNames(null, null);
        }

        return new ResolvedLocationNames(
                StringUtils.hasText(placeName) ? placeName : null,
                StringUtils.hasText(addressName) ? addressName : null
        );
    }

    private String resolveAddressName(Double latitude, Double longitude) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(COORD_TO_ADDRESS_PATH)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_AUTH_PREFIX + restApiKey)
                    .retrieve()
                    .body(String.class);

            return parseAddressName(response);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve Kakao address name: {}", e.getMessage());
            return null;
        }
    }

    private String resolvePlaceName(Double latitude, Double longitude) {
        for (String categoryGroupCode : PLACE_CATEGORY_GROUP_CODES) {
            String placeName = resolvePlaceName(latitude, longitude, categoryGroupCode);
            if (StringUtils.hasText(placeName)) {
                return placeName;
            }
        }

        return null;
    }

    private String resolvePlaceName(Double latitude, Double longitude, String categoryGroupCode) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CATEGORY_SEARCH_PATH)
                            .queryParam("category_group_code", categoryGroupCode)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", radiusMeters)
                            .queryParam("sort", SORT_BY_DISTANCE)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, KAKAO_AUTH_PREFIX + restApiKey)
                    .retrieve()
                    .body(String.class);

            return parsePlaceName(response);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve Kakao place name: {}", e.getMessage());
            return null;
        }
    }

    private String parseAddressName(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                return null;
            }

            JsonNode firstDocument = documents.get(0);
            String roadAddress = firstDocument.path("road_address").path("address_name").asString("");
            if (StringUtils.hasText(roadAddress)) {
                return roadAddress;
            }

            String address = firstDocument.path("address").path("address_name").asString("");
            return StringUtils.hasText(address) ? address : null;
        } catch (RuntimeException e) {
            log.warn("Failed to parse Kakao location response: {}", e.getMessage());
            return null;
        }
    }

    private String parsePlaceName(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                return null;
            }

            String placeName = documents.get(0).path("place_name").asString("");
            return StringUtils.hasText(placeName) ? placeName : null;
        } catch (RuntimeException e) {
            log.warn("Failed to parse Kakao place response: {}", e.getMessage());
            return null;
        }
    }
}
