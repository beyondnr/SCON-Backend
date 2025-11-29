package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Owner;

import java.util.Optional;

/**
 * Owner 엔티티를 위한 JPA Repository.
 * 
 * <p>사장/운영자 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (인증 관련 쿼리)</li>
 *   <li>{@code TC-AUTH-001~003} - 회원가입 관련 테스트</li>
 *   <li>{@code TC-AUTH-004~005} - 로그인 관련 테스트</li>
 * </ul>
 * 
 * @see Owner
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 인증 API §8.1</a>
 */
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    
    /**
     * 이메일로 Owner 조회
     * 
     * @param email 이메일 주소
     * @return Optional<Owner>
     */
    Optional<Owner> findByEmail(String email);
    
    /**
     * 이메일 중복 여부 확인
     * 
     * @param email 이메일 주소
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
}

