package com.smartcampus.student.service;

import com.smartcampus.student.client.AuthServiceClient;
import com.smartcampus.student.client.dto.UserValidationResponse;
import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.dto.response.StudentResponse;
import com.smartcampus.student.entity.Student;
import com.smartcampus.student.entity.StudentStatus;
import com.smartcampus.student.exception.*;
import com.smartcampus.student.repository.StudentRepository;
import com.smartcampus.student.security.UserPrincipal;
import com.smartcampus.student.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student sampleStudent;
    private CreateStudentRequest createRequest;
    private final String bearerToken = "Bearer test.jwt.token";

    @BeforeEach
    void setUp() {
        sampleStudent = new Student(
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
                StudentStatus.ACTIVE
        );
        sampleStudent.setId(1L);
        sampleStudent.setCreatedAt(LocalDateTime.now());
        sampleStudent.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateStudentRequest(
                101L,
                "STU2026001",
                "Srijan",
                "Bojja",
                LocalDate.of(2005, 8, 15),
                "9876543210",
                "Computer Science and Engineering",
                "B.Tech CSE",
                2,
                "A"
        );
    }

    @Test
    @DisplayName("Create student succeeds when Auth validation passes and no duplicates exist")
    void testCreateStudentSuccess() {
        when(studentRepository.existsByUserId(101L)).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU2026001")).thenReturn(false);
        when(authServiceClient.validateUser(eq(101L), eq("STUDENT"), eq(bearerToken)))
                .thenReturn(new UserValidationResponse(101L, true, true));
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent);

        StudentResponse response = studentService.createStudent(createRequest, bearerToken);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getUserId());
        assertEquals("STU2026001", response.getStudentNumber());
        assertEquals("Srijan", response.getFirstName());
        assertEquals(StudentStatus.ACTIVE, response.getStatus());

        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Create student throws DuplicateResourceException for duplicate userId")
    void testCreateStudentDuplicateUserId() {
        when(studentRepository.existsByUserId(101L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(createRequest, bearerToken));
        verify(studentRepository, never()).save(any(Student.class));
        verify(authServiceClient, never()).validateUser(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Create student throws DuplicateResourceException for duplicate studentNumber")
    void testCreateStudentDuplicateStudentNumber() {
        when(studentRepository.existsByUserId(101L)).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU2026001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> studentService.createStudent(createRequest, bearerToken));
        verify(studentRepository, never()).save(any(Student.class));
        verify(authServiceClient, never()).validateUser(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Create student throws UserValidationFailedException when Auth user is inactive")
    void testCreateStudentInactiveAuthUser() {
        when(studentRepository.existsByUserId(101L)).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU2026001")).thenReturn(false);
        when(authServiceClient.validateUser(eq(101L), eq("STUDENT"), eq(bearerToken)))
                .thenReturn(new UserValidationResponse(101L, false, true));

        assertThrows(UserValidationFailedException.class, () -> studentService.createStudent(createRequest, bearerToken));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Create student throws UserValidationFailedException when Auth user lacks STUDENT role")
    void testCreateStudentRoleMismatch() {
        when(studentRepository.existsByUserId(101L)).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU2026001")).thenReturn(false);
        when(authServiceClient.validateUser(eq(101L), eq("STUDENT"), eq(bearerToken)))
                .thenReturn(new UserValidationResponse(101L, true, false));

        assertThrows(UserValidationFailedException.class, () -> studentService.createStudent(createRequest, bearerToken));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Create student throws AuthServiceUnavailableException when Auth Service connection fails")
    void testCreateStudentAuthServiceUnavailable() {
        when(studentRepository.existsByUserId(101L)).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU2026001")).thenReturn(false);
        when(authServiceClient.validateUser(eq(101L), eq("STUDENT"), eq(bearerToken)))
                .thenThrow(new AuthServiceUnavailableException("Auth Service unreachable"));

        assertThrows(AuthServiceUnavailableException.class, () -> studentService.createStudent(createRequest, bearerToken));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Get student by ID succeeds for ADMIN caller")
    void testGetStudentByIdAdmin() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        UserPrincipal adminPrincipal = new UserPrincipal(999L, "admin", "admin@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        StudentResponse response = studentService.getStudentById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getUserId());
    }

    @Test
    @DisplayName("Get student by ID succeeds for FACULTY caller")
    void testGetStudentByIdFaculty() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        UserPrincipal facultyPrincipal = new UserPrincipal(888L, "prof_smith", "smith@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));

        StudentResponse response = studentService.getStudentById(1L, facultyPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Get student by ID succeeds for STUDENT caller viewing their own record")
    void testGetStudentByIdOwnProfile() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        UserPrincipal studentPrincipal = new UserPrincipal(101L, "srijan", "srijan@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        StudentResponse response = studentService.getStudentById(1L, studentPrincipal);

        assertNotNull(response);
        assertEquals(101L, response.getUserId());
    }

    @Test
    @DisplayName("Get student by ID throws AccessDeniedException for STUDENT caller viewing another student's record")
    void testGetStudentByIdForbiddenForOtherStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        UserPrincipal otherStudentPrincipal = new UserPrincipal(202L, "other_student", "other@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        assertThrows(AccessDeniedException.class, () -> studentService.getStudentById(1L, otherStudentPrincipal));
    }

    @Test
    @DisplayName("Get student by ID throws ResourceNotFoundException for unknown ID")
    void testGetStudentByIdNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentById(999L, null));
    }

    @Test
    @DisplayName("Update student updates mutable fields successfully")
    void testUpdateStudentSuccess() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent);

        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                "Srijan Updated",
                "Bojja Updated",
                LocalDate.of(2005, 8, 15),
                "9123456780",
                "CSE",
                "B.Tech CSE",
                3,
                "B"
        );

        StudentResponse response = studentService.updateStudent(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Srijan Updated", sampleStudent.getFirstName());
        assertEquals("9123456780", sampleStudent.getPhone());
        assertEquals(3, sampleStudent.getYearOfStudy());
        assertEquals(101L, sampleStudent.getUserId()); // immutable
        assertEquals("STU2026001", sampleStudent.getStudentNumber()); // immutable
    }

    @Test
    @DisplayName("Update student status updates status successfully")
    void testUpdateStudentStatusSuccess() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent);

        UpdateStudentStatusRequest statusRequest = new UpdateStudentStatusRequest(StudentStatus.GRADUATED);

        StudentResponse response = studentService.updateStudentStatus(1L, statusRequest);

        assertNotNull(response);
        assertEquals(StudentStatus.GRADUATED, sampleStudent.getStatus());
    }

    @Test
    @DisplayName("List students returns paginated result")
    void testListStudentsPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Student> studentPage = new PageImpl<>(List.of(sampleStudent), pageable, 1);

        when(studentRepository.findAll(pageable)).thenReturn(studentPage);

        Page<StudentResponse> response = studentService.listStudents(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("STU2026001", response.getContent().get(0).getStudentNumber());
    }
}
