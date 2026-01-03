package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.owner.OwnerProfileResponse;
import vibe.scon.scon_backend.dto.owner.UpdateOwnerProfileRequest;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;

/**
 * 사용자(Owner) 관리 서비스.
 * 
 * <p>사용자 프로필 조회 및 수정 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 조회/수정 API</li>
 * </ul>
 * 
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerService {

    private final OwnerRepository ownerRepository;

    /**
     * 현재 로그인한 사용자 프로필 조회.
     * 
     * <p>JWT 토큰에서 추출한 ownerId로 사용자 정보를 조회합니다.</p>
     * 
     * <h4>TC-OWNER-001 검증:</h4>
     * <ul>
     *   <li>사용자 정보가 정상적으로 조회됨</li>
     *   <li>모든 필드가 응답에 포함됨</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @return 사용자 프로필 응답 DTO
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    public OwnerProfileResponse getCurrentOwnerProfile(Long ownerId) {
        log.info("Getting owner profile for ownerId: {}", ownerId);

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.error("Owner not found with id: {}", ownerId);
                    return new ResourceNotFoundException("Owner", ownerId);
                });

        log.debug("Found owner: id={}, email={}, name={}, phone={}, createdAt={}, updatedAt={}", 
                owner.getId(), owner.getEmail(), owner.getName(), owner.getPhone(),
                owner.getCreatedAt(), owner.getUpdatedAt());

        OwnerProfileResponse response = OwnerProfileResponse.builder()
                .ownerId(owner.getId())
                .email(owner.getEmail())
                .name(owner.getName())
                .phone(owner.getPhone())
                .createdAt(owner.getCreatedAt())
                .updatedAt(owner.getUpdatedAt())
                .build();

        log.debug("Built OwnerProfileResponse: {}", response);
        return response;
    }

    /**
     * 현재 로그인한 사용자 프로필 수정.
     * 
     * <p>사용자의 이름과 전화번호를 수정합니다. 이메일은 변경할 수 없습니다.</p>
     * 
     * <h4>TC-OWNER-002 검증:</h4>
     * <ul>
     *   <li>name만 수정 가능</li>
     *   <li>phone만 수정 가능</li>
     *   <li>name과 phone 모두 수정 가능</li>
     *   <li>name을 빈 문자열로 설정 시도 시 400 에러</li>
     *   <li>phone을 빈 문자열로 전달 시 null로 처리</li>
     * </ul>
     * 
     * <h4>필드 제약사항:</h4>
     * <ul>
     *   <li>{@code name}: DB 제약사항(`nullable=false`)에 따라 빈 문자열 허용하지 않음</li>
     *   <li>{@code phone}: 선택 필드이므로 null 허용</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param request 프로필 수정 요청 DTO
     * @return 수정된 사용자 프로필 응답 DTO
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     * @throws BadRequestException name이 빈 문자열인 경우
     */
    @Transactional
    public OwnerProfileResponse updateOwnerProfile(
            Long ownerId,
            UpdateOwnerProfileRequest request
    ) {
        log.info("Updating owner profile for ownerId: {}", ownerId);

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));

        // name 업데이트 (제공된 경우에만, 빈 문자열 불가)
        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new BadRequestException("INVALID_NAME", "이름은 빈 문자열일 수 없습니다");
            }
            owner.updateName(trimmedName);
            log.debug("Updated name for ownerId: {}", ownerId);
        }

        // phone 업데이트 (제공된 경우, 빈 문자열은 null로 처리)
        if (request.getPhone() != null) {
            String trimmedPhone = request.getPhone().trim();
            owner.updatePhone(trimmedPhone.isEmpty() ? null : trimmedPhone);
            log.debug("Updated phone for ownerId: {}", ownerId);
        }

        ownerRepository.save(owner);
        log.info("Owner profile updated successfully. ownerId: {}", ownerId);

        return OwnerProfileResponse.builder()
                .ownerId(owner.getId())
                .email(owner.getEmail())
                .name(owner.getName())
                .phone(owner.getPhone())
                .createdAt(owner.getCreatedAt())
                .updatedAt(owner.getUpdatedAt())
                .build();
    }
}

