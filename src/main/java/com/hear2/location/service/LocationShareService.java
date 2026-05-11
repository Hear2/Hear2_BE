package com.hear2.location.service;

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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationShareService {

    private final LocationShareSettingRepository locationShareSettingRepository;
    private final UserLocationRepository userLocationRepository;
    private final LocationNameResolver locationNameResolver;

    @Value("${app.location-sharing.name-refresh-distance-meters:50}")
    private double nameRefreshDistanceMeters;

    @Transactional
    public LocationShareStatusResponse updateShareStatus(LocationShareStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "share status request is required");
        }

        validateRequiredIds(request.getCoupleId(), request.getUserId());
        if (request.getEnabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enabled is required");
        }

        LocationShareSetting setting = locationShareSettingRepository
                .findByCoupleIdAndUserId(request.getCoupleId(), request.getUserId())
                .orElseGet(() -> LocationShareSetting.create(
                        request.getCoupleId(),
                        request.getUserId(),
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
    public LocationShareStatusResponse getShareStatus(Long coupleId, Long userId) {
        validateRequiredIds(coupleId, userId);

        return locationShareSettingRepository.findByCoupleIdAndUserId(coupleId, userId)
                .map(LocationShareStatusResponse::from)
                .orElseGet(() -> LocationShareStatusResponse.disabled(coupleId, userId));
    }

    @Transactional
    public LocationResponse updateCurrentLocation(LocationUpdateRequest request) {
        validateLocationRequest(request);
        requireSharingEnabled(request.getCoupleId(), request.getUserId());

        Optional<UserLocation> savedLocation = userLocationRepository
                .findByCoupleIdAndUserId(request.getCoupleId(), request.getUserId());
        ResolvedLocationNames locationNames = resolveLocationNames(request, savedLocation.orElse(null));

        UserLocation location = savedLocation
                .orElseGet(() -> UserLocation.create(
                        request.getCoupleId(),
                        request.getUserId(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getAccuracyMeters(),
                        locationNames.placeName(),
                        locationNames.addressName(),
                        request.getRecordedAt()
                ));

        location.update(
                request.getLatitude(),
                request.getLongitude(),
                request.getAccuracyMeters(),
                locationNames.placeName(),
                locationNames.addressName(),
                request.getRecordedAt()
        );

        return LocationResponse.from(userLocationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public PartnerLocationResponse getPartnerLocation(Long coupleId, Long requesterId) {
        validateRequiredIds(coupleId, requesterId);

        Optional<UserLocation> sharedPartnerLocation = userLocationRepository
                .findByCoupleIdAndUserIdNotOrderByUpdatedAtDesc(coupleId, requesterId)
                .stream()
                .filter(location -> isSharingEnabled(coupleId, location.getUserId()))
                .findFirst();

        return sharedPartnerLocation
                .map(location -> PartnerLocationResponse.shared(
                        coupleId,
                        requesterId,
                        LocationResponse.from(location)
                ))
                .orElseGet(() -> PartnerLocationResponse.hidden(coupleId, requesterId));
    }

    private void validateLocationRequest(LocationUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location request is required");
        }

        validateRequiredIds(request.getCoupleId(), request.getUserId());

        if (request.getLatitude() == null || request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude must be between -90 and 90");
        }
        if (request.getLongitude() == null || request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude must be between -180 and 180");
        }
        if (request.getAccuracyMeters() != null && request.getAccuracyMeters() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accuracyMeters must not be negative");
        }
    }

    private ResolvedLocationNames resolveLocationNames(LocationUpdateRequest request, UserLocation savedLocation) {
        if (canReuseLocationNames(request, savedLocation)) {
            return new ResolvedLocationNames(savedLocation.getPlaceName(), savedLocation.getAddressName());
        }

        ResolvedLocationNames resolvedNames = locationNameResolver.resolve(
                request.getLatitude(),
                request.getLongitude()
        );
        if (resolvedNames == null) {
            resolvedNames = new ResolvedLocationNames(null, null);
        }

        return new ResolvedLocationNames(
                firstText(resolvedNames.placeName(), request.getPlaceName()),
                firstText(resolvedNames.addressName(), request.getAddressName())
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
                request.getLatitude(),
                request.getLongitude()
        );
        return movedDistanceMeters < nameRefreshDistanceMeters;
    }

    private String firstText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }

        return StringUtils.hasText(fallback) ? fallback : null;
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

    private void validateRequiredIds(Long coupleId, Long userId) {
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
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
}
