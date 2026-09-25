package com.smartcampus.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.student.client.AuthServiceClient;
import com.smartcampus.student.client.dto.UserValidationResponse;
import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.entity.Student;
import com.smartcampus.student.entity.StudentStatus;
import com.smartcampus.student.repository.StudentRepository;
import com.smartcampus.student.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private com.smartcampus.student.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthServiceClient authServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();

        student1 = new Student(
                101L,
                "STU2026001",
                "Alice",
                "Smith",
                LocalDate.of(2005, 5, 20),
                "9876543211",
                "Computer Science",
                "B.Tech CSE",
                2,
                "A",
                StudentStatus.ACTIVE
        );
        student1 = studentRepository.save(student1);

        student2 = new Student(
                102L,
                "STU2026002",
                "Bob",
                "Jones",
                LocalDate.of(2004, 11, 10),
                "9876543212",
                "Electronics",
                "B.Tech ECE",
                3,
                "B",
                StudentStatus.ACTIVE
        );
        student2 = studentRepository.save(student2);
    }

    private String createBearerToken(Long userId, String username, String role) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, username, username + "@smartcampus.edu", List.of(role));
    }

    @Test
    @DisplayName("1. Create student successfully as ADMIN")
    void testCreateStudentSuccess() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                103L, "STU2026003", "Charlie", "Brown",
                LocalDate.of(2005, 1, 1), "9876543213", "Mechanical", "B.Tech ME", 1, "A"
        );

        when(authServiceClient.validateUser(eq(103L), eq("STUDENT"), any()))
                .thenReturn(new UserValidationResponse(103L, true, true));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(103L))
                .andExpect(jsonPath("$.studentNumber").value("STU2026003"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("2. Create student fails when Auth user is inactive")
    void testCreateStudentInactiveUser() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                104L, "STU2026004", "David", "Miller",
                LocalDate.of(2005, 2, 2), "9876543214", "Civil", "B.Tech CE", 1, "A"
        );

        when(authServiceClient.validateUser(eq(104L), eq("STUDENT"), any()))
                .thenReturn(new UserValidationResponse(104L, false, true));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("3. Create student fails when Auth user lacks STUDENT role")
    void testCreateStudentRoleMismatch() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                105L, "STU2026005", "Emma", "Watson",
                LocalDate.of(2005, 3, 3), "9876543215", "CSE", "B.Tech CSE", 1, "A"
        );

        when(authServiceClient.validateUser(eq(105L), eq("STUDENT"), any()))
                .thenReturn(new UserValidationResponse(105L, true, false));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("4. Duplicate user_id returns 409 Conflict")
    void testCreateStudentDuplicateUserId() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                101L, "STU2026099", "Duplicate", "User",
                LocalDate.of(2005, 1, 1), "9876543210", "CSE", "B.Tech", 1, "A"
        );

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("5. Duplicate student_number returns 409 Conflict")
    void testCreateStudentDuplicateStudentNumber() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                199L, "STU2026001", "Duplicate", "Number",
                LocalDate.of(2005, 1, 1), "9876543210", "CSE", "B.Tech", 1, "A"
        );

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("6. Get student successfully as ADMIN")
    void testGetStudentAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + student1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student1.getId()))
                .andExpect(jsonPath("$.studentNumber").value("STU2026001"));
    }

    @Test
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    @DisplayName("7. Get student successfully as FACULTY")
    void testGetStudentAsFaculty() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + student1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student1.getId()));
    }

    @Test
    @DisplayName("8. Student can access their own student record")
    void testStudentAccessOwnRecord() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + student1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101L));
    }

    @Test
    @DisplayName("9. Student cannot access another student's record -> 403 Forbidden")
    void testStudentCannotAccessOtherRecord() throws Exception {
        // Alice (userId 101) tries to access Bob's record (student2, userId 102)
        mockMvc.perform(get("/api/v1/students/" + student2.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("10. Non-authenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + student1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    @DisplayName("11. Non-ADMIN (FACULTY) cannot create student -> 403 Forbidden")
    void testFacultyCannotCreateStudent() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest(
                106L, "STU2026006", "Test", "User",
                LocalDate.of(2005, 1, 1), "9876543210", "CSE", "B.Tech", 1, "A"
        );

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("12. Non-ADMIN (STUDENT) cannot list all students -> 403 Forbidden")
    void testStudentCannotListStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("13. Update student profile as ADMIN -> 200 OK")
    void testUpdateStudentAdmin() throws Exception {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "Alice Updated", "Smith Updated", LocalDate.of(2005, 5, 20),
                "9999999999", "AI & ML", "B.Tech AIML", 3, "C"
        );

        mockMvc.perform(put("/api/v1/students/" + student1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice Updated"))
                .andExpect(jsonPath("$.department").value("AI & ML"))
                .andExpect(jsonPath("$.yearOfStudy").value(3));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("14. Update student status as ADMIN -> 200 OK")
    void testUpdateStudentStatusAdmin() throws Exception {
        UpdateStudentStatusRequest request = new UpdateStudentStatusRequest(StudentStatus.SUSPENDED);

        mockMvc.perform(patch("/api/v1/students/" + student1.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("15. Student list supports pagination -> 200 OK")
    void testListStudentsPagination() throws Exception {
        mockMvc.perform(get("/api/v1/students?page=0&size=1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
