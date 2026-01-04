package vibe.scon.scon_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.schedule.ScheduleResponseDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.service.StoreService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 스케줄 관리 컨트롤러.
 * 
 * <p>주간/월간 스케줄 조회 및 관리 API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-SEC-001} - 데이터 격리 및 접근 제어 개선</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final StoreService storeService;

    /**
     * 월간 스케줄 목록 조회.
     * 
     * <p>특정 연/월(YYYY-MM)에 해당하는 스케줄 목록을 조회합니다.</p>
     * 
     * <h3>보안 강화 (POC-BE-SEC-001):</h3>
     * <ul>
     *   <li>인증된 사용자만 접근 가능 (Authentication 파라미터)</li>
     *   <li>매장 소유권 검증 (StoreService.getStore())</li>
     *   <li>쿼리 레벨 ownerId 필터링 (방어적 프로그래밍)</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param storeId 매장 ID
     * @param yearMonth 조회할 연/월 (YYYY-MM)
     * @return 해당 월의 스케줄 목록
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleResponseDto>> getMonthlySchedules(
            Authentication authentication,
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get monthly schedules request. ownerId: {}, storeId: {}, yearMonth: {}", 
                ownerId, storeId, yearMonth);
        
        // 매장 소유권 확인 (필수) - POC-BE-SEC-001
        storeService.getStore(ownerId, storeId);  // 이미 소유권 검증 포함
        
        // 월간 조회 범위 설정
        // 주의: weekStartDate가 2월 26일인 경우, 3월 1,2,3일이 포함되므로 3월 조회 시 나와야 함.
        // 따라서 검색 시작 범위를 '해당 월 1일'보다 넉넉하게 잡거나,
        // 비즈니스 로직상 "해당 월에 하루라도 포함되는 주"를 찾도록 해야 함.
        
        // 간단한 해결책: 해당 월의 1일 이전 6일부터 검색 (주간이 월요일 시작이므로)
        LocalDate startDate = yearMonth.atDay(1).minusDays(6);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // ownerId를 포함한 쿼리로 변경 (방어적 프로그래밍) - POC-BE-SEC-001
        List<Schedule> schedules = scheduleRepository.findByStoreIdAndOwnerIdAndWeekStartDateBetween(
                storeId, ownerId, startDate, endDate);

        // DTO 변환
        List<ScheduleResponseDto> response = schedules.stream()
                .map(ScheduleResponseDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}

