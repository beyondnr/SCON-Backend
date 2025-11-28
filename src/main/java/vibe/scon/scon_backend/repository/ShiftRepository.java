package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Shift;

import java.time.LocalDate;
import java.util.List;

/**
 * Shift 엔티티를 위한 JPA Repository.
 * 
 * <p>시프트(근무) 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * @see Shift
 */
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    
    /**
     * 특정 스케줄에 속한 시프트 목록 조회
     * 
     * @param scheduleId Schedule ID
     * @return 시프트 목록
     */
    List<Shift> findByScheduleId(Long scheduleId);
    
    /**
     * 특정 직원의 시프트 목록 조회
     * 
     * @param employeeId Employee ID
     * @return 시프트 목록
     */
    List<Shift> findByEmployeeId(Long employeeId);
    
    /**
     * 특정 직원의 특정 날짜 시프트 조회
     * 
     * @param employeeId Employee ID
     * @param workDate 근무일
     * @return 시프트 목록
     */
    List<Shift> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
    
    /**
     * 특정 날짜의 모든 시프트 조회
     * 
     * @param workDate 근무일
     * @return 시프트 목록
     */
    List<Shift> findByWorkDate(LocalDate workDate);
}

