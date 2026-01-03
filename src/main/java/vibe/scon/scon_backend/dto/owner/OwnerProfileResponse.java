package vibe.scon.scon_backend.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO.
 * 
 * <p>마이페이지에서 사용자 프로필 정보를 조회할 때 사용합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 조회/수정 API</li>
 * </ul>
 * 
 * @see <a href="../../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProfileResponse {

    /**
     * 사용자 ID.
     */
    private Long ownerId;

    /**
     * 이메일 주소 (변경 불가).
     */
    private String email;

    /**
     * 이름 (필수 필드).
     */
    private String name;

    /**
     * 휴대폰 번호 (선택 필드, nullable).
     */
    private String phone;

    /**
     * 계정 생성 일시.
     */
    private LocalDateTime createdAt;

    /**
     * 정보 수정 일시.
     */
    private LocalDateTime updatedAt;
}

