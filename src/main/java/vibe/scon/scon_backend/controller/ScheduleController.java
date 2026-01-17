package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.schedule.ScheduleDetailResponseDto;
import vibe.scon.scon_backend.dto.schedule.ScheduleResponseDto;
import vibe.scon.scon_backend.dto.schedule.UpdateScheduleRequestDto;
import org.springframework.http.HttpStatus;
import vibe.scon.scon_backend.dto.async.AsyncTaskResponseDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.enums.TaskStatus;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.service.AsyncScheduleService;
import vibe.scon.scon_backend.service.AsyncTaskService;
import vibe.scon.scon_backend.service.ScheduleService;
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
    private final ScheduleService scheduleService;
    private final AsyncScheduleService asyncScheduleService;
    private final AsyncTaskService asyncTaskService;

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

    /**
     * 스케줄 상세 조회.
     * 
     * <p>스케줄 ID로 상세 정보를 조회합니다. Shift 정보를 포함합니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code INTG-BE-Phase6} - 스케줄 편집 기능</li>
     *   <li>{@code REQ-FUNC-007} - 드래그&드롭 스케줄 편집</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 스케줄 ID
     * @return 스케줄 상세 응답 (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> getScheduleDetail(
            Authentication authentication,
            @PathVariable Long id) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get schedule detail request. scheduleId: {}, ownerId: {}", id, ownerId);
        
        ScheduleDetailResponseDto response = scheduleService.getScheduleDetail(ownerId, id);
        
        return ResponseEntity.ok(ApiResponse.success("스케줄 조회 성공", response));
    }

    /**
     * 스케줄 수정.
     * 
     * <p>스케줄 상태 및 Shift 정보를 수정합니다.</p>
     * 
     * <h3>부분 수정 지원:</h3>
     * <ul>
     *   <li>status가 null인 경우: 상태 변경 없음</li>
     *   <li>shifts가 null인 경우: Shift 정보 변경 없음</li>
     *   <li>둘 다 null이 아닌 경우: 둘 다 업데이트</li>
     * </ul>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code INTG-BE-Phase6} - 스케줄 편집 기능</li>
     *   <li>{@code REQ-FUNC-007} - 드래그&드롭 스케줄 편집</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 스케줄 ID
     * @param request 스케줄 수정 요청 DTO
     * @return 수정된 스케줄 상세 응답 (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponseDto>> updateSchedule(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Update schedule request. scheduleId: {}, ownerId: {}", id, ownerId);
        
        ScheduleDetailResponseDto response = scheduleService.updateSchedule(ownerId, id, request);
        
        return ResponseEntity.ok(ApiResponse.success("스케줄이 수정되었습니다", response));
    }
    
    /**
     * 스케줄 수정 (비동기).
     * 
     * <p>스케줄 수정 요청을 비동기로 처리합니다. 즉시 202 Accepted 응답을 반환하고,
     * 실제 작업은 백그라운드에서 진행됩니다.</p>
     * 
     * <h3>응답 플로우:</h3>
     * <ol>
     *   <li>요청 즉시: 202 Accepted + taskId 반환</li>
     *   <li>클라이언트: GET /api/v1/tasks/{taskId}로 상태 확인 (폴링)</li>
     *   <li>완료 후: GET /api/v1/tasks/{taskId}/result로 결과 조회</li>
     * </ol>
     * 
     * <h3>요구사항 추적:</h3>
     * <ul>
     *   <li>{@code Async Processing Plan Phase 2}: 스케줄 수정 비동기화</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 스케줄 ID
     * @param request 스케줄 수정 요청 DTO
     * @return 작업 접수 응답 (202 Accepted)
     */
    @PutMapping("/{id}/async")
    public ResponseEntity<ApiResponse<AsyncTaskResponseDto>> updateScheduleAsync(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Update schedule async request. scheduleId: {}, ownerId: {}", id, ownerId);
        
        // 작업 생성
        String taskId = asyncTaskService.createTask("SCHEDULE_UPDATE", ownerId, request);
        
        // 비동기 작업 시작
        asyncScheduleService.updateScheduleAsync(taskId, ownerId, id, request);
        
        // 즉시 응답 (202 Accepted)
        AsyncTaskResponseDto response = AsyncTaskResponseDto.builder()
                .taskId(taskId)
                .status(TaskStatus.IN_PROGRESS)
                .taskType("SCHEDULE_UPDATE")
                .progress(0)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("스케줄 수정 요청이 접수되었습니다", response));
    }
}

