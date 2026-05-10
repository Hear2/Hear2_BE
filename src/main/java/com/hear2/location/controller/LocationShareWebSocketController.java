package com.hear2.location.controller;

import com.hear2.location.dto.LocationResponse;
import com.hear2.location.dto.LocationUpdateRequest;
import com.hear2.location.service.LocationShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LocationShareWebSocketController {

    private final LocationShareService locationShareService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/locations/current")
    public void updateCurrentLocation(LocationUpdateRequest request) {
        LocationResponse response = locationShareService.updateCurrentLocation(request);

        messagingTemplate.convertAndSend(
                "/sub/locations/couples/" + response.getCoupleId(),
                response
        );
    }
}
