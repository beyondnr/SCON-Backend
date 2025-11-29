package vibe.scon.scon_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user lacks permission to access a resource.
 * Results in HTTP 403 Forbidden response.
 *
 * <p>Use this exception when the user is authenticated but not authorized
 * to perform the requested operation on a specific resource.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * if (!store.getOwner().getId().equals(ownerId)) {
 *     throw new ForbiddenException("해당 매장에 대한 접근 권한이 없습니다");
 * }
 * }</pre>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code TC-STORE-006} - 타 사용자 매장 접근 차단</li>
 *   <li>{@code TC-EMP-007} - 타 사용자 직원 접근 차단</li>
 * </ul>
 */
public class ForbiddenException extends BusinessException {

    private static final String ERROR_CODE = "FORBIDDEN";

    /**
     * Constructs a ForbiddenException with the given message.
     *
     * @param message Human-readable error message describing the access denial
     */
    public ForbiddenException(String message) {
        super(ERROR_CODE, message, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructs a ForbiddenException with a custom error code.
     *
     * @param errorCode Custom error code for specific forbidden scenarios
     * @param message   Human-readable error message
     */
    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructs a ForbiddenException with the given message and cause.
     *
     * @param message Human-readable error message
     * @param cause   The underlying cause of this exception
     */
    public ForbiddenException(String message, Throwable cause) {
        super(ERROR_CODE, message, HttpStatus.FORBIDDEN, cause);
    }
}
