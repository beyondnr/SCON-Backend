package vibe.scon.scon_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vibe.scon.scon_backend.dto.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles all exceptions and returns consistent error responses.
 *
 * <p>This class intercepts exceptions thrown from any controller
 * and converts them into standardized {@link ErrorResponse} objects.</p>
 *
 * <h3>Handled Exceptions:</h3>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} - Bean validation errors (400)</li>
 *   <li>{@link MissingServletRequestParameterException} - Missing required request parameters (400)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} - Type conversion errors (400)</li>
 *   <li>{@link ResourceNotFoundException} - Resource not found (404)</li>
 *   <li>{@link BadRequestException} - Bad request (400)</li>
 *   <li>{@link BusinessException} - Business logic errors (varies)</li>
 *   <li>{@link Exception} - Unexpected errors (500)</li>
 * </ul>
 * 
 * <h4>INTG-BE-Phase3-v1.1.0 (2026-01-10):</h4>
 * <ul>
 *   <li>MissingServletRequestParameterException 핸들러 추가 (Query Parameters 검증)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Jakarta Bean Validation errors.
     * Extracts field-level validation errors and returns a detailed response.
     *
     * @param ex      The validation exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 400 and field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        BindingResult bindingResult = ex.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed for request [{}]: {} errors",
                request.getRequestURI(), fieldErrors.size());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles missing required request parameter errors.
     * This occurs when a required @RequestParam is missing from the request.
     *
     * @param ex      The missing parameter exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        log.warn("Missing required parameter for request [{}]: {}", request.getRequestURI(), message);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "MISSING_PARAMETER",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles type mismatch errors (e.g., invalid path variable format, invalid query parameter format).
     * This includes @DateTimeFormat parsing failures.
     *
     * @param ex      The type mismatch exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());

        log.warn("Type mismatch for request [{}]: {}", request.getRequestURI(), message);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "TYPE_MISMATCH",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex      The not found exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found for request [{}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles bad request exceptions.
     *
     * @param ex      The bad request exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 400
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {

        log.warn("Bad request for [{}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all other business exceptions.
     *
     * @param ex      The business exception
     * @param request The HTTP request
     * @return ResponseEntity with the exception's HTTP status
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business exception for request [{}]: {} - {}",
                request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handles all unexpected exceptions.
     * Logs the full stack trace and returns a generic error message.
     *
     * @param ex      The unexpected exception
     * @param request The HTTP request
     * @return ResponseEntity with HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error for request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

