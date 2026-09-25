package com.smartcampus.academic.controller;

import com.smartcampus.academic.dto.request.CreateEnrollmentRequest;
import com.smartcampus.academic.dto.request.UpdateEnrollmentRequest;
import com.smartcampus.academic.dto.response.EnrollmentResponse;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollment Management", description = "Student course enrollment management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "List Enrollments", description = "Retrieve paginated course enrollments. Restricted to ADMIN and FACULTY.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Page<EnrollmentResponse>> listEnrollments(
            @RequestParam(name = "studentId", required = false) Long studentId,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "academicYear", required = false) String academicYear,
            @RequestParam(name = "semester", required = false) Integer semester,
            @PageableDefault(page = 0, size = 20, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<EnrollmentResponse> response = enrollmentService.listEnrollments(studentId, courseId, academicYear, semester, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Enrollment by ID", description = "Retrieve enrollment by ID. ADMIN and FACULTY can view any enrollment. STUDENT can only view their own.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(
            @PathVariable("id") Long id,
            Authentication authentication,
            HttpServletRequest request) {
        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        String bearerToken = request.getHeader("Authorization");
        EnrollmentResponse response = enrollmentService.getEnrollmentById(id, principal, bearerToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Enrollment", description = "Enroll a student in a course. Restricted to ADMIN.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody CreateEnrollmentRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        EnrollmentResponse response = enrollmentService.createEnrollment(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Enrollment", description = "Update enrollment status/details. Restricted to ADMIN.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> updateEnrollment(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateEnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.updateEnrollment(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Enrollment", description = "Cancel/delete enrollment record. Restricted to ADMIN.")
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable("id") Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}
