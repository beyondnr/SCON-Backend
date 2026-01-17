package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.dto.store.StoreResponseDto;
import vibe.scon.scon_backend.entity.enums.TaskStatus;

import java.util.concurrent.CompletableFuture;

/**
 * 비동기 매장 처리 서비스.
 * 
 * <p>매장 생성 작업을 비동기로 처리합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 3}: 매장 생성 비동기화</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.service.StoreService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncStoreService {
    
    private final StoreService storeService;
    private final AsyncTaskService asyncTaskService;
    
    /**
     * 매장 생성을 비동기로 처리합니다.
     * 
     * <p>별도 스레드에서 실행되며, 새로운 트랜잭션을 시작합니다.</p>
     * 
     * @param taskId 작업 ID (UUID)
     * @param ownerId 인증된 Owner ID
     * @param request 매장 생성 요청 DTO
     * @return CompletableFuture (결과 없음, 상태는 AsyncTask에 저장됨)
     */
    @Async("dbExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> createStoreAsync(
            String taskId, Long ownerId, StoreRequestDto request) {
        
        log.info("Starting async store creation. taskId: {}, ownerId: {}", taskId, ownerId);
        
        try {
            // 작업 진행률 업데이트 (10%)
            asyncTaskService.updateTaskProgress(taskId, 10);
            
            // 실제 작업 수행
            StoreResponseDto result = storeService.createStore(ownerId, request);
            
            // 작업 진행률 업데이트 (90%)
            asyncTaskService.updateTaskProgress(taskId, 90);
            
            // 작업 완료
            asyncTaskService.updateTaskStatus(taskId, TaskStatus.COMPLETED, result);
            
            log.info("Async store creation completed. taskId: {}, storeId: {}", 
                    taskId, result.getId());
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async store creation failed. taskId: {}", taskId, e);
            
            // 작업 실패 처리
            asyncTaskService.updateTaskStatus(taskId, TaskStatus.FAILED, null);
            asyncTaskService.setErrorMessage(taskId, e.getMessage());
            
            return CompletableFuture.failedFuture(e);
        }
    }
}
