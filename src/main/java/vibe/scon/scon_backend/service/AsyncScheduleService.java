package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.schedule.ScheduleDetailResponseDto;
import vibe.scon.scon_backend.dto.schedule.UpdateScheduleRequestDto;
import vibe.scon.scon_backend.entity.enums.TaskStatus;

import java.util.concurrent.CompletableFuture;

/**
 * 비동기 스케줄 처리 서비스.
 * 
 * <p>스케줄 수정 작업을 비동기로 처리합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 2}: 스케줄 수정 비동기화</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.service.ScheduleService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncScheduleService {
    
    private final ScheduleService scheduleService;
    private final AsyncTaskService asyncTaskService;
    
    /**
     * 스케줄 수정을 비동기로 처리합니다.
     * 
     * <p>별도 스레드에서 실행되며, 새로운 트랜잭션을 시작합니다.</p>
     * 
     * @param taskId 작업 ID (UUID)
     * @param ownerId 인증된 Owner ID
     * @param scheduleId 스케줄 ID
     * @param request 스케줄 수정 요청 DTO
     * @return CompletableFuture (결과 없음, 상태는 AsyncTask에 저장됨)
     */
    @Async("dbExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> updateScheduleAsync(
            String taskId, Long ownerId, Long scheduleId, 
            UpdateScheduleRequestDto request) {
        
        log.info("Starting async schedule update. taskId: {}, scheduleId: {}, ownerId: {}", 
                taskId, scheduleId, ownerId);
        
        try {
            // 작업 진행률 업데이트 (10%)
            asyncTaskService.updateTaskProgress(taskId, 10);
            
            // 실제 작업 수행
            ScheduleDetailResponseDto result = scheduleService.updateSchedule(
                    ownerId, scheduleId, request);
            
            // 작업 진행률 업데이트 (90%)
            asyncTaskService.updateTaskProgress(taskId, 90);
            
            // 작업 완료
            asyncTaskService.updateTaskStatus(taskId, TaskStatus.COMPLETED, result);
            
            log.info("Async schedule update completed. taskId: {}, scheduleId: {}", 
                    taskId, scheduleId);
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async schedule update failed. taskId: {}, scheduleId: {}", 
                    taskId, scheduleId, e);
            
            // 작업 실패 처리
            asyncTaskService.updateTaskStatus(taskId, TaskStatus.FAILED, null);
            asyncTaskService.setErrorMessage(taskId, e.getMessage());
            
            return CompletableFuture.failedFuture(e);
        }
    }
}
