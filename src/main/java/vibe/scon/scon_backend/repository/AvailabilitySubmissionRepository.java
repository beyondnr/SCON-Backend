package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.AvailabilitySubmission;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * AvailabilitySubmission 엔티티를 위한 JPA Repository.
 * 
 * <p>직원 가용시간 제출 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * @see AvailabilitySubmission
 */
public interface AvailabilitySubmissionRepository extends JpaRepository<AvailabilitySubmission, Long> {
    
    /**
     * 특정 직원의 가용시간 목록 조회
     * 
     * @param employeeId Employee ID
     * @return 가용시간 목록
     */
    List<AvailabilitySubmission> findByEmployeeId(Long employeeId);
    
    /**
     * 특정 직원의 특정 주차 가용시간 목록 조회
     * 
     * @param employeeId Employee ID
     * @param weekStartDate 주 시작일
     * @return 가용시간 목록
     */
    List<AvailabilitySubmission> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);
    
    /**
     * 특정 직원의 특정 주차, 특정 요일 가용시간 조회
     * 
     * @param employeeId Employee ID
     * @param weekStartDate 주 시작일
     * @param dayOfWeek 요일
     * @return 가용시간 목록
     */
    List<AvailabilitySubmission> findByEmployeeIdAndWeekStartDateAndDayOfWeek(
            Long employeeId, LocalDate weekStartDate, DayOfWeek dayOfWeek);
}

