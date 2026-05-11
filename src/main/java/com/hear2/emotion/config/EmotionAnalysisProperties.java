package com.hear2.emotion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.emotion.analysis")
public class EmotionAnalysisProperties {

    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8000";
    private String analyzePath = "/analyze";
    private String judgePath = "/judge";
    private int connectTimeoutMs = 1500;
    private int readTimeoutMs = 5000;
}
