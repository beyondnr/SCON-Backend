package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.onboarding.OnboardingCompleteRequestDto;
import vibe.scon.scon_backend.dto.onboarding.OnboardingCompleteResponseDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ForbiddenException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 온보딩 서비스.
 * 
 * <p>온보딩 완료 처리 및 첫 스케줄 자동 생성 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사</li>
 *   <li>{@code AC-005} - 온보딩 완료 및 첫 스케줄 생성</li>
 *   <li>{@code TC-ONBOARD-001} - 온보딩 플로우 전체</li>
 *   <li>{@code TC-ONBOARD-002} - Draft 스케줄 자동 생성</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.4</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final StoreRepository storeRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * 온보딩 완료 처리.
     * 
     * <p>온보딩을 완료하고 현재 주차의 Draft 스케줄을 자동 생성합니다.</p>
     * 
     * <h4>AC-005 검증:</h4>
     * <ul>
     *   <li>해당 매장에 현재 주차의 Draft 스케줄이 자동 생성되어야 한다</li>
     *   <li>대시보드에서 해당 스케줄을 조회/수정할 수 있어야 한다</li>
     * </ul>
     * 
     * <h4>TC-ONBOARD-002 검증:</h4>
     * <ul>
     *   <li>Draft 스케줄 자동 생성</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param request 온보딩 완료 요청 DTO
     * @return 온보딩 완료 응답 DTO
     */
    @Transactional
    public OnboardingCompleteResponseDto completeOnboarding(Long ownerId, OnboardingCompleteRequestDto request) {
        log.info("Processing onboarding completion. ownerId: {}, storeId: {}", ownerId, request.getStoreId());

        // 1. 매장 조회 및 소유권 검증
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("매장을 찾을 수 없습니다: " + request.getStoreId()));

        if (!store.getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized onboarding attempt. storeId: {}, requestedBy: {}", 
                    request.getStoreId(), ownerId);
            throw new ForbiddenException("해당 매장에 대한 접근 권한이 없습니다");
        }

        // 2. 현재 주차의 주 시작일 계산 (월요일 기준)
        LocalDate weekStartDate = calculateWeekStartDate();

        // 3. 이미 스케줄이 존재하는지 확인
        if (scheduleRepository.findByStoreIdAndWeekStartDate(store.getId(), weekStartDate).isPresent()) {
            log.warn("Schedule already exists for store. storeId: {}, weekStartDate: {}", 
                    store.getId(), weekStartDate);
            throw new BadRequestException("이미 해당 주차의 스케줄이 존재합니다");
        }

        // 4. Draft 스케줄 생성
        Schedule schedule = Schedule.builder()
                .weekStartDate(weekStartDate)
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Draft schedule created. scheduleId: {}, storeId: {}, weekStartDate: {}", 
                savedSchedule.getId(), store.getId(), weekStartDate);

        // 5. 응답 생성
        return OnboardingCompleteResponseDto.builder()
                .completed(true)
                .storeId(store.getId())
                .storeName(store.getName())
                .scheduleId(savedSchedule.getId())
                .weekStartDate(savedSchedule.getWeekStartDate())
                .scheduleStatus(savedSchedule.getStatus().name())
                .message("온보딩이 완료되었습니다. 첫 스케줄(Draft)이 생성되었습니다.")
                .build();
    }

    /**
     * 현재 주차의 주 시작일(월요일) 계산.
     * 
     * <p>현재 날짜 기준으로 이번 주 월요일 또는 다음 주 월요일을 반환합니다.
     * 토요일 이후라면 다음 주 월요일을 반환합니다.</p>
     * 
     * @return 주 시작일 (월요일)
     */
    private LocalDate calculateWeekStartDate() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 토요일(6) 또는 일요일(7)이면 다음 주 월요일 반환
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        // 그 외에는 이번 주 월요일 반환
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
