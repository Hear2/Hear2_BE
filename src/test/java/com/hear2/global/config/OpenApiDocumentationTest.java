package com.hear2.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsIncludeChatEmotionFcmAndJudgeApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"/api/v1/chats/messages\"")))
                .andExpect(content().string(containsString("\"/api/v1/emotions/analyze\"")))
                .andExpect(content().string(containsString("\"/api/v1/notifications/fcm-tokens\"")))
                .andExpect(content().string(containsString("\"/api/v1/judge\"")))
                .andExpect(content().string(containsString("\"/api/v1/judge/couples/{coupleId}/histories\"")))
                .andExpect(content().string(containsString("\"/api/v1/judge/couples/{coupleId}/patterns\"")));
    }
}
