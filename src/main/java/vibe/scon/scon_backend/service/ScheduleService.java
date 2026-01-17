package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.schedule.ScheduleDetailResponseDto;
import vibe.scon.scon_backend.dto.schedule.ShiftRequestDto;
import vibe.scon.scon_backend.dto.schedule.UpdateScheduleRequestDto;
import vibe.scon.scon_backend.entity.Employee;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Shift;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ForbiddenException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.EmployeeRepository;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.ShiftRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄 관리 서비스.
 * 
 * <p>스케줄 조회 및 수정 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-007} - 드래그&드롭 스케줄 편집</li>
 *   <li>{@code INTG-BE-Phase6} - 스케줄 편집 기능</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    /**
     * 스케줄 상세 조회.
     * 
     * <p>스케줄 ID로 상세 정보를 조회합니다. Shift 정보를 포함합니다.</p>
     * 
     * @param ownerId 인증된 Owner ID
     * @param scheduleId 스케줄 ID
     * @return 스케줄 상세 응답 DTO (Shift 정보 포함)
     * @throws ResourceNotFoundException 스케줄을 찾을 수 없는 경우
     * @throws ForbiddenException 본인 소유 매장의 스케줄이 아닌 경우
     */
    public ScheduleDetailResponseDto getScheduleDetail(Long ownerId, Long scheduleId) {
        log.debug("Getting schedule detail. scheduleId: {}, ownerId: {}", scheduleId, ownerId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다: " + scheduleId));

        // 매장 소유권 확인 (StoreService 활용)
        validateScheduleOwnership(schedule, ownerId);

        // Shift 정보 로드 (N+1 문제 방지를 위해 엔티티 그래프 고려)
        // 현재는 LAZY 로딩이지만, ScheduleDetailResponseDto.from()에서 shifts를 순회하므로
        // 트랜잭션 내에서 자동으로 로드됨
        
        return ScheduleDetailResponseDto.from(schedule);
    }

    /**
     * 스케줄 수정.
     * 
     * <p>스케줄 상태 및 Shift 정보를 수정합니다.</p>
     * 
     * <h3>상태 수정 규칙:</h3>
     * <ul>
     *   <li>DRAFT, PENDING 상태만 수정 가능</li>
     *   <li>APPROVED, PUBLISHED 상태는 수정 불가</li>
     * </ul>
     * 
     * <h3>Shift 처리 방식 (Full Replace):</h3>
     * <ul>
     *   <li>기존 Shift 전체 삭제</li>
     *   <li>새로운 Shift 생성 및 추가</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param scheduleId 스케줄 ID
     * @param request 스케줄 수정 요청 DTO
     * @return 수정된 스케줄 상세 응답 DTO
     * @throws ResourceNotFoundException 스케줄을 찾을 수 없는 경우
     * @throws ForbiddenException 본인 소유 매장의 스케줄이 아닌 경우
     * @throws BadRequestException 상태로 인한 수정 불가 또는 비즈니스 로직 검증 실패
     */
    @Transactional
    public ScheduleDetailResponseDto updateSchedule(Long ownerId, Long scheduleId, UpdateScheduleRequestDto request) {
        log.info("Updating schedule. scheduleId: {}, ownerId: {}", scheduleId, ownerId);

        if (!request.hasUpdate()) {
            throw new BadRequestException("수정할 내용이 없습니다");
        }

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다: " + scheduleId));

        // 매장 소유권 확인
        validateScheduleOwnership(schedule, ownerId);

        // 스케줄 상태 검증 (DRAFT, PENDING만 수정 가능)
        if (schedule.getStatus() == ScheduleStatus.APPROVED || schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            throw new BadRequestException(
                    String.format("승인된 스케줄(%s)은 수정할 수 없습니다", schedule.getStatus()));
        }

        // 상태 업데이트
        if (request.getStatus() != null) {
            schedule.changeStatus(request.getStatus());
            log.debug("Schedule status updated. scheduleId: {}, newStatus: {}", scheduleId, request.getStatus());
        }

        // Shift 정보 업데이트 (Full Replace 방식)
        if (request.getShifts() != null) {
            updateShifts(schedule, request.getShifts());
            log.debug("Shifts updated. scheduleId: {}, shiftCount: {}", scheduleId, request.getShifts().size());
        }

        // 스케줄 저장 (cascade로 인해 Shift도 함께 저장됨)
        scheduleRepository.save(schedule);
        
        log.info("Schedule updated successfully. scheduleId: {}", scheduleId);

        // 저장된 엔티티 다시 조회하여 Shift 정보 포함 확인 (Lazy Loading 문제 방지)
        schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다: " + scheduleId));

        return ScheduleDetailResponseDto.from(schedule);
    }

    /**
     * Shift 정보 업데이트 (Full Replace 방식).
     * 
     * <p>기존 Shift를 모두 삭제하고 새로운 Shift를 생성합니다.</p>
     * 
     * @param schedule 스케줄 엔티티
     * @param shiftRequests 새로운 Shift 요청 목록
     * @throws BadRequestException 비즈니스 로직 검증 실패
     */
    private void updateShifts(Schedule schedule, List<ShiftRequestDto> shiftRequests) {
        // 기존 Shift 삭제 (orphanRemoval 활용)
        // Schedule 엔티티에 cascade = CascadeType.ALL, orphanRemoval = true 설정되어 있으므로,
        // 컬렉션을 clear하면 orphanRemoval로 인해 자동 삭제됨
        // 명시적 삭제보다 컬렉션 clear가 더 안전함 (트랜잭션 내에서 일관성 보장)
        List<Shift> existingShifts = new ArrayList<>(schedule.getShifts()); // 복사본 생성 (ConcurrentModificationException 방지)
        schedule.getShifts().clear(); // orphanRemoval로 인해 자동 삭제됨
        
        // 명시적으로 삭제하여 즉시 반영 (orphanRemoval이 트랜잭션 커밋 시 발생하므로, 명시적 삭제로 확실하게 처리)
        if (!existingShifts.isEmpty()) {
            shiftRepository.deleteAll(existingShifts);
            log.debug("Existing shifts deleted. scheduleId: {}, count: {}", schedule.getId(), existingShifts.size());
        }

        // 새로운 Shift 생성 및 검증
        Store store = schedule.getStore();
        LocalDate weekStartDate = schedule.getWeekStartDate();
        LocalDate weekEndDate = weekStartDate.plusDays(6); // 주의 마지막 날짜 (일요일)

        for (ShiftRequestDto shiftRequest : shiftRequests) {
            // 직원 존재 여부 및 매장 소유권 검증
            Employee employee = employeeRepository.findById(shiftRequest.getEmployeeId())
                    .orElseThrow(() -> new BadRequestException(
                            String.format("직원을 찾을 수 없습니다: %d", shiftRequest.getEmployeeId())));

            // 직원이 해당 매장에 속하는지 확인
            if (!employee.getStore().getId().equals(store.getId())) {
                throw new BadRequestException(
                        String.format("직원(%d)이 해당 매장(%d)에 속하지 않습니다", 
                                employee.getId(), store.getId()));
            }

            // 비즈니스 로직 검증
            validateShiftBusinessRules(shiftRequest, weekStartDate, weekEndDate);

            // Shift 엔티티 생성 및 추가
            Shift shift = Shift.builder()
                    .workDate(shiftRequest.getWorkDate())
                    .startTime(shiftRequest.getStartTime())
                    .endTime(shiftRequest.getEndTime())
                    .schedule(schedule)
                    .employee(employee)
                    .build();

            schedule.addShift(shift); // 연관관계 편의 메서드 사용 (cascade로 인해 자동 저장됨)
        }

        log.debug("Shifts created. scheduleId: {}, count: {}", schedule.getId(), shiftRequests.size());
    }

    /**
     * Shift 비즈니스 로직 검증.
     * 
     * @param shiftRequest Shift 요청 DTO
     * @param weekStartDate 주 시작일 (월요일)
     * @param weekEndDate 주 종료일 (일요일)
     * @throws BadRequestException 검증 실패
     */
    private void validateShiftBusinessRules(ShiftRequestDto shiftRequest, 
                                           LocalDate weekStartDate, 
                                           LocalDate weekEndDate) {
        // 시작 시간 < 종료 시간 검증
        if (!shiftRequest.getStartTime().isBefore(shiftRequest.getEndTime())) {
            throw new BadRequestException(
                    String.format("시작 시간(%s)은 종료 시간(%s)보다 이전이어야 합니다",
                            shiftRequest.getStartTime(), shiftRequest.getEndTime()));
        }

        // 근무일이 스케줄 주차 내인지 검증
        LocalDate workDate = shiftRequest.getWorkDate();
        if (workDate.isBefore(weekStartDate) || workDate.isAfter(weekEndDate)) {
            throw new BadRequestException(
                    String.format("근무일(%s)은 스케줄 주차(%s ~ %s) 내에 있어야 합니다",
                            workDate, weekStartDate, weekEndDate));
        }

        // 근무 시간 최소/최대 검증 (선택적, 필요 시 추가)
        // 예: 최소 1시간, 최대 12시간
        long workHours = java.time.Duration.between(
                shiftRequest.getStartTime(), shiftRequest.getEndTime()).toHours();
        
        if (workHours < 1) {
            throw new BadRequestException("근무 시간은 최소 1시간 이상이어야 합니다");
        }
        
        if (workHours > 12) {
            throw new BadRequestException("근무 시간은 최대 12시간 이하여야 합니다");
        }
    }

    /**
     * 스케줄 소유권 검증.
     * 
     * <p>스케줄이 해당 Owner의 매장에 속하는지 확인합니다.</p>
     * 
     * @param schedule 검증할 스케줄
     * @param ownerId Owner ID
     * @throws ForbiddenException 본인 소유 매장의 스케줄이 아닌 경우
     */
    private void validateScheduleOwnership(Schedule schedule, Long ownerId) {
        Store store = schedule.getStore();
        if (!store.getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized schedule access. scheduleId: {}, requestedBy: {}, actualOwner: {}",
                    schedule.getId(), ownerId, store.getOwner().getId());
            throw new ForbiddenException("해당 스케줄에 대한 접근 권한이 없습니다");
        }
    }
}
