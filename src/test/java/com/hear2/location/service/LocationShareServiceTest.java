package com.hear2.location.service;

import com.hear2.couple.entity.Couple;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.couple.repository.CoupleRepository;
import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationShareStatusRequest;
import com.hear2.location.dto.LocationShareStatusResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.dto.PartnerLocationResponse;
import com.hear2.location.repository.LocationShareSettingRepository;
import com.hear2.location.repository.UserLocationRepository;
import com.hear2.user.entity.User;
import com.hear2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

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

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private UserRepository userRepository;

    private Couple couple;
    private User me;
    private User partner;

    @BeforeEach
    void setUp() {
        userLocationRepository.deleteAll();
        locationShareSettingRepository.deleteAll();
        coupleMemberRepository.deleteAll();
        coupleRepository.deleteAll();
        userRepository.deleteAll();

        me = saveUser("me-location@example.com");
        partner = saveUser("partner-location@example.com");
        couple = coupleRepository.save(Couple.builder()
                .coupleCode("LOC001")
                .build());

        saveMember(couple.getCoupleId(), me.getUserId(), "OWNER");
        saveMember(couple.getCoupleId(), partner.getUserId(), "PARTNER");
    }

    @Test
    void locationShareStatusIsDisabledByDefault() {
        LocationShareStatusResponse response = locationShareService.getShareStatus(me.getUserId());

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.getCoupleId()).isEqualTo(couple.getCoupleId());
        assertThat(response.getUserId()).isEqualTo(me.getUserId());
    }

    @Test
    void currentLocationCannotBeUpdatedWhenSharingIsDisabled() {
        LocationUpdateRequest request = buildLocationUpdateRequest(37.2221, 127.1875);

        assertThatThrownBy(() -> locationShareService.updateCurrentLocation(me.getUserId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void enabledUserCanUpdateCurrentLocation() {
        enableSharing(me.getUserId());

        LocationResponse response = locationShareService.updateCurrentLocation(
                me.getUserId(),
                buildLocationUpdateRequest(37.2221, 127.1875)
        );

        assertThat(response.getCoupleId()).isEqualTo(couple.getCoupleId());
        assertThat(response.getUserId()).isEqualTo(me.getUserId());
        assertThat(response.getLat()).isEqualTo(37.2221);
        assertThat(response.getLng()).isEqualTo(127.1875);
        assertThat(response.getPlaceName()).isEqualTo("명지대학교 자연캠퍼스");
        assertThat(response.getAddressName()).isEqualTo("경기 용인시 처인구 명지로 116");
        assertThat(response.getLocationName()).isEqualTo("명지대학교 자연캠퍼스");
        assertThat(userLocationRepository.findByCoupleIdAndUserId(couple.getCoupleId(), me.getUserId())).isPresent();
    }

    @Test
    void partnerLocationIsHiddenWhenPartnerSharingIsDisabled() {
        enableSharing(me.getUserId());
        enableSharing(partner.getUserId());
        locationShareService.updateCurrentLocation(me.getUserId(), buildLocationUpdateRequest(37.2221, 127.1875));

        locationShareService.updateShareStatus(me.getUserId(), LocationShareStatusRequest.builder()
                .enabled(false)
                .build());

        PartnerLocationResponse response = locationShareService.getCoupleLocation(partner.getUserId());

        assertThat(response.getMe()).isNull();
        assertThat(response.getPartner()).isNull();
        assertThat(userLocationRepository.findByCoupleIdAndUserId(couple.getCoupleId(), me.getUserId())).isEmpty();
    }

    @Test
    void requesterCanReadSharedPartnerLocationOnlyInsideSameCouple() {
        enableSharing(me.getUserId());
        enableSharing(partner.getUserId());
        locationShareService.updateCurrentLocation(me.getUserId(), buildLocationUpdateRequest(37.2221, 127.1875));

        PartnerLocationResponse response = locationShareService.getCoupleLocation(partner.getUserId());

        assertThat(response.getPartner()).isNotNull();
        assertThat(response.getPartner().getUserId()).isEqualTo(me.getUserId());
        assertThat(response.getPartner().getCoupleId()).isEqualTo(couple.getCoupleId());
        assertThat(response.getPartner().getLat()).isEqualTo(37.2221);
    }

    @Test
    void requesterCannotReadPartnerLocationWhenOwnSharingIsDisabled() {
        enableSharing(me.getUserId());
        locationShareService.updateCurrentLocation(me.getUserId(), buildLocationUpdateRequest(37.2221, 127.1875));

        assertThatThrownBy(() -> locationShareService.getCoupleLocation(partner.getUserId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void currentLocationIsUpdatedWithoutKeepingOldRows() {
        enableSharing(me.getUserId());

        locationShareService.updateCurrentLocation(me.getUserId(), buildLocationUpdateRequest(37.2221, 127.1875));
        LocationResponse updated = locationShareService.updateCurrentLocation(
                me.getUserId(),
                buildLocationUpdateRequest(37.2230, 127.1880)
        );

        assertThat(updated.getLat()).isEqualTo(37.2230);
        assertThat(updated.getLng()).isEqualTo(127.1880);
        assertThat(userLocationRepository.findAll()).hasSize(1);
    }

    private void enableSharing(Long userId) {
        locationShareService.updateShareStatus(userId, LocationShareStatusRequest.builder()
                .enabled(true)
                .build());
    }

    private LocationUpdateRequest buildLocationUpdateRequest(Double latitude, Double longitude) {
        return LocationUpdateRequest.builder()
                .lat(latitude)
                .lng(longitude)
                .accuracy(20.0)
                .capturedAt(OffsetDateTime.parse("2026-05-09T14:30:00Z"))
                .build();
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .nickname(email)
                .provider("LOCAL")
                .emailVerified(true)
                .build());
    }

    private void saveMember(Long coupleId, Long userId, String role) {
        coupleMemberRepository.save(CoupleMember.builder()
                .coupleId(coupleId)
                .userId(userId)
                .role(role)
                .build());
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
