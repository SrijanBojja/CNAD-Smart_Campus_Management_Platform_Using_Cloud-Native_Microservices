package com.smartcampus.academic.service.impl;

import com.smartcampus.academic.dto.request.CreateScheduleRequest;
import com.smartcampus.academic.dto.request.UpdateScheduleRequest;
import com.smartcampus.academic.dto.response.ScheduleResponse;
import com.smartcampus.academic.entity.ClassSchedule;
import com.smartcampus.academic.entity.Subject;
import com.smartcampus.academic.exception.BadRequestException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.ClassScheduleRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private final ClassScheduleRepository scheduleRepository;
    private final SubjectRepository subjectRepository;

    public ScheduleServiceImpl(ClassScheduleRepository scheduleRepository, SubjectRepository subjectRepository) {
        this.scheduleRepository = scheduleRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        log.info("Attempting to create class schedule for subjectId: {}, facultyUserId: {}, day: {}",
                request.getSubjectId(), request.getFacultyUserId(), request.getDayOfWeek());

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        ClassSchedule schedule = new ClassSchedule(
                subject,
                request.getFacultyUserId(),
                request.getDayOfWeek().toUpperCase(),
                request.getStartTime(),
                request.getEndTime(),
                request.getRoomNumber(),
                request.getAcademicYear().trim(),
                request.getSemester(),
                request.getSection()
        );

        ClassSchedule saved = scheduleRepository.save(schedule);
        log.info("Class schedule successfully created with ID: {}", saved.getId());

        return ScheduleResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(Long id) {
        log.debug("Retrieving schedule by ID: {}", id);
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with ID: " + id));
        return ScheduleResponse.fromEntity(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduleResponse> listSchedules(Long subjectId, Long facultyUserId, String dayOfWeek, String academicYear, Integer semester, Pageable pageable) {
        log.debug("Listing schedules with subjectId: {}, facultyUserId: {}, dayOfWeek: {}", subjectId, facultyUserId, dayOfWeek);
        Page<ClassSchedule> page;
        if (subjectId != null) {
            page = scheduleRepository.findBySubjectId(subjectId, pageable);
        } else if (facultyUserId != null) {
            page = scheduleRepository.findByFacultyUserId(facultyUserId, pageable);
        } else if (dayOfWeek != null && !dayOfWeek.isBlank()) {
            page = scheduleRepository.findByDayOfWeek(dayOfWeek.trim().toUpperCase(), pageable);
        } else if (academicYear != null && semester != null) {
            page = scheduleRepository.findByAcademicYearAndSemester(academicYear.trim(), semester, pageable);
        } else {
            page = scheduleRepository.findAll(pageable);
        }
        return page.map(ScheduleResponse::fromEntity);
    }

    @Override
    @Transactional
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest request) {
        log.info("Updating class schedule with ID: {}", id);

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with ID: " + id));

        schedule.setFacultyUserId(request.getFacultyUserId());
        schedule.setDayOfWeek(request.getDayOfWeek().toUpperCase());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoomNumber(request.getRoomNumber());
        schedule.setAcademicYear(request.getAcademicYear().trim());
        schedule.setSemester(request.getSemester());
        schedule.setSection(request.getSection());

        ClassSchedule updated = scheduleRepository.save(schedule);
        log.info("Class schedule successfully updated with ID: {}", updated.getId());

        return ScheduleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        log.info("Deleting class schedule with ID: {}", id);
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class schedule not found with ID: " + id));
        scheduleRepository.delete(schedule);
        log.info("Class schedule successfully deleted with ID: {}", id);
    }
}
