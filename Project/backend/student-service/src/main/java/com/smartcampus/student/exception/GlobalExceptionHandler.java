package com.smartcampus.student.exception;

import com.smartcampus.student.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        String traceId = UUID.randomUUID().toString();
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                traceId
        );
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BadRequestException.class,
            UserValidationFailedException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception ex, HttpServletRequest request) {
        String errorMessage = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException manve) {
            errorMessage = manve.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else if (ex instanceof MissingServletRequestParameterException msrpe) {
            errorMessage = "Required request parameter '" + msrpe.getParameterName() + "' is not present";
        } else if (ex instanceof MethodArgumentTypeMismatchException matme) {
            errorMessage = "Parameter '" + matme.getName() + "' should be of type " +
                    (matme.getRequiredType() != null ? matme.getRequiredType().getSimpleName() : "valid");
        }

        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage, request.getRequestURI());
        log.warn("Validation / Bad Request for [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request payload", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI());
        log.warn("Unauthorized access to [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "Access denied: insufficient permissions", request.getRequestURI());
        log.warn("Access denied on [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            Exception ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(
            Exception ex, HttpServletRequest request) {
        String message = (ex instanceof DuplicateResourceException) ? ex.getMessage() : "Resource conflict or duplicate entry";
        ErrorResponse response = buildErrorResponse(HttpStatus.CONFLICT, message, request.getRequestURI());
        log.warn("Conflict on [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceUnavailable(
            AuthServiceUnavailableException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());
        log.error("Auth Service integration error on [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
        log.error("Unhandled exception processing request [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
