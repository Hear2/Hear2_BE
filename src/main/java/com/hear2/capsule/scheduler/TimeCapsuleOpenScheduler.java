package com.hear2.capsule.scheduler;

import com.hear2.capsule.service.TimeCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeCapsuleOpenScheduler {

    private final TimeCapsuleService timeCapsuleService;

    @Scheduled(fixedDelayString = "${app.time-capsule.open-scheduler-delay-ms:60000}")
    public void openExpiredCapsules() {
        timeCapsuleService.openExpiredCapsules();
    }
}
