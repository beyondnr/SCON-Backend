package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Store;

import java.util.List;

/**
 * Store 엔티티를 위한 JPA Repository.
 * 
 * <p>매장 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집 (CRUD 쿼리)</li>
 *   <li>{@code TC-STORE-001~007} - 매장 API 테스트</li>
 * </ul>
 * 
 * @see Store
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 매장 API §8.2</a>
 */
public interface StoreRepository extends JpaRepository<Store, Long> {
    
    /**
     * 특정 Owner가 소유한 매장 목록 조회
     * 
     * @param ownerId Owner ID
     * @return 매장 목록
     */
    List<Store> findByOwnerId(Long ownerId);
}

