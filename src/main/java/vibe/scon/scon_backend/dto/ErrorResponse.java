package vibe.scon.scon_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response structure for API errors.
 * Provides consistent error information format across all endpoints.
 *
 * <h3>Response Structure:</h3>
 * <pre>{@code
 * {
 *   "status": 400,
 *   "error": "BAD_REQUEST",
 *   "message": "Validation failed",
 *   "path": "/api/v1/employees",
 *   "timestamp": "2025-01-01T12:00:00",
 *   "fieldErrors": [
 *     {
 *       "field": "email",
 *       "rejectedValue": "invalid-email",
 *       "message": "Invalid email format"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @see vibe.scon.scon_backend.exception.GlobalExceptionHandler
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code of the error.
     */
    private final int status;

    /**
     * Error code for client-side error handling.
     * Example: "BAD_REQUEST", "NOT_FOUND", "INTERNAL_ERROR"
     */
    private final String error;

    /**
     * Human-readable error message.
     */
    private final String message;

    /**
     * Request path that caused the error.
     */
    private final String path;

    /**
     * Timestamp when the error occurred.
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * List of field-specific validation errors.
     * Only present for validation errors (HTTP 400).
     */
    private final List<FieldError> fieldErrors;

    /**
     * Represents a single field validation error.
     */
    @Getter
    @Builder
    public static class FieldError {

        /**
         * Name of the field that failed validation.
         */
        private final String field;

        /**
         * The value that was rejected.
         */
        private final Object rejectedValue;

        /**
         * Validation error message.
         */
        private final String message;
    }

    /**
     * Creates a simple error response without field errors.
     *
     * @param status  HTTP status code
     * @param error   Error code
     * @param message Error message
     * @param path    Request path
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates an error response with field validation errors.
     *
     * @param status      HTTP status code
     * @param error       Error code
     * @param message     Error message
     * @param path        Request path
     * @param fieldErrors List of field validation errors
     * @return ErrorResponse instance with field errors
     */
    public static ErrorResponse of(int status, String error, String message, String path,
                                    List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}

