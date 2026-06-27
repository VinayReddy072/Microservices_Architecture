package com.labplatform.equipment.exception;
import com.labplatform.common.exception.ApiErrorResponse;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EquipmentNotFoundException ex, HttpServletRequest req) {
        log.warn("Equipment not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req.getRequestURI());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req.getRequestURI());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue() != null
                                ? String.valueOf(fe.getRejectedValue()) : null)
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        ApiErrorResponse error = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .path(req.getRequestURI())
                .traceId(getCurrentTraceId())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action", req.getRequestURI());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. TraceId: " + getCurrentTraceId(), req.getRequestURI());
    }
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status, String error, String message, String path) {
        return ResponseEntity.status(status.value()).body(ApiErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .traceId(getCurrentTraceId())
                .build());
    }
}
