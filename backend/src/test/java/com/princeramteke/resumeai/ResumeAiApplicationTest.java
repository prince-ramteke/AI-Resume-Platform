package com.princeramteke.resumeai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiry-minutes=60"
})
class ResumeAiApplicationTest {

    @Test
    void contextLoads() {
    }
}
