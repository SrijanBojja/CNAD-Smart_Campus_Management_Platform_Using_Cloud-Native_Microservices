package com.smartcampus.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.notification.client.AuthServiceClient;
import com.smartcampus.notification.client.dto.UserValidationResponseDto;
import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.entity.NotificationStatus;
import com.smartcampus.notification.entity.NotificationType;
import com.smartcampus.notification.repository.NotificationRepository;
import com.smartcampus.notification.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthServiceClient authServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String adminToken;
    private String facultyToken;
    private String student1Token;
    private String student2Token;

    @BeforeEach
    void cleanAndSetup() {
        notificationRepository.deleteAll();

        adminToken = "Bearer " + jwtTokenProvider.generateToken(1L, "adminUser", "admin@smartcampus.edu", List.of("ADMIN"));
        facultyToken = "Bearer " + jwtTokenProvider.generateToken(2L, "facultyUser", "faculty@smartcampus.edu", List.of("FACULTY"));
        student1Token = "Bearer " + jwtTokenProvider.generateToken(101L, "student1", "student1@smartcampus.edu", List.of("STUDENT"));
        student2Token = "Bearer " + jwtTokenProvider.generateToken(102L, "student2", "student2@smartcampus.edu", List.of("STUDENT"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Complete Notification Lifecycle and Object-Level Authorization")
    void testCompleteLifecycle() throws Exception {
        // 1. Student attempts to create notification -> 403 Forbidden
        CreateNotificationRequest studentCreateReq = new CreateNotificationRequest(
                102L, "Spam", "Test", NotificationType.GENERAL, null, null
        );
        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentCreateReq)))
                .andExpect(status().isForbidden());

        // 2. Faculty creates notification for Student 1 (recipient 101L) -> 201 Created
        when(authServiceClient.validateUser(eq(101L), isNull(), any()))
                .thenReturn(new UserValidationResponseDto(101L, true, true));

        CreateNotificationRequest createReq = new CreateNotificationRequest(
                101L,
                "Midterm Schedule Released",
                "The schedule for CS-301 Midterm examination has been posted.",
                NotificationType.ACADEMIC,
                "ACADEMIC_SCHEDULE",
                12L
        );

        String createJson = mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.recipientUserId").value(101L))
                .andExpect(jsonPath("$.title").value("Midterm Schedule Released"))
                .andExpect(jsonPath("$.status").value("UNREAD"))
                .andReturn().getResponse().getContentAsString();

        Long notificationId = objectMapper.readTree(createJson).get("id").asLong();

        // 3. Student 1 checks unread count -> 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1L));

        // 4. Student 2 checks unread count -> 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0L));

        // 5. Student 1 views own notification -> 200 OK
        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.recipientUserId").value(101L));

        // 6. Student 2 attempts to view Student 1's notification -> 403 Forbidden
        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("Authorization", student2Token))
                .andExpect(status().isForbidden());

        // 7. Student 2 attempts to mark Student 1's notification as read -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", student2Token))
                .andExpect(status().isForbidden());

        // 8. Student 1 marks own notification as READ -> 200 OK
        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").exists());

        // 9. Student 1 checks unread count -> 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0L));

        // 10. Student 1 marks notification as UNREAD -> 200 OK
        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/unread")
                        .header("Authorization", student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNREAD"))
                .andExpect(jsonPath("$.readAt").doesNotExist());

        // 11. Student 2 attempts to delete Student 1's notification -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/notifications/" + notificationId)
                        .header("Authorization", student2Token))
                .andExpect(status().isForbidden());

        // 12. Student 1 deletes own notification -> 204 No Content
        mockMvc.perform(delete("/api/v1/notifications/" + notificationId)
                        .header("Authorization", student1Token))
                .andExpect(status().isNoContent());

        // 13. Student 1 gets notification by ID -> 404 Not Found
        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("Authorization", student1Token))
                .andExpect(status().isNotFound());
    }
}
