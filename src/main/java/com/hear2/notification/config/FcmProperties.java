package com.hear2.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {

    private boolean enabled = false;
    private String credentialsPath;
    private String projectId;
}
