package com.hear2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret",
        "app.jwt.access-token-expiration-seconds=3600",
        "app.jwt.refresh-token-expiration-seconds=1209600"
})
class Hear2BeApplicationTests {

    @Test
    void contextLoads() {
    }

}
