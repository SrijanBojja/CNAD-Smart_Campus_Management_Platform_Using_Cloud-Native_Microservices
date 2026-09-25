package com.smartcampus.auth.exception;

import com.smartcampus.auth.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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
            BadRequestException.class
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
            errorMessage = "Parameter '" + matme.getName() + "' should be of type " + (matme.getRequiredType() != null ? matme.getRequiredType().getSimpleName() : "valid");
        }
        
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage, request.getRequestURI());
        log.warn("Validation failed for request [{}]: traceId={}, error={}", request.getRequestURI(), response.getTraceId(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            Exception ex, HttpServletRequest request) {
        String message = (ex instanceof BadCredentialsException) ? "Invalid username or password" : ex.getMessage();
        ErrorResponse response = buildErrorResponse(HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
        log.warn("Authentication failed for path [{}]: traceId={}", request.getRequestURI(), response.getTraceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI());
        log.warn("Unauthorized access to [{}]: traceId={}", request.getRequestURI(), response.getTraceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({UserDisabledException.class, DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleUserDisabled(
            Exception ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
        log.warn("Forbidden access for disabled/locked account on [{}]: traceId={}", request.getRequestURI(), response.getTraceId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions", request.getRequestURI());
        log.warn("Access denied on [{}]: traceId={}", request.getRequestURI(), response.getTraceId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            Exception ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.CONFLICT, "Resource conflict or duplicate entry", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponse response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
        log.error("Unhandled exception processing request [{}] (traceId={}): {}", request.getRequestURI(), response.getTraceId(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
