package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateScheduleRequest;
import com.smartcampus.academic.dto.request.UpdateScheduleRequest;
import com.smartcampus.academic.dto.response.ScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScheduleService {

    ScheduleResponse createSchedule(CreateScheduleRequest request);

    ScheduleResponse getScheduleById(Long id);

    Page<ScheduleResponse> listSchedules(Long subjectId, Long facultyUserId, String dayOfWeek, String academicYear, Integer semester, Pageable pageable);

    ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest request);

    void deleteSchedule(Long id);
}
