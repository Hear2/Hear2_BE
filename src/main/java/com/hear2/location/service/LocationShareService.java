package com.hear2.location.service;

import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationShareStatusRequest;
import com.hear2.location.dto.LocationShareStatusResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.dto.PartnerLocationResponse;
import com.hear2.location.entity.LocationShareSetting;
import com.hear2.location.entity.UserLocation;
import com.hear2.location.repository.LocationShareSettingRepository;
import com.hear2.location.repository.UserLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationShareService {

    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    private final LocationShareSettingRepository locationShareSettingRepository;
    private final UserLocationRepository userLocationRepository;
    private final LocationNameResolver locationNameResolver;
    private final CoupleMemberRepository coupleMemberRepository;

    @Value("${app.location-sharing.name-refresh-distance-meters:50}")
    private double nameRefreshDistanceMeters;

    @Transactional
    public LocationShareStatusResponse updateShareStatus(Long currentUserId, LocationShareStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "share status request is required");
        }

        CoupleContext context = resolveCoupleContext(currentUserId);
        if (request.getEnabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enabled is required");
        }

        LocationShareSetting setting = locationShareSettingRepository
                .findByCoupleIdAndUserId(context.coupleId(), context.userId())
                .orElseGet(() -> LocationShareSetting.create(
                        context.coupleId(),
                        context.userId(),
                        request.getEnabled()
                ));

        setting.changeEnabled(request.getEnabled());
        LocationShareSetting savedSetting = locationShareSettingRepository.save(setting);

        if (!savedSetting.isEnabled()) {
            userLocationRepository.findByCoupleIdAndUserId(
                    savedSetting.getCoupleId(),
                    savedSetting.getUserId()
            ).ifPresent(userLocationRepository::delete);
        }

        return LocationShareStatusResponse.from(savedSetting);
    }

    @Transactional(readOnly = true)
    public LocationShareStatusResponse getShareStatus(Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);

        return locationShareSettingRepository.findByCoupleIdAndUserId(context.coupleId(), context.userId())
                .map(LocationShareStatusResponse::from)
                .orElseGet(() -> LocationShareStatusResponse.disabled(context.coupleId(), context.userId()));
    }

    @Transactional
    public LocationResponse updateCurrentLocation(Long currentUserId, LocationUpdateRequest request) {
        validateLocationRequest(request);
        CoupleContext context = resolveCoupleContext(currentUserId);
        requireSharingEnabled(context.coupleId(), context.userId());

        Optional<UserLocation> savedLocation = userLocationRepository
                .findByCoupleIdAndUserId(context.coupleId(), context.userId());
        ResolvedLocationNames locationNames = resolveLocationNames(request, savedLocation.orElse(null));

        UserLocation location = savedLocation
                .orElseGet(() -> UserLocation.create(
                        context.coupleId(),
                        context.userId(),
                        request.getLat(),
                        request.getLng(),
                        request.getAccuracy(),
                        locationNames.placeName(),
                        locationNames.addressName(),
                        toRecordedAt(request)
                ));

        location.update(
                request.getLat(),
                request.getLng(),
                request.getAccuracy(),
                locationNames.placeName(),
                locationNames.addressName(),
                toRecordedAt(request)
        );

        return toLocationResponse(userLocationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public PartnerLocationResponse getCoupleLocation(Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        requireSharingEnabled(context.coupleId(), context.userId());

        LocationResponse me = userLocationRepository
                .findByCoupleIdAndUserId(context.coupleId(), context.userId())
                .map(this::toLocationResponse)
                .orElse(null);

        LocationResponse partner = null;
        if (isSharingEnabled(context.coupleId(), context.partnerId())) {
            partner = userLocationRepository
                    .findByCoupleIdAndUserId(context.coupleId(), context.partnerId())
                    .map(this::toLocationResponse)
                    .orElse(null);
        }

        return PartnerLocationResponse.of(me, partner);
    }

    private void validateLocationRequest(LocationUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location request is required");
        }

        if (request.getLat() == null || request.getLat() < -90 || request.getLat() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude must be between -90 and 90");
        }
        if (request.getLng() == null || request.getLng() < -180 || request.getLng() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude must be between -180 and 180");
        }
        if (request.getAccuracy() != null && request.getAccuracy() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accuracy must not be negative");
        }
    }

    private ResolvedLocationNames resolveLocationNames(LocationUpdateRequest request, UserLocation savedLocation) {
        if (canReuseLocationNames(request, savedLocation)) {
            return new ResolvedLocationNames(savedLocation.getPlaceName(), savedLocation.getAddressName());
        }

        ResolvedLocationNames resolvedNames = locationNameResolver.resolve(
                request.getLat(),
                request.getLng()
        );
        if (resolvedNames == null) {
            resolvedNames = new ResolvedLocationNames(null, null);
        }

        return new ResolvedLocationNames(
                resolvedNames.placeName(),
                resolvedNames.addressName()
        );
    }

    private boolean canReuseLocationNames(LocationUpdateRequest request, UserLocation savedLocation) {
        if (savedLocation == null) {
            return false;
        }
        if (!StringUtils.hasText(savedLocation.getPlaceName()) && !StringUtils.hasText(savedLocation.getAddressName())) {
            return false;
        }

        double movedDistanceMeters = calculateDistanceMeters(
                savedLocation.getLatitude(),
                savedLocation.getLongitude(),
                request.getLat(),
                request.getLng()
        );
        return movedDistanceMeters < nameRefreshDistanceMeters;
    }

    private double calculateDistanceMeters(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        double earthRadiusMeters = 6_371_000;
        double latitudeDistance = Math.toRadians(endLatitude - startLatitude);
        double longitudeDistance = Math.toRadians(endLongitude - startLongitude);
        double startLatitudeRadians = Math.toRadians(startLatitude);
        double endLatitudeRadians = Math.toRadians(endLatitude);

        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(startLatitudeRadians) * Math.cos(endLatitudeRadians)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return earthRadiusMeters * angularDistance;
    }

    private void requireSharingEnabled(Long coupleId, Long userId) {
        if (!isSharingEnabled(coupleId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "location sharing is disabled");
        }
    }

    private boolean isSharingEnabled(Long coupleId, Long userId) {
        return locationShareSettingRepository.findByCoupleIdAndUserId(coupleId, userId)
                .map(LocationShareSetting::isEnabled)
                .orElse(false);
    }

    private CoupleContext resolveCoupleContext(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
        }

        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "connected partner not found"));

        CoupleMember partner = coupleMemberRepository
                .findFirstByCoupleIdAndUserIdNot(member.getCoupleId(), currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "connected partner not found"));

        return new CoupleContext(member.getCoupleId(), currentUserId, partner.getUserId());
    }

    private LocalDateTime toRecordedAt(LocationUpdateRequest request) {
        if (request.getCapturedAt() == null) {
            return null;
        }

        return LocalDateTime.ofInstant(request.getCapturedAt().toInstant(), ZoneOffset.UTC);
    }

    private LocationResponse toLocationResponse(UserLocation location) {
        return LocationResponse.from(location, isStale(location));
    }

    private boolean isStale(UserLocation location) {
        return location.getUpdatedAt().plus(STALE_THRESHOLD).isBefore(LocalDateTime.now());
    }

    private record CoupleContext(Long coupleId, Long userId, Long partnerId) {
    }
}
