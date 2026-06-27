package com.labplatform.booking.exception;
import com.labplatform.common.exception.ApiErrorResponse;
import feign.FeignException;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final Tracer tracer;
    private String getCurrentTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "unavailable";
    }
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingNotFound(
            BookingNotFoundException ex, HttpServletRequest request) {
        log.warn("Booking not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingConflict(
            BookingConflictException ex, HttpServletRequest request) {
        log.warn("Booking conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Booking Conflict", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(EquipmentUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleEquipmentUnavailable(
            EquipmentUnavailableException ex, HttpServletRequest request) {
        log.warn("Equipment unavailable: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Equipment Unavailable", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEquipmentNotFound(
            EquipmentNotFoundException ex, HttpServletRequest request) {
        log.warn("Equipment not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Equipment Not Found", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Downstream service unavailable: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(
            FeignException ex, HttpServletRequest request) {
        log.error("Feign call failed: status={} message={}", ex.status(), ex.getMessage());
        HttpStatus status = ex.status() >= 500 || ex.status() < 0
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return buildErrorResponse(status, "Downstream Service Error",
                "Equipment service returned error: " + ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue() != null
                                ? String.valueOf(fe.getRejectedValue()) : null)
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        log.warn("Validation failed: {} field errors", fieldErrors.size());
        ApiErrorResponse error = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed — " + fieldErrors.size() + " error(s)")
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action", request.getRequestURI());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact support with traceId: " + getCurrentTraceId(),
                request.getRequestURI());
    }
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status, String error, String message, String path) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .traceId(getCurrentTraceId())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
