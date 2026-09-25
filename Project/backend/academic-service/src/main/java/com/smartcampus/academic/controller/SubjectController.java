package com.smartcampus.academic.controller;

import com.smartcampus.academic.dto.request.CreateSubjectRequest;
import com.smartcampus.academic.dto.request.UpdateSubjectRequest;
import com.smartcampus.academic.dto.response.ErrorResponse;
import com.smartcampus.academic.dto.response.SubjectResponse;
import com.smartcampus.academic.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subject Management", description = "Subject lifecycle and semester-wise academic modules")
@SecurityRequirement(name = "BearerAuth")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Operation(summary = "List Subjects", description = "Retrieve paginated subjects. Accessible by ADMIN, FACULTY, STUDENT.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Page<SubjectResponse>> listSubjects(
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "facultyUserId", required = false) Long facultyUserId,
            @PageableDefault(page = 0, size = 20, sort = "subjectCode", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<SubjectResponse> response = subjectService.listSubjects(courseId, semester, facultyUserId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Subject by ID", description = "Retrieve subject by ID. Accessible by ADMIN, FACULTY, STUDENT.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable("id") Long id) {
        SubjectResponse response = subjectService.getSubjectById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Subject", description = "Create a subject. Restricted to ADMIN and FACULTY.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = subjectService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Subject", description = "Update subject. Restricted to ADMIN and FACULTY.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        SubjectResponse response = subjectService.updateSubject(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Subject", description = "Delete a subject. Restricted to ADMIN.")
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubject(@PathVariable("id") Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
