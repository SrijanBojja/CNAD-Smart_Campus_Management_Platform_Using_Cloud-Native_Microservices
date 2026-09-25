package com.smartcampus.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.dto.response.StudentResponse;
import com.smartcampus.student.entity.StudentStatus;
import com.smartcampus.student.exception.DuplicateResourceException;
import com.smartcampus.student.exception.GlobalExceptionHandler;
import com.smartcampus.student.exception.ResourceNotFoundException;
import com.smartcampus.student.exception.UserValidationFailedException;
import com.smartcampus.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private StudentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(studentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = new StudentResponse(
                1L,
                101L,
                "STU2026001",
                "Srijan",
                "Bojja",
                LocalDate.of(2005, 8, 15),
                "9876543210",
                "Computer Science and Engineering",
                "B.Tech CSE",
                2,
                "A",
                StudentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/students returns 201 Created on valid request")
    void testCreateStudentSuccess() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                101L, "STU2026001", "Srijan", "Bojja",
                LocalDate.of(2005, 8, 15), "9876543210", "CSE", "B.Tech", 2, "A"
        );

        when(studentService.createStudent(any(CreateStudentRequest.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer mock.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.studentNumber").value("STU2026001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/students returns 400 Bad Request on validation failure (missing userId)")
    void testCreateStudentValidationFailure() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                null, "", "", "",
                null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer mock.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/v1/students returns 409 Conflict on duplicate resource")
    void testCreateStudentDuplicateConflict() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                101L, "STU2026001", "Srijan", "Bojja",
                LocalDate.of(2005, 8, 15), "9876543210", "CSE", "B.Tech", 2, "A"
        );

        when(studentService.createStudent(any(CreateStudentRequest.class), any()))
                .thenThrow(new DuplicateResourceException("Student profile already exists for user ID: 101"));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer mock.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Student profile already exists for user ID: 101"));
    }

    @Test
    @DisplayName("POST /api/v1/students returns 400 Bad Request when Auth validation fails")
    void testCreateStudentAuthValidationFailed() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                101L, "STU2026001", "Srijan", "Bojja",
                LocalDate.of(2005, 8, 15), "9876543210", "CSE", "B.Tech", 2, "A"
        );

        when(studentService.createStudent(any(CreateStudentRequest.class), any()))
                .thenThrow(new UserValidationFailedException("Auth user with ID 101 does not exist"));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer mock.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Auth user with ID 101 does not exist"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} returns 200 and StudentResponse")
    void testGetStudentByIdSuccess() throws Exception {
        when(studentService.getStudentById(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/students/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.studentNumber").value("STU2026001"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} returns 404 Not Found when student does not exist")
    void testGetStudentByIdNotFound() throws Exception {
        when(studentService.getStudentById(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Student not found with ID: 999"));

        mockMvc.perform(get("/api/v1/students/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/students returns 200 with paginated student list")
    void testListStudentsSuccess() throws Exception {
        when(studentService.listStudents(any())).thenReturn(new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/students?page=0&size=20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studentNumber").value("STU2026001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/students/{id} returns 200 on valid update")
    void testUpdateStudentSuccess() throws Exception {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "Srijan", "Bojja", LocalDate.of(2005, 8, 15),
                "9876543210", "CSE", "B.Tech CSE", 3, "B"
        );

        when(studentService.updateStudent(eq(1L), any(UpdateStudentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/v1/students/{id}/status returns 200 on valid status change")
    void testUpdateStudentStatusSuccess() throws Exception {
        UpdateStudentStatusRequest request = new UpdateStudentStatusRequest(StudentStatus.GRADUATED);
        sampleResponse.setStatus(StudentStatus.GRADUATED);

        when(studentService.updateStudentStatus(eq(1L), any(UpdateStudentStatusRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/students/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADUATED"));
    }
}
