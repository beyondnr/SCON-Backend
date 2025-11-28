package vibe.scon.scon_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Abstract base exception for all business logic errors.
 * Provides a consistent structure for domain-specific exceptions
 * with error codes and HTTP status mapping.
 *
 * <p>All custom business exceptions should extend this class to ensure
 * consistent error handling across the application.</p>
 *
 * @see GlobalExceptionHandler
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /**
     * Application-specific error code for client-side error handling.
     * Example: "RESOURCE_NOT_FOUND", "INVALID_INPUT", "SCHEDULE_CONFLICT"
     */
    private final String errorCode;

    /**
     * HTTP status code to be returned in the response.
     */
    private final HttpStatus httpStatus;

    /**
     * Constructs a new BusinessException with error code, message, and HTTP status.
     *
     * @param errorCode  Application-specific error code
     * @param message    Human-readable error message
     * @param httpStatus HTTP status code for the response
     */
    protected BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Constructs a new BusinessException with error code, message, HTTP status, and cause.
     *
     * @param errorCode  Application-specific error code
     * @param message    Human-readable error message
     * @param httpStatus HTTP status code for the response
     * @param cause      The underlying cause of this exception
     */
    protected BusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

