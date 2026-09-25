package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateScheduleRequest;
import com.smartcampus.academic.dto.response.ScheduleResponse;
import com.smartcampus.academic.entity.ClassSchedule;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.entity.Subject;
import com.smartcampus.academic.exception.BadRequestException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.ClassScheduleRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ClassScheduleRepository scheduleRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Subject sampleSubject;
    private ClassSchedule sampleSchedule;

    @BeforeEach
    void setUp() {
        Course course = new Course("CSE-BTECH", "B.Tech CSE", "Desc", "CSE", 4, CourseStatus.ACTIVE);
        sampleSubject = new Subject(course, "CS301", "Data Structures", 4, 3, 201L);
        sampleSubject.setId(10L);

        sampleSchedule = new ClassSchedule(sampleSubject, 201L, "MONDAY",
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302", "2026-2027", 3, "A");
        sampleSchedule.setId(300L);
    }

    @Test
    @DisplayName("Create class schedule successfully")
    void testCreateScheduleSuccess() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                10L, 201L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Room-302", "2026-2027", 3, "A"
        );

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));
        when(scheduleRepository.save(any(ClassSchedule.class))).thenReturn(sampleSchedule);

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertThat(response).isNotNull();
        assertThat(response.getSubjectId()).isEqualTo(10L);
        assertThat(response.getDayOfWeek()).isEqualTo("MONDAY");
        verify(scheduleRepository).save(any(ClassSchedule.class));
    }

    @Test
    @DisplayName("Create schedule fails when start time is after end time")
    void testCreateScheduleInvalidTimes() {
        CreateScheduleRequest request = new CreateScheduleRequest(
                10L, 201L, "MONDAY", LocalTime.of(11, 0), LocalTime.of(10, 0),
                "Room-302", "2026-2027", 3, "A"
        );

        assertThatThrownBy(() -> scheduleService.createSchedule(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    @DisplayName("Get schedule by ID successfully")
    void testGetScheduleById() {
        when(scheduleRepository.findById(300L)).thenReturn(Optional.of(sampleSchedule));

        ScheduleResponse response = scheduleService.getScheduleById(300L);

        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getRoomNumber()).isEqualTo("Room-302");
    }

    @Test
    @DisplayName("Delete schedule successfully")
    void testDeleteSchedule() {
        when(scheduleRepository.findById(300L)).thenReturn(Optional.of(sampleSchedule));

        scheduleService.deleteSchedule(300L);

        verify(scheduleRepository).delete(sampleSchedule);
    }
}
