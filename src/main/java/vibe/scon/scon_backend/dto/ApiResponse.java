package vibe.scon.scon_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper class.
 * All API endpoints should return responses wrapped in this format
 * for consistency across the application.
 *
 * <h3>Response Structure:</h3>
 * <pre>{@code
 * {
 *   "status": 200,
 *   "message": "Success",
 *   "data": { ... },
 *   "timestamp": "2025-01-01T12:00:00"
 * }
 * }</pre>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Success with data
 * return ApiResponse.success(employeeDto);
 *
 * // Success with custom message
 * return ApiResponse.success("Employee created successfully", employeeDto);
 *
 * // Created (201)
 * return ApiResponse.created(newEmployeeDto);
 *
 * // Error
 * return ApiResponse.error(404, "Resource not found");
 * }</pre>
 *
 * @param <T> The type of data contained in the response
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * HTTP status code of the response.
     */
    private final int status;

    /**
     * Human-readable message describing the result.
     */
    private final String message;

    /**
     * Response payload data. Null for error responses.
     */
    private final T data;

    /**
     * Timestamp when the response was generated.
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a success response (HTTP 200) with data.
     *
     * @param data The response payload
     * @param <T>  The type of data
     * @return ApiResponse with status 200 and the provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }

    /**
     * Creates a success response (HTTP 200) with custom message and data.
     *
     * @param message Custom success message
     * @param data    The response payload
     * @param <T>     The type of data
     * @return ApiResponse with status 200, custom message, and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates a success response (HTTP 200) with message only (no data).
     *
     * @param message Success message
     * @param <T>     The type of data (will be null)
     * @return ApiResponse with status 200 and message
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .build();
    }

    /**
     * Creates a created response (HTTP 201) with data.
     *
     * @param data The newly created resource
     * @param <T>  The type of data
     * @return ApiResponse with status 201 and the created data
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .message("Created")
                .data(data)
                .build();
    }

    /**
     * Creates a created response (HTTP 201) with custom message and data.
     *
     * @param message Custom message
     * @param data    The newly created resource
     * @param <T>     The type of data
     * @return ApiResponse with status 201, message, and data
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with custom status and message.
     *
     * @param status  HTTP status code
     * @param message Error message
     * @param <T>     The type of data (will be null)
     * @return ApiResponse with error status and message
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .build();
    }
}

