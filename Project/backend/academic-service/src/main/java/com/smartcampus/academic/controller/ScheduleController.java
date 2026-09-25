package com.smartcampus.academic.controller;

import com.smartcampus.academic.dto.request.CreateScheduleRequest;
import com.smartcampus.academic.dto.request.UpdateScheduleRequest;
import com.smartcampus.academic.dto.response.ScheduleResponse;
import com.smartcampus.academic.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Class Schedule Management", description = "Timetable and class schedule management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "List Class Schedules", description = "Retrieve paginated timetable/schedules. Accessible by ADMIN, FACULTY, STUDENT.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Page<ScheduleResponse>> listSchedules(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "facultyUserId", required = false) Long facultyUserId,
            @RequestParam(name = "dayOfWeek", required = false) String dayOfWeek,
            @RequestParam(name = "academicYear", required = false) String academicYear,
            @RequestParam(name = "semester", required = false) Integer semester,
            @PageableDefault(page = 0, size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ScheduleResponse> response = scheduleService.listSchedules(subjectId, facultyUserId, dayOfWeek, academicYear, semester, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Class Schedule by ID", description = "Retrieve class schedule by ID. Accessible by ADMIN, FACULTY, STUDENT.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<ScheduleResponse> getScheduleById(@PathVariable("id") Long id) {
        ScheduleResponse response = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Class Schedule", description = "Create schedule slot. Restricted to ADMIN and FACULTY.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Class Schedule", description = "Update schedule slot. Restricted to ADMIN and FACULTY.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateScheduleRequest request) {
        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Class Schedule", description = "Delete schedule slot. Restricted to ADMIN and FACULTY.")
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable("id") Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
