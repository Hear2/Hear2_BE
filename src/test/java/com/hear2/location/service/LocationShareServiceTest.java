package com.hear2.location.service;

import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationShareStatusRequest;
import com.hear2.location.dto.LocationShareStatusResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.dto.PartnerLocationResponse;
import com.hear2.location.repository.LocationShareSettingRepository;
import com.hear2.location.repository.UserLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LocationShareServiceTest {

    @Autowired
    private LocationShareService locationShareService;

    @Autowired
    private LocationShareSettingRepository locationShareSettingRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @BeforeEach
    void setUp() {
        userLocationRepository.deleteAll();
        locationShareSettingRepository.deleteAll();
    }

    @Test
    void locationShareStatusIsDisabledByDefault() {
        LocationShareStatusResponse response = locationShareService.getShareStatus(1L, 10L);

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.getCoupleId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(10L);
    }

    @Test
    void currentLocationCannotBeUpdatedWhenSharingIsDisabled() {
        LocationUpdateRequest request = buildLocationUpdateRequest(1L, 10L, 37.2221, 127.1875);

        assertThatThrownBy(() -> locationShareService.updateCurrentLocation(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void enabledUserCanUpdateCurrentLocation() {
        enableSharing(1L, 10L);

        LocationResponse response = locationShareService.updateCurrentLocation(
                buildLocationUpdateRequest(1L, 10L, 37.2221, 127.1875)
        );

        assertThat(response.getCoupleId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getLatitude()).isEqualTo(37.2221);
        assertThat(response.getLongitude()).isEqualTo(127.1875);
        assertThat(response.getPlaceName()).isEqualTo("명지대학교 자연캠퍼스");
        assertThat(response.getAddressName()).isEqualTo("경기 용인시 처인구 명지로 116");
        assertThat(response.getLocationName()).isEqualTo("명지대학교 자연캠퍼스");
        assertThat(userLocationRepository.findByCoupleIdAndUserId(1L, 10L)).isPresent();
    }

    @Test
    void backendResolvesLocationNamesWhenRequestHasOnlyCoordinates() {
        enableSharing(1L, 10L);

        LocationResponse response = locationShareService.updateCurrentLocation(
                buildCoordinatesOnlyRequest(1L, 10L, 37.2221, 127.1875)
        );

        assertThat(response.getLatitude()).isEqualTo(37.2221);
        assertThat(response.getLongitude()).isEqualTo(127.1875);
        assertThat(response.getPlaceName()).isEqualTo("명지대학교 자연캠퍼스");
        assertThat(response.getAddressName()).isEqualTo("경기 용인시 처인구 명지로 116");
        assertThat(response.getLocationName()).isEqualTo("명지대학교 자연캠퍼스");
    }

    @Test
    void partnerLocationIsHiddenWhenPartnerSharingIsDisabled() {
        enableSharing(1L, 10L);
        locationShareService.updateCurrentLocation(buildLocationUpdateRequest(1L, 10L, 37.2221, 127.1875));

        locationShareService.updateShareStatus(LocationShareStatusRequest.builder()
                .coupleId(1L)
                .userId(10L)
                .enabled(false)
                .build());

        PartnerLocationResponse response = locationShareService.getPartnerLocation(1L, 20L);

        assertThat(response.isShared()).isFalse();
        assertThat(response.getLocation()).isNull();
        assertThat(userLocationRepository.findByCoupleIdAndUserId(1L, 10L)).isEmpty();
    }

    @Test
    void requesterCanReadSharedPartnerLocationOnlyInsideSameCouple() {
        enableSharing(1L, 10L);
        enableSharing(2L, 30L);

        locationShareService.updateCurrentLocation(buildLocationUpdateRequest(1L, 10L, 37.2221, 127.1875));
        locationShareService.updateCurrentLocation(buildLocationUpdateRequest(2L, 30L, 35.1796, 129.0756));

        PartnerLocationResponse response = locationShareService.getPartnerLocation(1L, 20L);

        assertThat(response.isShared()).isTrue();
        assertThat(response.getLocation().getUserId()).isEqualTo(10L);
        assertThat(response.getLocation().getCoupleId()).isEqualTo(1L);
        assertThat(response.getLocation().getLatitude()).isEqualTo(37.2221);
    }

    @Test
    void currentLocationIsUpdatedWithoutKeepingOldRows() {
        enableSharing(1L, 10L);

        locationShareService.updateCurrentLocation(buildLocationUpdateRequest(1L, 10L, 37.2221, 127.1875));
        LocationResponse updated = locationShareService.updateCurrentLocation(
                buildLocationUpdateRequest(1L, 10L, 37.2230, 127.1880)
        );

        assertThat(updated.getLatitude()).isEqualTo(37.2230);
        assertThat(updated.getLongitude()).isEqualTo(127.1880);
        assertThat(userLocationRepository.findAll()).hasSize(1);
    }

    private void enableSharing(Long coupleId, Long userId) {
        locationShareService.updateShareStatus(LocationShareStatusRequest.builder()
                .coupleId(coupleId)
                .userId(userId)
                .enabled(true)
                .build());
    }

    private LocationUpdateRequest buildLocationUpdateRequest(
            Long coupleId,
            Long userId,
            Double latitude,
            Double longitude
    ) {
        return LocationUpdateRequest.builder()
                .coupleId(coupleId)
                .userId(userId)
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(20.0)
                .placeName("명지대학교 자연캠퍼스")
                .addressName("경기 용인시 처인구 명지로 116")
                .recordedAt(LocalDateTime.of(2026, 5, 9, 14, 30))
                .build();
    }

    private LocationUpdateRequest buildCoordinatesOnlyRequest(
            Long coupleId,
            Long userId,
            Double latitude,
            Double longitude
    ) {
        return LocationUpdateRequest.builder()
                .coupleId(coupleId)
                .userId(userId)
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(20.0)
                .recordedAt(LocalDateTime.of(2026, 5, 9, 14, 30))
                .build();
    }

    @TestConfiguration
    static class TestLocationNameResolverConfig {

        @Bean
        @Primary
        LocationNameResolver testLocationNameResolver() {
            return (latitude, longitude) -> new ResolvedLocationNames(
                    "명지대학교 자연캠퍼스",
                    "경기 용인시 처인구 명지로 116"
            );
        }
    }
}
