package vibe.scon.scon_backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Health check response DTO.
 * Contains server status information for monitoring and diagnostics.
 */
@Getter
@Builder
public class HealthResponse {

    /**
     * Server health status.
     * Values: "UP", "DOWN", "DEGRADED"
     */
    private final String status;

    /**
     * Current server timestamp.
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Application name.
     */
    private final String applicationName;

    /**
     * Application version (optional).
     */
    private final String version;

    /**
     * Active Spring profile.
     */
    private final String profile;
}

