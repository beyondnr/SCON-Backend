package vibe.scon.scon_backend.dto.async;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import vibe.scon.scon_backend.entity.AsyncTask;
import vibe.scon.scon_backend.entity.enums.TaskStatus;

import java.time.LocalDateTime;

/**
 * 비동기 작업 상태 응답 DTO.
 * 
 * <p>클라이언트에게 비동기 작업의 상태를 반환할 때 사용됩니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 1}: 인프라 구축</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AsyncTaskResponseDto {
    
    /**
     * 작업 ID (UUID).
     */
    private String taskId;
    
    /**
     * 작업 상태.
     */
    private TaskStatus status;
    
    /**
     * 작업 유형.
     */
    private String taskType;
    
    /**
     * 진행률 (0-100).
     */
    private Integer progress;
    
    /**
     * 예상 완료 시간.
     */
    private LocalDateTime estimatedCompletionTime;
    
    /**
     * 작업 시작 시간.
     */
    private LocalDateTime startedAt;
    
    /**
     * 작업 완료 시간.
     */
    private LocalDateTime completedAt;
    
    /**
     * 결과 만료 시간.
     */
    private LocalDateTime expiresAt;
    
    /**
     * 에러 메시지 (실패 시에만).
     */
    private String errorMessage;
    
    /**
     * AsyncTask 엔티티로부터 DTO를 생성합니다.
     * 
     * @param task AsyncTask 엔티티
     * @return AsyncTaskResponseDto
     */
    public static AsyncTaskResponseDto from(AsyncTask task) {
        return AsyncTaskResponseDto.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .taskType(task.getTaskType())
                .progress(task.getProgress())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .expiresAt(task.getExpiresAt())
                .errorMessage(task.getErrorMessage())
                .build();
    }
}
