package vibe.scon.scon_backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.HealthResponse;

/**
 * Health Check Controller.
 * Provides endpoints for monitoring server status and availability.
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>GET /api/v1/health - Returns server health status</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.application.name:scon-backend}")
    private String applicationName;

    /**
     * Health check endpoint.
     * Returns the current server status for monitoring systems.
     *
     * <h4>Response Example:</h4>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "Server is running",
     *   "data": {
     *     "status": "UP",
     *     "timestamp": "2025-01-01T12:00:00",
     *     "version": "0.0.1-SNAPSHOT",
     *     "profile": "dev"
     *   },
     *   "timestamp": "2025-01-01T12:00:00"
     * }
     * }</pre>
     *
     * @return ApiResponse containing HealthResponse data
     */
    @GetMapping("/health")
    public ApiResponse<HealthResponse> healthCheck() {
        log.debug("Health check requested");

        HealthResponse healthResponse = HealthResponse.builder()
                .status("UP")
                .version("0.0.1-SNAPSHOT")
                .profile(activeProfile)
                .build();

        return ApiResponse.success("Server is running", healthResponse);
    }

    /**
     * Simple ping endpoint for basic connectivity check.
     *
     * @return ApiResponse with "pong" message
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("pong");
    }
}

