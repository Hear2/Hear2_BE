package com.hear2.location.controller;

import com.hear2.global.response.ApiResponse;
import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationShareStatusRequest;
import com.hear2.location.dto.LocationShareStatusResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.dto.PartnerLocationResponse;
import com.hear2.location.service.LocationShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationShareController {

    private final LocationShareService locationShareService;

    @PutMapping("/share-status")
    public ApiResponse<LocationShareStatusResponse> updateShareStatus(
            @Valid @RequestBody LocationShareStatusRequest request
    ) {
        return ApiResponse.success(locationShareService.updateShareStatus(request));
    }

    @GetMapping("/couples/{coupleId}/users/{userId}/share-status")
    public ApiResponse<LocationShareStatusResponse> getShareStatus(
            @PathVariable Long coupleId,
            @PathVariable Long userId
    ) {
        return ApiResponse.success(locationShareService.getShareStatus(coupleId, userId));
    }

    @PutMapping("/current")
    public ApiResponse<LocationResponse> updateCurrentLocation(
            @Valid @RequestBody LocationUpdateRequest request
    ) {
        return ApiResponse.success(locationShareService.updateCurrentLocation(request));
    }

    @GetMapping("/couples/{coupleId}/partners/{requesterId}")
    public ApiResponse<PartnerLocationResponse> getPartnerLocation(
            @PathVariable Long coupleId,
            @PathVariable Long requesterId
    ) {
        return ApiResponse.success(locationShareService.getPartnerLocation(coupleId, requesterId));
    }
}
