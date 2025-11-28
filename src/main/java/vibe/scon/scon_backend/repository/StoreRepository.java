package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Store;

import java.util.List;

/**
 * Store 엔티티를 위한 JPA Repository.
 * 
 * <p>매장 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * @see Store
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

