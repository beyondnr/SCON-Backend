package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Schedule 엔티티를 위한 JPA Repository.
 * 
 * <p>스케줄 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * @see Schedule
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    /**
     * 특정 매장의 스케줄 목록 조회
     * 
     * @param storeId Store ID
     * @return 스케줄 목록
     */
    List<Schedule> findByStoreId(Long storeId);
    
    /**
     * 특정 매장의 특정 주차 스케줄 조회
     * 
     * @param storeId Store ID
     * @param weekStartDate 주 시작일
     * @return Optional<Schedule>
     */
    Optional<Schedule> findByStoreIdAndWeekStartDate(Long storeId, LocalDate weekStartDate);
    
    /**
     * 특정 상태의 스케줄 목록 조회
     * 
     * @param status 스케줄 상태
     * @return 스케줄 목록
     */
    List<Schedule> findByStatus(ScheduleStatus status);
    
    /**
     * 특정 매장의 특정 상태 스케줄 목록 조회
     * 
     * @param storeId Store ID
     * @param status 스케줄 상태
     * @return 스케줄 목록
     */
    List<Schedule> findByStoreIdAndStatus(Long storeId, ScheduleStatus status);

    /**
     * 특정 매장의 기간별 스케줄 목록 조회.
     * 
     * <p>월간 조회 등에 사용됩니다.</p>
     * 
     * @param storeId Store ID
     * @param start 주 시작일 범위 시작
     * @param end 주 시작일 범위 끝
     * @return 스케줄 목록
     */
    List<Schedule> findAllByStoreIdAndWeekStartDateBetween(Long storeId, LocalDate start, LocalDate end);
    
    /**
     * 특정 Owner가 소유한 매장의 기간별 스케줄 목록 조회
     * 
     * <p>보안 강화를 위해 ownerId를 포함한 쿼리 레벨 필터링을 제공합니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-001} - 데이터 격리 및 접근 제어 개선</li>
     * </ul>
     * 
     * @param storeId Store ID
     * @param ownerId Owner ID
     * @param start 주 시작일 범위 시작
     * @param end 주 시작일 범위 끝
     * @return 스케줄 목록
     */
    @Query("SELECT s FROM Schedule s WHERE s.store.id = :storeId AND s.store.owner.id = :ownerId " +
           "AND s.weekStartDate BETWEEN :start AND :end")
    List<Schedule> findByStoreIdAndOwnerIdAndWeekStartDateBetween(
            @Param("storeId") Long storeId, 
            @Param("ownerId") Long ownerId,
            @Param("start") LocalDate start, 
            @Param("end") LocalDate end);
}

