package vibe.scon.scon_backend.dto.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.Store;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 매장 응답 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집</li>
 *   <li>{@code AC-003} - 매장 정보 저장 및 조회</li>
 *   <li>{@code TC-STORE-002} - 매장 조회 API</li>
 *   <li>{@code TC-STORE-005} - 내 매장 목록 조회 API</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.2</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponseDto {

    /**
     * 매장 ID.
     */
    private Long id;

    /**
     * 매장명.
     */
    private String name;

    /**
     * 업종.
     */
    private String businessType;

    /**
     * 매장 주소.
     */
    private String address;

    /**
     * 영업 시작 시간.
     */
    private LocalTime openTime;

    /**
     * 영업 종료 시간.
     */
    private LocalTime closeTime;

    /**
     * 소유자 ID.
     */
    private Long ownerId;

    /**
     * 소속 직원 수.
     */
    private Integer employeeCount;

    /**
     * 생성 시각.
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시각.
     */
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환.
     * 
     * @param store Store 엔티티
     * @return StoreResponseDto
     */
    public static StoreResponseDto from(Store store) {
        return StoreResponseDto.builder()
                .id(store.getId())
                .name(store.getName())
                .businessType(store.getBusinessType())
                .address(store.getAddress())
                .openTime(store.getOpenTime())
                .closeTime(store.getCloseTime())
                .ownerId(store.getOwner().getId())
                .employeeCount(store.getEmployees() != null ? store.getEmployees().size() : 0)
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }
}
