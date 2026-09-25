package com.smartcampus.auth.controller;

import com.smartcampus.auth.dto.response.ErrorResponse;
import com.smartcampus.auth.dto.response.UserValidationResponse;
import com.smartcampus.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@Tag(name = "Internal User Validation", description = "Internal service integration endpoints for identity and role verification")
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Validate User Existence, Status, and Role",
            description = "Internal endpoint consumed by downstream microservices (e.g., student-service, notification-service) to validate if an Auth user exists, is active, and optionally possesses a required role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User validation processed successfully",
                    content = @Content(schema = @Schema(implementation = UserValidationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid requiredRole query parameter",
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
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(value = "/{userId}/validation", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserValidationResponse> validateUser(
            @Parameter(description = "Numeric ID of the Auth user to validate", example = "101", required = true)
            @PathVariable("userId") Long userId,
            @Parameter(description = "Optional required role name to check (e.g. STUDENT, FACULTY, ADMIN)", example = "STUDENT", required = false)
            @RequestParam(name = "requiredRole", required = false) String requiredRole) {
        UserValidationResponse response = authService.validateUser(userId, requiredRole);
        return ResponseEntity.ok(response);
    }
}
