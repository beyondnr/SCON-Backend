package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

