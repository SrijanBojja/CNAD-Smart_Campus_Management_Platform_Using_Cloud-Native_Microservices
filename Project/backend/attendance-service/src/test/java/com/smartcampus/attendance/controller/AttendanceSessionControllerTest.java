package com.smartcampus.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.attendance.config.SecurityConfig;
import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.response.AttendanceSessionResponse;
import com.smartcampus.attendance.security.JwtAccessDeniedHandler;
import com.smartcampus.attendance.security.JwtAuthenticationEntryPoint;
import com.smartcampus.attendance.security.JwtTokenProvider;
import com.smartcampus.attendance.service.AttendanceSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceSessionController.class)
@Import(SecurityConfig.class)
class AttendanceSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceSessionService sessionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get session by ID as STUDENT returns 200 OK")
    void testGetSessionAsStudent() throws Exception {
        AttendanceSessionResponse response = new AttendanceSessionResponse();
        response.setId(1L);
        response.setSubjectId(10L);
        response.setFacultyUserId(201L);
        response.setSessionDate(LocalDate.of(2026, 9, 25));
        response.setStartTime(LocalTime.of(9, 0));
        response.setEndTime(LocalTime.of(10, 30));

        when(sessionService.getSessionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/attendance/sessions/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.subjectId").value(10L));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create session as STUDENT returns 403 Forbidden")
    void testCreateSessionAsStudentForbidden() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Create session as FACULTY returns 201 Created")
    void testCreateSessionAsFacultySuccess() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );

        AttendanceSessionResponse response = new AttendanceSessionResponse();
        response.setId(1L);
        response.setSubjectId(10L);
        response.setFacultyUserId(201L);

        when(sessionService.createSession(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
