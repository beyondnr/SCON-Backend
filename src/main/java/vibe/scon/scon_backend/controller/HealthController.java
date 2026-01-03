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
 *   <li>GET /api/v1/health - Returns detailed server health status</li>
 *   <li>GET /api/v1/health/ping - Simple connectivity check (for load balancers)</li>
 *   <li>GET /api/v1/ping - Legacy ping endpoint (deprecated, use /api/v1/health/ping)</li>
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

    @Value("${spring.application.version:0.0.1-SNAPSHOT}")
    private String applicationVersion;

    /**
     * Detailed health check endpoint.
     * Returns comprehensive server status information for monitoring systems.
     *
     * <h4>Response Example:</h4>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "Server is running",
     *   "data": {
     *     "status": "UP",
     *     "timestamp": "2026-01-03T12:00:00",
     *     "applicationName": "scon-backend",
     *     "version": "0.0.1-SNAPSHOT",
     *     "profile": "dev"
     *   },
     *   "timestamp": "2026-01-03T12:00:00"
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
                .applicationName(applicationName)
                .version(applicationVersion)
                .profile(activeProfile)
                .build();

        return ApiResponse.success("Server is running", healthResponse);
    }

    /**
     * Simple ping endpoint for basic connectivity check.
     * Used by load balancers and monitoring systems.
     *
     * @return ApiResponse with "pong" message
     */
    @GetMapping("/health/ping")
    public ApiResponse<String> ping() {
        log.debug("Ping requested");
        return ApiResponse.success("pong");
    }

    /**
     * Legacy ping endpoint (deprecated).
     * 
     * <p>This endpoint is maintained for backward compatibility.
     * Please migrate to {@link #ping()} at /api/v1/health/ping</p>
     *
     * @return ApiResponse with "pong" message
     * @deprecated Use /api/v1/health/ping instead
     */
    @GetMapping("/ping")
    @Deprecated
    public ApiResponse<String> pingLegacy() {
        log.warn("Deprecated endpoint /api/v1/ping used. Please migrate to /api/v1/health/ping");
        return ApiResponse.success("pong");
    }
}

