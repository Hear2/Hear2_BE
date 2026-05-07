package com.hear2.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hear2OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hear2 API")
                        .description("Hear2 채팅, 감정 분석, 리스크 알림 API 문서")
                        .version("v1")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
