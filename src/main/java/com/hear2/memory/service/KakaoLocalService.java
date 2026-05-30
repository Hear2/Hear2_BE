package com.hear2.memory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Slf4j
@Service
public class KakaoLocalService {

    private static final String KAKAO_AUTH_PREFIX = "KakaoAK ";
    private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;

    public KakaoLocalService(
            ObjectMapper objectMapper,
            @Value("${kakao.rest-api-key:}") String restApiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .build();
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey;
    }

    public String resolveLocationName(BigDecimal latitude, BigDecimal longitude) {
        KakaoLocationNames locationNames = resolveLocationNames(latitude, longitude);
        return locationNames == null ? null : locationNames.displayName();
    }

    public KakaoLocationNames resolveLocationNames(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(restApiKey) || latitude == null || longitude == null) {
            return null;
        }

        String addressName = resolveDongAddressName(latitude, longitude);

        if (!StringUtils.hasText(addressName)) {
            return null;
        }

        return new KakaoLocationNames(
                null,
                StringUtils.hasText(addressName) ? addressName : null
        );
    }

    private String resolveDongAddressName(BigDecimal latitude, BigDecimal longitude) {
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

            return parseDongAddressName(response);
        } catch (RuntimeException e) {
            log.warn("Failed to resolve Kakao address name: {}", e.getMessage());
            return null;
        }
    }

    private String parseDongAddressName(String response) {
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
            String dongAddressName = formatDongAddress(firstDocument.path("address"));
            if (StringUtils.hasText(dongAddressName)) {
                return dongAddressName;
            }

            String addressName = firstDocument.path("address").path("address_name").asString("");
            return trimAddressToDong(addressName);
        } catch (RuntimeException e) {
            log.warn("Failed to parse Kakao location response: {}", e.getMessage());
            return null;
        }
    }

    private String formatDongAddress(JsonNode address) {
        if (address == null || address.isMissingNode() || address.isNull()) {
            return null;
        }

        String region1 = address.path("region_1depth_name").asString("");
        String region2 = address.path("region_2depth_name").asString("");
        String region3 = address.path("region_3depth_h_name").asString("");
        if (!StringUtils.hasText(region3)) {
            region3 = address.path("region_3depth_name").asString("");
        }
        if (!StringUtils.hasText(region1) || !StringUtils.hasText(region2) || !StringUtils.hasText(region3)) {
            return null;
        }

        return String.join(" ", region1.trim(), region2.trim(), region3.trim());
    }

    private String trimAddressToDong(String addressName) {
        if (!StringUtils.hasText(addressName)) {
            return null;
        }

        String[] parts = addressName.trim().split("\\s+");
        if (parts.length < 3) {
            return addressName.trim();
        }

        return String.join(" ", parts[0], parts[1], parts[2]);
    }
}
