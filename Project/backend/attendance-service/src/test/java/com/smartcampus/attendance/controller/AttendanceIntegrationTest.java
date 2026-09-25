package com.smartcampus.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.attendance.client.AcademicServiceClient;
import com.smartcampus.attendance.client.AuthServiceClient;
import com.smartcampus.attendance.client.StudentServiceClient;
import com.smartcampus.attendance.client.dto.StudentResponseDto;
import com.smartcampus.attendance.client.dto.SubjectResponseDto;
import com.smartcampus.attendance.client.dto.UserValidationResponseDto;
import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceRecordRequest;
import com.smartcampus.attendance.entity.AttendanceRecord;
import com.smartcampus.attendance.entity.AttendanceSession;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.repository.AttendanceRecordRepository;
import com.smartcampus.attendance.repository.AttendanceSessionRepository;
import com.smartcampus.attendance.security.JwtTokenProvider;
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

import java.time.LocalDate;
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
class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StudentServiceClient studentServiceClient;

    @MockBean
    private AcademicServiceClient academicServiceClient;

    @MockBean
    private AuthServiceClient authServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AttendanceSession session1;
    private AttendanceRecord record1;

    private String createBearerToken(Long userId, String username, String role) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, username, username + "@smartcampus.edu", List.of(role));
    }

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        sessionRepository.deleteAll();

        session1 = new AttendanceSession(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );
        session1 = sessionRepository.save(session1);

        record1 = new AttendanceRecord(session1, 50L, AttendanceStatus.PRESENT, "On time");
        record1 = recordRepository.save(record1);

        // Mock external client calls
        when(academicServiceClient.getSubjectById(eq(10L), any()))
                .thenReturn(new SubjectResponseDto(10L, 1L, "CS301", "Data Structures", 4, 3, 201L));

        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, true, true));

        // Alice (studentId 50L, userId 101L)
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));

        // Bob (studentId 51L, userId 102L)
        when(studentServiceClient.getStudentById(eq(51L), any()))
                .thenReturn(new StudentResponseDto(51L, 102L, "STU2026002", "Bob", "Jones", "ACTIVE"));
    }

    @Test
    @DisplayName("1. Unauthenticated request to sessions returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/sessions/" + session1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("2. Student can retrieve session details -> 200 OK")
    void testGetSessionAsStudent() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/sessions/" + session1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session1.getId()))
                .andExpect(jsonPath("$.subjectId").value(10L));
    }

    @Test
    @DisplayName("3. Create session as FACULTY -> 201 Created")
    void testCreateSessionAsFaculty() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 26),
                LocalTime.of(11, 0), LocalTime.of(12, 30), "Room-305",
                "2026-2027", 3, "B"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("Room-305"));
    }

    @Test
    @DisplayName("4. Create session as ADMIN -> 201 Created")
    void testCreateSessionAsAdmin() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 27),
                LocalTime.of(14, 0), LocalTime.of(15, 30), "Room-308",
                "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("Room-308"));
    }

    @Test
    @DisplayName("5. Create session as STUDENT -> 403 Forbidden")
    void testCreateSessionAsStudentForbidden() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 28),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("6. Create session with invalid time range -> 400 Bad Request")
    void testCreateSessionInvalidTime() throws Exception {
        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 28),
                LocalTime.of(12, 0), LocalTime.of(10, 0), "Room-302",
                "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("7. Create session with missing subject in Academic Service -> 404 Not Found")
    void testCreateSessionSubjectNotFound() throws Exception {
        when(academicServiceClient.getSubjectById(eq(999L), any())).thenReturn(null);

        CreateAttendanceSessionRequest request = new CreateAttendanceSessionRequest(
                999L, 201L, LocalDate.of(2026, 9, 28),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("8. Create attendance record as FACULTY for student 51 -> 201 Created")
    void testCreateRecordAsFaculty() throws Exception {
        CreateAttendanceRecordRequest request = new CreateAttendanceRecordRequest(
                session1.getId(), 51L, AttendanceStatus.PRESENT, "Present on time"
        );

        mockMvc.perform(post("/api/v1/attendance/records")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(51L))
                .andExpect(jsonPath("$.status").value("PRESENT"));
    }

    @Test
    @DisplayName("9. Duplicate attendance record in same session -> 409 Conflict")
    void testCreateDuplicateRecord() throws Exception {
        CreateAttendanceRecordRequest request = new CreateAttendanceRecordRequest(
                session1.getId(), 50L, AttendanceStatus.PRESENT, "Duplicate attempt"
        );

        mockMvc.perform(post("/api/v1/attendance/records")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("10. Student can access their own attendance record -> 200 OK")
    void testStudentAccessOwnAttendanceRecord() throws Exception {
        // Alice has userId 101L -> studentId 50L (record1)
        mockMvc.perform(get("/api/v1/attendance/records/" + record1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record1.getId()))
                .andExpect(jsonPath("$.studentId").value(50L));
    }

    @Test
    @DisplayName("11. Student cannot access another student's attendance record -> 403 Forbidden")
    void testStudentCannotAccessOtherAttendanceRecord() throws Exception {
        // Bob has userId 102L -> studentId 51L, tries to access record1 (studentId 50L)
        mockMvc.perform(get("/api/v1/attendance/records/" + record1.getId())
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("12. Student cannot retrieve all attendance records -> 403 Forbidden")
    void testStudentCannotListAttendanceRecords() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/records")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("13. Update attendance record as FACULTY -> 200 OK")
    void testUpdateRecordAsFaculty() throws Exception {
        UpdateAttendanceRecordRequest request = new UpdateAttendanceRecordRequest(
                AttendanceStatus.EXCUSED, "Doctor note verified"
        );

        mockMvc.perform(put("/api/v1/attendance/records/" + record1.getId())
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCUSED"))
                .andExpect(jsonPath("$.remarks").value("Doctor note verified"));
    }

    @Test
    @DisplayName("14. Delete attendance session as ADMIN -> 204 No Content")
    void testDeleteSessionAsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/attendance/sessions/" + session1.getId())
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }
}
