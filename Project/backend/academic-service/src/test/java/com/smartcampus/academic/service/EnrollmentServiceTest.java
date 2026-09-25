package com.smartcampus.academic.service;

import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.dto.request.CreateEnrollmentRequest;
import com.smartcampus.academic.dto.response.EnrollmentResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseEnrollment;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.entity.EnrollmentStatus;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseEnrollmentRepository;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Course sampleCourse;
    private CourseEnrollment sampleEnrollment;
    private StudentResponseDto sampleStudentDto;

    @BeforeEach
    void setUp() {
        sampleCourse = new Course("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE);
        sampleCourse.setId(1L);

        sampleEnrollment = new CourseEnrollment(sampleCourse, 50L, "2026-2027", 3, EnrollmentStatus.ACTIVE);
        sampleEnrollment.setId(100L);

        sampleStudentDto = new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE");
    }

    @Test
    @DisplayName("Create enrollment successfully")
    void testCreateEnrollmentSuccess() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest(1L, 50L, "2026-2027", 3, EnrollmentStatus.ACTIVE);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);
        when(enrollmentRepository.existsByCourseIdAndStudentIdAndAcademicYearAndSemester(1L, 50L, "2026-2027", 3)).thenReturn(false);
        when(enrollmentRepository.save(any(CourseEnrollment.class))).thenReturn(sampleEnrollment);

        EnrollmentResponse response = enrollmentService.createEnrollment(request, "mock.token");

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(50L);
        assertThat(response.getCourseId()).isEqualTo(1L);
        verify(enrollmentRepository).save(any(CourseEnrollment.class));
    }

    @Test
    @DisplayName("Create enrollment fails when student does not exist")
    void testCreateEnrollmentStudentNotFound() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest(1L, 99L, "2026-2027", 3, EnrollmentStatus.ACTIVE);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(studentServiceClient.getStudentById(eq(99L), any())).thenReturn(null);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request, "mock.token"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    @DisplayName("Create enrollment fails on duplicate")
    void testCreateEnrollmentDuplicate() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest(1L, 50L, "2026-2027", 3, EnrollmentStatus.ACTIVE);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);
        when(enrollmentRepository.existsByCourseIdAndStudentIdAndAcademicYearAndSemester(1L, 50L, "2026-2027", 3)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request, "mock.token"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    @DisplayName("Student can view own enrollment")
    void testStudentViewOwnEnrollment() {
        UserPrincipal principal = new UserPrincipal(101L, "alice", "alice@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(sampleEnrollment));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(100L, principal, "mock.token");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Student cannot view other student's enrollment -> AccessDeniedException")
    void testStudentCannotViewOtherEnrollment() {
        // Principal has userId 202L, but enrollment belongs to student with userId 101L
        UserPrincipal principal = new UserPrincipal(202L, "bob", "bob@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(sampleEnrollment));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);

        assertThatThrownBy(() -> enrollmentService.getEnrollmentById(100L, principal, "mock.token"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
