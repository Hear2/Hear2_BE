package com.hear2.location.controller;

import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.service.LocationShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LocationShareWebSocketController {

    private final LocationShareService locationShareService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/locations/current")
    public void updateCurrentLocation(LocationUpdateRequest request, Principal principal) {
        LocationResponse response = locationShareService.updateCurrentLocation(currentUserId(principal), request);

        messagingTemplate.convertAndSend(
                "/sub/locations/couples/" + response.getCoupleId(),
                response
        );
    }

    private Long currentUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login is required");
    }
}
