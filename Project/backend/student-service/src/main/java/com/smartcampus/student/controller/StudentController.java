package com.smartcampus.student.controller;

import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.dto.response.ErrorResponse;
import com.smartcampus.student.dto.response.StudentResponse;
import com.smartcampus.student.security.UserPrincipal;
import com.smartcampus.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/v1/students")
@Tag(name = "Student Management", description = "Student profile lifecycle and academic identity management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(
            summary = "List Students",
            description = "Retrieve a paginated list of students. Restricted to ADMIN and FACULTY roles."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of students retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Caller lacks ADMIN or FACULTY role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Page<StudentResponse>> listStudents(
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StudentResponse> page = studentService.listStudents(pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(
            summary = "Get Student by ID",
            description = "Retrieve student details by ID. ADMIN and FACULTY can view any student. A STUDENT can only view their own profile."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Student profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Student attempted to access another student's record",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<StudentResponse> getStudentById(
            @Parameter(description = "Primary key ID of the student record", required = true)
            @PathVariable("id") Long id,
            Authentication authentication) {
        UserPrincipal principal = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) {
            principal = up;
        }

        StudentResponse response = studentService.getStudentById(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create Student Profile",
            description = "Create a new student profile. Restricted to ADMIN. Validates user existence, status, and role with Auth Service."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Student profile created successfully",
                    content = @Content(schema = @Schema(implementation = StudentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or Auth user is inactive / lacks STUDENT role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Caller lacks ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Duplicate userId or studentNumber",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Downstream Auth Service error or internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        StudentResponse response = studentService.createStudent(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update Student Profile",
            description = "Update mutable profile details of an existing student. Restricted to ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Student profile updated successfully",
                    content = @Content(schema = @Schema(implementation = StudentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure on request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Caller lacks ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> updateStudent(
            @Parameter(description = "Primary key ID of the student record", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update Student Lifecycle Status",
            description = "Update the active status of a student (ACTIVE, INACTIVE, SUSPENDED, GRADUATED). Restricted to ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Student status updated successfully",
                    content = @Content(schema = @Schema(implementation = StudentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing status value",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Caller lacks ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> updateStudentStatus(
            @Parameter(description = "Primary key ID of the student record", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentStatusRequest request) {
        StudentResponse response = studentService.updateStudentStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
