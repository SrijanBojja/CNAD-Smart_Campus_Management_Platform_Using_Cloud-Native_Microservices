package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateSubjectRequest;
import com.smartcampus.academic.dto.request.UpdateSubjectRequest;
import com.smartcampus.academic.dto.response.SubjectResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.entity.Subject;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.service.impl.SubjectServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Course sampleCourse;
    private Subject sampleSubject;

    @BeforeEach
    void setUp() {
        sampleCourse = new Course("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE);
        sampleCourse.setId(1L);

        sampleSubject = new Subject(sampleCourse, "CS301", "Data Structures", 4, 3, 201L);
        sampleSubject.setId(10L);
    }

    @Test
    @DisplayName("Create subject successfully")
    void testCreateSubjectSuccess() {
        CreateSubjectRequest request = new CreateSubjectRequest(1L, "CS301", "Data Structures", 4, 3, 201L);

        when(subjectRepository.existsBySubjectCode("CS301")).thenReturn(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(subjectRepository.save(any(Subject.class))).thenReturn(sampleSubject);

        SubjectResponse response = subjectService.createSubject(request);

        assertThat(response).isNotNull();
        assertThat(response.getSubjectCode()).isEqualTo("CS301");
        assertThat(response.getCourseId()).isEqualTo(1L);
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    @DisplayName("Create subject fails on duplicate code")
    void testCreateSubjectDuplicate() {
        CreateSubjectRequest request = new CreateSubjectRequest(1L, "CS301", "Data Structures", 4, 3, 201L);
        when(subjectRepository.existsBySubjectCode("CS301")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.createSubject(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Get subject by ID successfully")
    void testGetSubjectByIdSuccess() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));

        SubjectResponse response = subjectService.getSubjectById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSubjectName()).isEqualTo("Data Structures");
    }

    @Test
    @DisplayName("Delete subject successfully")
    void testDeleteSubject() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));

        subjectService.deleteSubject(10L);

        verify(subjectRepository).delete(sampleSubject);
    }
}
