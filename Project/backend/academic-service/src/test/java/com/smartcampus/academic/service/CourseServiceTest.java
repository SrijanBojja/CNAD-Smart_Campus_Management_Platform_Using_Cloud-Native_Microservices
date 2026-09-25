package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseStatusRequest;
import com.smartcampus.academic.dto.response.CourseResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.service.impl.CourseServiceImpl;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = new Course(
                "CSE-BTECH",
                "Bachelor of Technology in Computer Science",
                "4-year undergraduate degree",
                "Computer Science",
                4,
                CourseStatus.ACTIVE
        );
        sampleCourse.setId(1L);
    }

    @Test
    @DisplayName("Create course successfully")
    void testCreateCourseSuccess() {
        CreateCourseRequest request = new CreateCourseRequest(
                "CSE-BTECH", "Bachelor of Technology in Computer Science",
                "4-year undergraduate degree", "Computer Science", 4
        );

        when(courseRepository.existsByCourseCode("CSE-BTECH")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse response = courseService.createCourse(request);

        assertThat(response).isNotNull();
        assertThat(response.getCourseCode()).isEqualTo("CSE-BTECH");
        assertThat(response.getStatus()).isEqualTo(CourseStatus.ACTIVE);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Create course fails on duplicate courseCode")
    void testCreateCourseDuplicate() {
        CreateCourseRequest request = new CreateCourseRequest(
                "CSE-BTECH", "BTech", "Desc", "CSE", 4
        );

        when(courseRepository.existsByCourseCode("CSE-BTECH")).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Get course by ID successfully")
    void testGetCourseByIdSuccess() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        CourseResponse response = courseService.getCourseById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCourseName()).isEqualTo("Bachelor of Technology in Computer Science");
    }

    @Test
    @DisplayName("Get course by ID not found")
    void testGetCourseByIdNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("List courses with pagination")
    void testListCourses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(sampleCourse), pageable, 1);
        when(courseRepository.findAll(pageable)).thenReturn(page);

        Page<CourseResponse> result = courseService.listCourses(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseCode()).isEqualTo("CSE-BTECH");
    }

    @Test
    @DisplayName("Update course status successfully")
    void testUpdateCourseStatus() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        UpdateCourseStatusRequest request = new UpdateCourseStatusRequest(CourseStatus.INACTIVE);
        CourseResponse response = courseService.updateCourseStatus(1L, request);

        assertThat(response).isNotNull();
        verify(courseRepository).save(sampleCourse);
    }
}
