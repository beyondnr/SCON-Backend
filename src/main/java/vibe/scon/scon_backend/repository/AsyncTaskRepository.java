package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vibe.scon.scon_backend.entity.AsyncTask;
import vibe.scon.scon_backend.entity.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 비동기 작업 Repository.
 * 
 * <p>AsyncTask 엔티티의 데이터 접근을 담당합니다.</p>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 1}: 인프라 구축</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.entity.AsyncTask
 */
@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask, String> {
    
    /**
     * 작업 ID로 비동기 작업 조회.
     * 
     * @param taskId 작업 ID (UUID)
     * @return AsyncTask (없으면 empty)
     */
    Optional<AsyncTask> findByTaskId(String taskId);
    
    /**
     * 사용자 ID와 상태로 비동기 작업 목록 조회.
     * 
     * @param userId 사용자 ID
     * @param status 작업 상태
     * @return AsyncTask 목록
     */
    List<AsyncTask> findByUserIdAndStatus(Long userId, TaskStatus status);
    
    /**
     * 상태와 생성 시간으로 비동기 작업 목록 조회 (만료 작업 찾기용).
     * 
     * @param status 작업 상태
     * @param before 이전 시간
     * @return AsyncTask 목록
     */
    List<AsyncTask> findByStatusAndCreatedAtBefore(TaskStatus status, LocalDateTime before);
    
    /**
     * 만료된 작업 삭제.
     * 
     * <p>COMPLETED 또는 FAILED 상태이고 expiresAt이 지난 작업을 삭제합니다.</p>
     * 
     * @param now 현재 시간
     */
    @Modifying
    @Query("DELETE FROM AsyncTask t WHERE t.status IN ('COMPLETED', 'FAILED') " +
           "AND t.expiresAt < :now")
    void deleteExpiredTasks(@Param("now") LocalDateTime now);
}
