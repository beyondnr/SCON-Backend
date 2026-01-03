package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.dto.store.StoreResponseDto;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.exception.ForbiddenException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 매장 관리 서비스.
 * 
 * <p>매장 CRUD 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집</li>
 *   <li>{@code AC-003} - 매장 정보 저장 및 조회</li>
 *   <li>{@code REQ-NF-001} - API 응답 시간 p95 ≤ 0.8s</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.2</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final OwnerRepository ownerRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * 매장 생성.
     * 
     * <p>인증된 사장님의 새 매장을 생성하고, 현재 주차의 Draft 스케줄을 자동 생성합니다.</p>
     * 
     * <h4>TC-STORE-001 검증:</h4>
     * <ul>
     *   <li>Store 엔티티가 Owner와 연결되어 저장됨</li>
     *   <li>생성된 매장 정보 반환</li>
     * </ul>
     * 
     * <h4>UX 플로우 (2026-01-02):</h4>
     * <ul>
     *   <li>매장 생성 시 현재 주차의 Draft 스케줄이 자동 생성됨</li>
     *   <li>온보딩 완료 플래그 역할 (매장 생성 = 온보딩 완료)</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param request 매장 생성 요청 DTO
     * @return 생성된 매장 응답 DTO
     */
    @Transactional
    public StoreResponseDto createStore(Long ownerId, StoreRequestDto request) {
        log.info("Creating store for ownerId: {}, storeName: {}", ownerId, request.getName());

        // Owner 조회
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + ownerId));

        // Store 엔티티 생성
        Store store = Store.builder()
                .name(request.getName())
                .businessType(request.getBusinessType())
                .address(request.getAddress())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .storeHoliday(request.getStoreHoliday())
                .owner(owner)
                .build();

        Store savedStore = storeRepository.save(store);
        log.info("Store created successfully. storeId: {}, ownerId: {}", savedStore.getId(), ownerId);

        // Draft 스케줄 자동 생성 (UX 문서 요구사항: Step 2 완료 시 자동 생성)
        createDraftScheduleIfNotExists(savedStore);

        return StoreResponseDto.from(savedStore);
    }

    /**
     * 매장 상세 조회.
     * 
     * <p>매장 ID로 상세 정보를 조회합니다. 본인 소유 매장만 조회 가능합니다.</p>
     * 
     * <h4>TC-STORE-002, TC-STORE-006 검증:</h4>
     * <ul>
     *   <li>매장 상세 정보 반환</li>
     *   <li>타 사용자 매장 접근 시 403 반환</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param storeId 매장 ID
     * @return 매장 응답 DTO
     * @throws ResourceNotFoundException 매장을 찾을 수 없는 경우 (TC-STORE-007)
     * @throws BusinessException 본인 소유 매장이 아닌 경우 (TC-STORE-006)
     */
    public StoreResponseDto getStore(Long ownerId, Long storeId) {
        log.debug("Getting store. storeId: {}, ownerId: {}", storeId, ownerId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("매장을 찾을 수 없습니다: " + storeId));

        // 본인 소유 매장 확인 (TC-STORE-006)
        validateOwnership(store, ownerId);

        return StoreResponseDto.from(store);
    }

    /**
     * 내 매장 목록 조회.
     * 
     * <p>인증된 사장님이 소유한 모든 매장 목록을 조회합니다.</p>
     * 
     * <h4>TC-STORE-005 검증:</h4>
     * <ul>
     *   <li>본인 소유 매장 목록만 반환</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @return 매장 목록
     */
    public List<StoreResponseDto> getMyStores(Long ownerId) {
        log.debug("Getting stores for ownerId: {}", ownerId);

        List<Store> stores = storeRepository.findByOwnerId(ownerId);
        
        return stores.stream()
                .map(StoreResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 매장 정보 수정.
     * 
     * <p>본인 소유 매장의 정보를 수정합니다.</p>
     * 
     * <h4>TC-STORE-004 검증:</h4>
     * <ul>
     *   <li>매장 정보 수정 성공</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param storeId 매장 ID
     * @param request 매장 수정 요청 DTO
     * @return 수정된 매장 응답 DTO
     */
    @Transactional
    public StoreResponseDto updateStore(Long ownerId, Long storeId, StoreRequestDto request) {
        log.info("Updating store. storeId: {}, ownerId: {}", storeId, ownerId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("매장을 찾을 수 없습니다: " + storeId));

        // 본인 소유 매장 확인
        validateOwnership(store, ownerId);

        // 매장 정보 업데이트 (JPA dirty checking)
        store.update(
                request.getName(),
                request.getBusinessType(),
                request.getAddress(),
                request.getOpenTime(),
                request.getCloseTime(),
                request.getStoreHoliday()
        );

        log.info("Store updated successfully. storeId: {}", storeId);
        
        return StoreResponseDto.from(store);
    }

    /**
     * 매장 소유권 검증.
     * 
     * @param store 검증할 매장
     * @param ownerId 인증된 Owner ID
     * @throws ForbiddenException 본인 소유 매장이 아닌 경우
     */
    private void validateOwnership(Store store, Long ownerId) {
        if (!store.getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized store access. storeId: {}, requestedBy: {}, actualOwner: {}",
                    store.getId(), ownerId, store.getOwner().getId());
            throw new ForbiddenException("해당 매장에 대한 접근 권한이 없습니다");
        }
    }

    /**
     * Draft 스케줄 자동 생성 (매장 생성 시).
     * 
     * <p>현재 주차의 Draft 스케줄이 없으면 자동으로 생성합니다.
     * 이미 존재하는 경우 생성하지 않습니다.</p>
     * 
     * <h4>UX 플로우 (2026-01-02):</h4>
     * <ul>
     *   <li>Step 2(매장 정보) 완료 시 Draft 스케줄 자동 생성</li>
     *   <li>온보딩 완료 플래그 역할</li>
     * </ul>
     * 
     * @param store 생성할 스케줄의 매장
     */
    private void createDraftScheduleIfNotExists(Store store) {
        // 현재 주차의 주 시작일 계산 (월요일 기준)
        LocalDate weekStartDate = calculateWeekStartDate();

        // 이미 스케줄이 존재하는지 확인
        if (scheduleRepository.findByStoreIdAndWeekStartDate(store.getId(), weekStartDate).isPresent()) {
            log.debug("Schedule already exists for store. storeId: {}, weekStartDate: {}. Skipping auto-creation.",
                    store.getId(), weekStartDate);
            return;
        }

        // Draft 스케줄 생성
        Schedule schedule = Schedule.builder()
                .weekStartDate(weekStartDate)
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Draft schedule auto-created for new store. scheduleId: {}, storeId: {}, weekStartDate: {}",
                savedSchedule.getId(), store.getId(), weekStartDate);
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
