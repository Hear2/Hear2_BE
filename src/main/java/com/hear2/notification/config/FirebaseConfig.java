package com.hear2.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final FcmProperties properties;

    @Bean
    @ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        if (!StringUtils.hasText(properties.getCredentialsPath())) {
            throw new IllegalStateException("FCM_CREDENTIALS_PATH is required when FCM is enabled");
        }

        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (!apps.isEmpty()) {
            return apps.get(0);
        }

        try (FileInputStream serviceAccount = new FileInputStream(properties.getCredentialsPath())) {
            FirebaseOptions.Builder options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (StringUtils.hasText(properties.getProjectId())) {
                options.setProjectId(properties.getProjectId());
            }

            return FirebaseApp.initializeApp(options.build());
        }
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
