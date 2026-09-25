package com.smartcampus.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.notification.config.SecurityConfig;
import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.dto.response.NotificationResponse;
import com.smartcampus.notification.dto.response.UnreadCountResponse;
import com.smartcampus.notification.entity.NotificationStatus;
import com.smartcampus.notification.entity.NotificationType;
import com.smartcampus.notification.security.JwtAccessDeniedHandler;
import com.smartcampus.notification.security.JwtAuthenticationEntryPoint;
import com.smartcampus.notification.security.JwtTokenProvider;
import com.smartcampus.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get my notifications as STUDENT returns 200 OK")
    void testGetMyNotificationsAsStudent() throws Exception {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setTitle("Welcome to Campus");
        response.setNotificationType(NotificationType.GENERAL);
        response.setStatus(NotificationStatus.UNREAD);

        when(notificationService.getMyNotifications(any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Welcome to Campus"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get unread count as STUDENT returns 200 OK")
    void testGetUnreadCountAsStudent() throws Exception {
        when(notificationService.getUnreadCount(any()))
                .thenReturn(new UnreadCountResponse(4L));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4L));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create notification as STUDENT returns 403 Forbidden")
    void testCreateNotificationAsStudentForbidden() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest(
                101L, "Event Notice", "Hackathon starts tomorrow", NotificationType.EVENT, "EVENT", 10L
        );

        mockMvc.perform(post("/api/v1/notifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Create notification as FACULTY returns 201 Created")
    void testCreateNotificationAsFacultySuccess() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest(
                101L, "Assignment Due", "CS-301 Lab 2 due Friday", NotificationType.ACADEMIC, null, null
        );

        NotificationResponse response = new NotificationResponse();
        response.setId(10L);
        response.setRecipientUserId(101L);
        response.setTitle("Assignment Due");
        response.setStatus(NotificationStatus.UNREAD);

        when(notificationService.createNotification(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Assignment Due"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Mark notification as read returns 200 OK")
    void testMarkAsReadSuccess() throws Exception {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setStatus(NotificationStatus.READ);

        when(notificationService.markAsRead(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Mark notification as unread returns 200 OK")
    void testMarkAsUnreadSuccess() throws Exception {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setStatus(NotificationStatus.UNREAD);

        when(notificationService.markAsUnread(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/1/unread")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNREAD"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Delete notification returns 204 No Content")
    void testDeleteNotificationSuccess() throws Exception {
        doNothing().when(notificationService).deleteNotification(eq(1L), any());

        mockMvc.perform(delete("/api/v1/notifications/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
