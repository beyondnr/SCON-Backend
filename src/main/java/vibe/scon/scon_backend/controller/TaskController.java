package vibe.scon.scon_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.async.AsyncTaskResponseDto;
import vibe.scon.scon_backend.entity.AsyncTask;
import vibe.scon.scon_backend.entity.enums.TaskStatus;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.service.AsyncTaskService;

/**
 * 비동기 작업 상태 조회 컨트롤러.
 * 
 * <p>클라이언트가 비동기 작업의 상태와 결과를 조회할 수 있는 API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 2}: 스케줄 수정 비동기화</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code GET /api/v1/tasks/{taskId}} - 작업 상태 조회</li>
 *   <li>{@code GET /api/v1/tasks/{taskId}/result} - 작업 결과 조회</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final AsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;
    
    /**
     * 작업 상태 조회 API.
     * 
     * <p>작업 ID로 작업의 현재 상태를 조회합니다.</p>
     * 
     * <h4>응답 예시:</h4>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "작업 상태 조회 성공",
     *   "data": {
     *     "taskId": "550e8400-e29b-41d4-a716-446655440000",
     *     "status": "IN_PROGRESS",
     *     "progress": 50,
     *     ...
     *   }
     * }
     * }</pre>
     * 
     * @param taskId 작업 ID (UUID)
     * @return 작업 상태 응답 (200 OK)
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<AsyncTaskResponseDto>> getTaskStatus(
            @PathVariable String taskId) {
        
        log.debug("Get task status request. taskId: {}", taskId);
        
        AsyncTaskResponseDto response = asyncTaskService.getTaskStatus(taskId);
        
        return ResponseEntity.ok(ApiResponse.success("작업 상태 조회 성공", response));
    }
    
    /**
     * 작업 결과 조회 API.
     * 
     * <p>작업이 완료된 경우 결과 데이터를 반환합니다.</p>
     * 
     * <h4>응답 예시:</h4>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "결과 조회 성공",
     *   "data": {
     *     "scheduleId": 123,
     *     "status": "PENDING",
     *     "shifts": [...]
     *   }
     * }
     * }</pre>
     * 
     * @param taskId 작업 ID (UUID)
     * @return 작업 결과 응답 (200 OK)
     * @throws BadRequestException 작업이 아직 완료되지 않은 경우 (400 Bad Request)
     */
    @GetMapping("/{taskId}/result")
    public ResponseEntity<ApiResponse<Object>> getTaskResult(
            @PathVariable String taskId) {
        
        log.debug("Get task result request. taskId: {}", taskId);
        
        AsyncTask task = asyncTaskService.getTask(taskId);
        
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BadRequestException(
                    String.format("작업이 아직 완료되지 않았습니다. 현재 상태: %s", task.getStatus()));
        }
        
        try {
            // JSON 결과 데이터를 Object로 역직렬화
            Object result = task.getResultData() != null
                    ? objectMapper.readValue(task.getResultData(), Object.class)
                    : null;
            
            return ResponseEntity.ok(ApiResponse.success("결과 조회 성공", result));
        } catch (Exception e) {
            log.error("Failed to deserialize task result. taskId: {}", taskId, e);
            throw new BadRequestException("결과 데이터를 읽을 수 없습니다");
        }
    }
}
