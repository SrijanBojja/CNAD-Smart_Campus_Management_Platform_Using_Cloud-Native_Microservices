package com.smartcampus.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTest {

    @Test
    @DisplayName("Context loads successfully for Auth Service")
    void contextLoads() {
    }
}
