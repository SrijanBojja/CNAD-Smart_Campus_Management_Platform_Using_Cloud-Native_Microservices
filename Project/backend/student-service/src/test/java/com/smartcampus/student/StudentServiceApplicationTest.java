package com.smartcampus.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StudentServiceApplicationTest {

    @Test
    @DisplayName("Context loads successfully for Student Service")
    void contextLoads() {
    }
}
