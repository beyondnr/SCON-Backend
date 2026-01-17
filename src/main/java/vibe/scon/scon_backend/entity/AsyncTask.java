package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vibe.scon.scon_backend.entity.enums.TaskStatus;

import java.time.LocalDateTime;

/**
 * 비동기 작업 상태를 관리하는 엔티티.
 * 
 * <p>클라이언트로부터 요청을 받아 비동기로 처리되는 작업의 상태와 결과를 추적합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 1}: 인프라 구축</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.service.AsyncTaskService
 */
@Entity
@Table(name = "async_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AsyncTask {
    
    /**
     * 작업 고유 ID (UUID 형식).
     */
    @Id
    @Column(name = "task_id", length = 36, nullable = false, unique = true)
    private String taskId;
    
    /**
     * 작업 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;
    
    /**
     * 작업 유형 (예: SCHEDULE_UPDATE, STORE_CREATE).
     */
    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;
    
    /**
     * 요청자 ID (Owner ID).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 요청 데이터 (JSON 형식).
     */
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    /**
     * 결과 데이터 (JSON 형식).
     */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;
    
    /**
     * 에러 메시지 (실패 시).
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    /**
     * 진행률 (0-100).
     */
    @Column(name = "progress")
    private Integer progress;
    
    /**
     * 작업 시작 시간.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    /**
     * 작업 완료 시간.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 결과 만료 시간 (기본 24시간).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 생성 시간 (자동 설정).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간 (자동 설정).
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Builder
    private AsyncTask(String taskId, TaskStatus status, String taskType, Long userId,
                     String requestData, String resultData, String errorMessage,
                     Integer progress, LocalDateTime startedAt, LocalDateTime completedAt,
                     LocalDateTime expiresAt) {
        this.taskId = taskId;
        this.status = status;
        this.taskType = taskType;
        this.userId = userId;
        this.requestData = requestData;
        this.resultData = resultData;
        this.errorMessage = errorMessage;
        this.progress = progress;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * 작업 상태 업데이트.
     * 
     * @param status 새로운 상태
     */
    public void updateStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 진행률 업데이트.
     * 
     * @param progress 진행률 (0-100)
     */
    public void updateProgress(Integer progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        this.progress = progress;
    }
    
    /**
     * 에러 메시지 설정.
     * 
     * @param errorMessage 에러 메시지
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = TaskStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 결과 데이터 설정.
     * 
     * @param resultData 결과 데이터 (JSON 형식)
     */
    public void setResultData(String resultData) {
        this.resultData = resultData;
    }
}
