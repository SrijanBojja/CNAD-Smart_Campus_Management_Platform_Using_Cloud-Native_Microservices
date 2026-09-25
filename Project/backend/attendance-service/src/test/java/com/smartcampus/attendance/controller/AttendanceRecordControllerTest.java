package com.smartcampus.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.attendance.config.SecurityConfig;
import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.response.AttendanceRecordResponse;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.security.JwtAccessDeniedHandler;
import com.smartcampus.attendance.security.JwtAuthenticationEntryPoint;
import com.smartcampus.attendance.security.JwtTokenProvider;
import com.smartcampus.attendance.service.AttendanceRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceRecordController.class)
@Import(SecurityConfig.class)
class AttendanceRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceRecordService recordService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("List attendance records as STUDENT returns 403 Forbidden")
    void testListRecordsAsStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/records")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create attendance record as STUDENT returns 403 Forbidden")
    void testCreateRecordAsStudentForbidden() throws Exception {
        CreateAttendanceRecordRequest request = new CreateAttendanceRecordRequest(
                1L, 50L, AttendanceStatus.PRESENT, "Remarks"
        );

        mockMvc.perform(post("/api/v1/attendance/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Create attendance record as FACULTY returns 201 Created")
    void testCreateRecordAsFacultySuccess() throws Exception {
        CreateAttendanceRecordRequest request = new CreateAttendanceRecordRequest(
                1L, 50L, AttendanceStatus.PRESENT, "On time"
        );

        AttendanceRecordResponse response = new AttendanceRecordResponse();
        response.setId(100L);
        response.setSessionId(1L);
        response.setStudentId(50L);
        response.setStatus(AttendanceStatus.PRESENT);

        when(recordService.createRecord(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/attendance/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));
    }
}
