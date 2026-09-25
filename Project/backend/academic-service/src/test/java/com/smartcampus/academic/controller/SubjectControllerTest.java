package com.smartcampus.academic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.academic.config.SecurityConfig;
import com.smartcampus.academic.dto.request.CreateSubjectRequest;
import com.smartcampus.academic.dto.response.SubjectResponse;
import com.smartcampus.academic.security.JwtAccessDeniedHandler;
import com.smartcampus.academic.security.JwtAuthenticationEntryPoint;
import com.smartcampus.academic.security.JwtTokenProvider;
import com.smartcampus.academic.service.SubjectService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
@Import(SecurityConfig.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubjectService subjectService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint entryPoint;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Create subject as FACULTY -> 201 Created")
    void testCreateSubjectFaculty() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest(1L, "CS301", "Data Structures", 4, 3, 201L);
        SubjectResponse response = new SubjectResponse(
                10L, 1L, "CSE-BTECH", "B.Tech CSE", "CS301", "Data Structures", 4, 3, 201L,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(subjectService.createSubject(any(CreateSubjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectCode").value("CS301"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create subject as STUDENT -> 403 Forbidden")
    void testCreateSubjectStudentForbidden() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest(1L, "CS301", "Data Structures", 4, 3, 201L);

        mockMvc.perform(post("/api/v1/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
