package com.smartcampus.academic.controller;

import com.smartcampus.academic.dto.request.CreateAcademicRecordRequest;
import com.smartcampus.academic.dto.request.UpdateAcademicRecordRequest;
import com.smartcampus.academic.dto.response.AcademicRecordResponse;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.AcademicRecordService;
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
@RequestMapping("/api/v1/academic-records")
@Tag(name = "Academic Records Management", description = "Student marks, grades, and academic performance records")
@SecurityRequirement(name = "BearerAuth")
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;

    public AcademicRecordController(AcademicRecordService academicRecordService) {
        this.academicRecordService = academicRecordService;
    }

    @Operation(summary = "List Academic Records", description = "Retrieve paginated academic records. Restricted to ADMIN and FACULTY.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Page<AcademicRecordResponse>> listAcademicRecords(
            @RequestParam(name = "studentId", required = false) Long studentId,
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "academicYear", required = false) String academicYear,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AcademicRecordResponse> response = academicRecordService.listAcademicRecords(studentId, subjectId, semester, academicYear, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Academic Record by ID", description = "Retrieve academic record by ID. ADMIN and FACULTY can view any. STUDENT can only view their own.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<AcademicRecordResponse> getAcademicRecordById(
            @PathVariable("id") Long id,
            Authentication authentication,
            HttpServletRequest request) {
        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        String bearerToken = request.getHeader("Authorization");
        AcademicRecordResponse response = academicRecordService.getAcademicRecordById(id, principal, bearerToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Academic Record", description = "Enter student marks/grades. Restricted to ADMIN and FACULTY.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AcademicRecordResponse> createAcademicRecord(
            @Valid @RequestBody CreateAcademicRecordRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        AcademicRecordResponse response = academicRecordService.createAcademicRecord(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Academic Record", description = "Update student marks/grades. Restricted to ADMIN and FACULTY.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AcademicRecordResponse> updateAcademicRecord(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAcademicRecordRequest request) {
        AcademicRecordResponse response = academicRecordService.updateAcademicRecord(id, request);
        return ResponseEntity.ok(response);
    }
}
