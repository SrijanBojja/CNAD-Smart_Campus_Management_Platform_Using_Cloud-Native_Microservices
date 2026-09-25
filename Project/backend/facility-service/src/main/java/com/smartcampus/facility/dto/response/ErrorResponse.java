package com.smartcampus.facility.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of error in UTC ISO-8601 format", example = "2026-10-01T12:00:00Z")
    private final String timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private final int status;

    @Schema(description = "HTTP error reason phrase", example = "Not Found")
    private final String error;

    @Schema(description = "Descriptive error message", example = "Facility not found with ID: 101")
    private final String message;

    @Schema(description = "Request URI path", example = "/api/v1/facilities/101")
    private final String path;

    @Schema(description = "Unique correlation trace ID for auditing and debugging", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private final String traceId;

    @Schema(description = "Field-level validation error details when applicable")
    private Map<String, String> validationErrors;

    public ErrorResponse(int status, String error, String message, String path, String traceId) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
    }

    public ErrorResponse(int status, String error, String message, String path, String traceId, Map<String, String> validationErrors) {
        this(status, error, message, path, traceId);
        this.validationErrors = validationErrors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
