package vibe.scon.scon_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 Not Found response.
 *
 * <p>Use this exception when an entity lookup by ID or other identifier
 * returns no result.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Employee employee = employeeRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
 * }</pre>
 */
public class ResourceNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Constructs a ResourceNotFoundException for a resource with the given name and ID.
     *
     * @param resourceName The name of the resource type (e.g., "Employee", "Store")
     * @param resourceId   The ID that was not found
     */
    public ResourceNotFoundException(String resourceName, Long resourceId) {
        super(
            ERROR_CODE,
            String.format("%s not found with id: %d", resourceName, resourceId),
            HttpStatus.NOT_FOUND
        );
    }

    /**
     * Constructs a ResourceNotFoundException for a resource with a custom identifier.
     *
     * @param resourceName The name of the resource type
     * @param fieldName    The name of the field used for lookup (e.g., "email", "code")
     * @param fieldValue   The value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(
            ERROR_CODE,
            String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND
        );
    }

    /**
     * Constructs a ResourceNotFoundException with a custom message.
     *
     * @param message Custom error message
     */
    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }
}

