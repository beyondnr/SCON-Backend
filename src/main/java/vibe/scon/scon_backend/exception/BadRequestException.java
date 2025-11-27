package vibe.scon.scon_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when client request is invalid or malformed.
 * Results in HTTP 400 Bad Request response.
 *
 * <p>Use this exception for validation failures that are not covered
 * by Jakarta Bean Validation, or for business rule violations
 * detected at the service layer.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * if (startTime.isAfter(endTime)) {
 *     throw new BadRequestException("Start time must be before end time");
 * }
 * }</pre>
 */
public class BadRequestException extends BusinessException {

    private static final String ERROR_CODE = "BAD_REQUEST";

    /**
     * Constructs a BadRequestException with the given message.
     *
     * @param message Human-readable error message describing the invalid request
     */
    public BadRequestException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructs a BadRequestException with a custom error code.
     *
     * @param errorCode Custom error code for specific bad request scenarios
     * @param message   Human-readable error message
     */
    public BadRequestException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructs a BadRequestException with the given message and cause.
     *
     * @param message Human-readable error message
     * @param cause   The underlying cause of this exception
     */
    public BadRequestException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST, cause);
    }
}

