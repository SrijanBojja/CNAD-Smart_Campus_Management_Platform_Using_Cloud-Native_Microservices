package com.smartcampus.academic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.academic.config.SecurityConfig;
import com.smartcampus.academic.dto.request.CreateCourseRequest;
import com.smartcampus.academic.dto.response.CourseResponse;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.security.JwtAccessDeniedHandler;
import com.smartcampus.academic.security.JwtAuthenticationEntryPoint;
import com.smartcampus.academic.security.JwtAuthenticationFilter;
import com.smartcampus.academic.security.JwtTokenProvider;
import com.smartcampus.academic.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint entryPoint;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get course by ID as STUDENT -> 200 OK")
    void testGetCourseAsStudent() throws Exception {
        CourseResponse response = new CourseResponse(
                1L, "CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(courseService.getCourseById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/courses/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CSE-BTECH"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Create course as ADMIN -> 201 Created")
    void testCreateCourseAsAdmin() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4);
        CourseResponse response = new CourseResponse(
                1L, "CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(courseService.createCourse(any(CreateCourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create course as STUDENT -> 403 Forbidden")
    void testCreateCourseAsStudentForbidden() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4);

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
