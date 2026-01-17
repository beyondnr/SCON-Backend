package vibe.scon.scon_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.async.AsyncTaskResponseDto;
import vibe.scon.scon_backend.entity.AsyncTask;
import vibe.scon.scon_backend.entity.enums.TaskStatus;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.AsyncTaskRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비동기 작업 관리 서비스.
 * 
 * <p>비동기 작업의 생성, 상태 업데이트, 조회 등을 담당합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 1}: 인프라 구축</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.entity.AsyncTask
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsyncTaskService {
    
    private final AsyncTaskRepository asyncTaskRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 비동기 작업을 생성합니다.
     * 
     * @param taskType 작업 유형 (예: SCHEDULE_UPDATE, STORE_CREATE)
     * @param userId 요청자 ID (Owner ID)
     * @param requestData 요청 데이터 (객체)
     * @return 생성된 작업 ID (UUID)
     */
    @Transactional
    public String createTask(String taskType, Long userId, Object requestData) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            String requestDataJson = requestData != null 
                    ? objectMapper.writeValueAsString(requestData)
                    : null;
            
            AsyncTask task = AsyncTask.builder()
                    .taskId(taskId)
                    .status(TaskStatus.IN_PROGRESS)
                    .taskType(taskType)
                    .userId(userId)
                    .requestData(requestDataJson)
                    .progress(0)
                    .startedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            
            asyncTaskRepository.save(task);
            log.info("Async task created. taskId: {}, taskType: {}, userId: {}", 
                    taskId, taskType, userId);
            
            return taskId;
        } catch (Exception e) {
            log.error("Failed to create async task. taskType: {}, userId: {}", taskType, userId, e);
            throw new RuntimeException("Failed to create async task", e);
        }
    }
    
    /**
     * 작업 상태를 업데이트합니다.
     * 
     * @param taskId 작업 ID
     * @param status 새로운 상태
     * @param result 결과 데이터 (객체, null 가능)
     */
    @Transactional
    public void updateTaskStatus(String taskId, TaskStatus status, Object result) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다: " + taskId));
        
        task.updateStatus(status);
        
        if (result != null) {
            try {
                String resultDataJson = objectMapper.writeValueAsString(result);
                task.setResultData(resultDataJson);
            } catch (Exception e) {
                log.error("Failed to serialize result data. taskId: {}", taskId, e);
            }
        }
        
        asyncTaskRepository.save(task);
        log.debug("Task status updated. taskId: {}, status: {}", taskId, status);
    }
    
    /**
     * 작업 진행률을 업데이트합니다.
     * 
     * @param taskId 작업 ID
     * @param progress 진행률 (0-100)
     */
    @Transactional
    public void updateTaskProgress(String taskId, Integer progress) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다: " + taskId));
        
        task.updateProgress(progress);
        asyncTaskRepository.save(task);
    }
    
    /**
     * 에러 메시지를 설정합니다.
     * 
     * @param taskId 작업 ID
     * @param errorMessage 에러 메시지
     */
    @Transactional
    public void setErrorMessage(String taskId, String errorMessage) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다: " + taskId));
        
        task.setErrorMessage(errorMessage);
        asyncTaskRepository.save(task);
        log.warn("Task failed. taskId: {}, error: {}", taskId, errorMessage);
    }
    
    /**
     * 작업 상태를 조회합니다.
     * 
     * @param taskId 작업 ID
     * @return AsyncTaskResponseDto
     */
    public AsyncTaskResponseDto getTaskStatus(String taskId) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다: " + taskId));
        
        return AsyncTaskResponseDto.from(task);
    }
    
    /**
     * 작업 엔티티를 조회합니다 (내부 사용).
     * 
     * @param taskId 작업 ID
     * @return AsyncTask
     */
    public AsyncTask getTask(String taskId) {
        return asyncTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("작업을 찾을 수 없습니다: " + taskId));
    }
}
