package com.smartcampus.academic.service;

import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.dto.request.CreateAcademicRecordRequest;
import com.smartcampus.academic.dto.response.AcademicRecordResponse;
import com.smartcampus.academic.entity.*;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.AcademicRecordRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.impl.AcademicRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicRecordServiceTest {

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @InjectMocks
    private AcademicRecordServiceImpl academicRecordService;

    private Subject sampleSubject;
    private AcademicRecord sampleRecord;
    private StudentResponseDto sampleStudentDto;

    @BeforeEach
    void setUp() {
        Course course = new Course("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE);
        sampleSubject = new Subject(course, "CS301", "Data Structures", 4, 3, 201L);
        sampleSubject.setId(10L);

        sampleRecord = new AcademicRecord(50L, sampleSubject, "2026-2027", 3,
                new BigDecimal("28.50"), new BigDecimal("65.00"), new BigDecimal("93.50"),
                "A+", new BigDecimal("10.00"), ResultStatus.PASS);
        sampleRecord.setId(200L);

        sampleStudentDto = new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE");
    }

    @Test
    @DisplayName("Create academic record successfully")
    void testCreateRecordSuccess() {
        CreateAcademicRecordRequest request = new CreateAcademicRecordRequest(
                50L, 10L, "2026-2027", 3, new BigDecimal("28.50"), new BigDecimal("65.00"),
                "A+", new BigDecimal("10.00"), ResultStatus.PASS
        );

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);
        when(academicRecordRepository.existsByStudentIdAndSubjectIdAndAcademicYearAndSemester(50L, 10L, "2026-2027", 3)).thenReturn(false);
        when(academicRecordRepository.save(any(AcademicRecord.class))).thenReturn(sampleRecord);

        AcademicRecordResponse response = academicRecordService.createAcademicRecord(request, "mock.token");

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(50L);
        assertThat(response.getSubjectId()).isEqualTo(10L);
        verify(academicRecordRepository).save(any(AcademicRecord.class));
    }

    @Test
    @DisplayName("Create academic record fails on duplicate")
    void testCreateRecordDuplicate() {
        CreateAcademicRecordRequest request = new CreateAcademicRecordRequest(
                50L, 10L, "2026-2027", 3, new BigDecimal("28.50"), new BigDecimal("65.00"),
                "A+", new BigDecimal("10.00"), ResultStatus.PASS
        );

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);
        when(academicRecordRepository.existsByStudentIdAndSubjectIdAndAcademicYearAndSemester(50L, 10L, "2026-2027", 3)).thenReturn(true);

        assertThatThrownBy(() -> academicRecordService.createAcademicRecord(request, "mock.token"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Student can view own academic record")
    void testStudentViewOwnRecord() {
        UserPrincipal principal = new UserPrincipal(101L, "alice", "alice@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(academicRecordRepository.findById(200L)).thenReturn(Optional.of(sampleRecord));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);

        AcademicRecordResponse response = academicRecordService.getAcademicRecordById(200L, principal, "mock.token");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Student cannot view other student's academic record -> AccessDeniedException")
    void testStudentCannotViewOtherRecord() {
        UserPrincipal principal = new UserPrincipal(202L, "bob", "bob@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        when(academicRecordRepository.findById(200L)).thenReturn(Optional.of(sampleRecord));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(sampleStudentDto);

        assertThatThrownBy(() -> academicRecordService.getAcademicRecordById(200L, principal, "mock.token"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
