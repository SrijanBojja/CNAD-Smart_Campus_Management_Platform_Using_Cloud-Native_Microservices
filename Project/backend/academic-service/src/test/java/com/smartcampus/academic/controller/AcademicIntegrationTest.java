package com.smartcampus.academic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.academic.client.AuthServiceClient;
import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.dto.request.*;
import com.smartcampus.academic.entity.*;
import com.smartcampus.academic.repository.*;
import com.smartcampus.academic.security.JwtTokenProvider;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private AcademicRecordRepository academicRecordRepository;

    @Autowired
    private ClassScheduleRepository scheduleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StudentServiceClient studentServiceClient;

    @MockBean
    private AuthServiceClient authServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Course course1;
    private Subject subject1;
    private CourseEnrollment enrollment1;
    private AcademicRecord record1;

    private String createBearerToken(Long userId, String username, String role) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, username, username + "@smartcampus.edu", List.of(role));
    }

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        academicRecordRepository.deleteAll();
        enrollmentRepository.deleteAll();
        subjectRepository.deleteAll();
        courseRepository.deleteAll();

        course1 = new Course("CSE-BTECH", "Bachelor of Technology in CSE", "Desc", "Computer Science", 4, CourseStatus.ACTIVE);
        course1 = courseRepository.save(course1);

        subject1 = new Subject(course1, "CS301", "Data Structures", 4, 3, 201L);
        subject1 = subjectRepository.save(subject1);

        enrollment1 = new CourseEnrollment(course1, 50L, "2026-2027", 3, EnrollmentStatus.ACTIVE);
        enrollment1 = enrollmentRepository.save(enrollment1);

        record1 = new AcademicRecord(50L, subject1, "2026-2027", 3,
                new BigDecimal("28.00"), new BigDecimal("65.00"), new BigDecimal("93.00"),
                "A+", new BigDecimal("10.00"), ResultStatus.PASS);
        record1 = academicRecordRepository.save(record1);

        // Mock studentServiceClient to return student info for studentId 50L (userId 101L) and 51L (userId 102L)
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));

        when(studentServiceClient.getStudentById(eq(51L), any()))
                .thenReturn(new StudentResponseDto(51L, 102L, "STU2026002", "Bob", "Jones", "ACTIVE"));
    }

    @Test
    @DisplayName("1. Get course by ID as STUDENT -> 200 OK")
    void testGetCourseAsStudent() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + course1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CSE-BTECH"));
    }

    @Test
    @DisplayName("2. Create course as ADMIN -> 201 Created")
    void testCreateCourseAsAdmin() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("ECE-BTECH", "B.Tech ECE", "Electronics", "ECE", 4);

        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("ECE-BTECH"));
    }

    @Test
    @DisplayName("3. Create course as FACULTY -> 403 Forbidden")
    void testCreateCourseAsFacultyForbidden() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest("ME-BTECH", "B.Tech ME", "Mechanical", "ME", 4);

        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Create subject as FACULTY -> 201 Created")
    void testCreateSubjectAsFaculty() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest(course1.getId(), "CS302", "Algorithms", 4, 3, 201L);

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectCode").value("CS302"));
    }

    @Test
    @DisplayName("5. Create subject as STUDENT -> 403 Forbidden")
    void testCreateSubjectAsStudentForbidden() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest(course1.getId(), "CS303", "OS", 4, 4, 201L);

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Create enrollment as ADMIN with valid student -> 201 Created")
    void testCreateEnrollmentAsAdmin() throws Exception {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest(course1.getId(), 51L, "2026-2027", 3, EnrollmentStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(51L));
    }

    @Test
    @DisplayName("7. Duplicate enrollment returns 409 Conflict")
    void testCreateDuplicateEnrollment() throws Exception {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest(course1.getId(), 50L, "2026-2027", 3, EnrollmentStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("8. Student can access their own enrollment -> 200 OK")
    void testStudentAccessOwnEnrollment() throws Exception {
        // Alice has userId 101L which maps to studentId 50L (enrollment1)
        mockMvc.perform(get("/api/v1/enrollments/" + enrollment1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrollment1.getId()))
                .andExpect(jsonPath("$.studentId").value(50L));
    }

    @Test
    @DisplayName("9. Student cannot access another student's enrollment -> 403 Forbidden")
    void testStudentCannotAccessOtherEnrollment() throws Exception {
        // Bob has userId 102L (studentId 51L), tries to access enrollment1 (studentId 50L)
        mockMvc.perform(get("/api/v1/enrollments/" + enrollment1.getId())
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("10. Student can access their own academic record -> 200 OK")
    void testStudentAccessOwnAcademicRecord() throws Exception {
        mockMvc.perform(get("/api/v1/academic-records/" + record1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record1.getId()))
                .andExpect(jsonPath("$.grade").value("A+"));
    }

    @Test
    @DisplayName("11. Student cannot access another student's academic record -> 403 Forbidden")
    void testStudentCannotAccessOtherAcademicRecord() throws Exception {
        mockMvc.perform(get("/api/v1/academic-records/" + record1.getId())
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("12. Create schedule as FACULTY -> 201 Created")
    void testCreateScheduleAsFaculty() throws Exception {
        CreateScheduleRequest request = new CreateScheduleRequest(
                subject1.getId(), 201L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room-302", "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/schedules")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("Room-302"));
    }

    @Test
    @DisplayName("13. Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + course1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
