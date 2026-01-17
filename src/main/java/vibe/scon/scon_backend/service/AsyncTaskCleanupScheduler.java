package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vibe.scon.scon_backend.repository.AsyncTaskRepository;

import java.time.LocalDateTime;

/**
 * 비동기 작업 만료 정리 스케줄러.
 * 
 * <p>완료된 작업(COMPLETED, FAILED) 중 만료 시간이 지난 작업을 주기적으로 삭제합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 4}: 모니터링 및 최적화</li>
 * </ul>
 * 
 * <h3>스케줄:</h3>
 * <ul>
 *   <li>실행 주기: 매일 새벽 2시 (cron = "0 0 2 * * ?")</li>
 *   <li>삭제 대상: COMPLETED 또는 FAILED 상태이고 expiresAt이 지난 작업</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.repository.AsyncTaskRepository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskCleanupScheduler {
    
    private final AsyncTaskRepository asyncTaskRepository;
    
    /**
     * 만료된 비동기 작업을 정리합니다.
     * 
     * <p>매일 새벽 2시에 실행됩니다.
     * COMPLETED 또는 FAILED 상태이고 expiresAt이 현재 시간보다 이전인 작업을 삭제합니다.</p>
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void cleanupExpiredTasks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 만료된 작업 삭제
            asyncTaskRepository.deleteExpiredTasks(now);
            
            log.info("Expired async tasks cleaned up at {}", now);
        } catch (Exception e) {
            log.error("Failed to cleanup expired tasks", e);
        }
    }
}
