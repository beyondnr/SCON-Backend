package vibe.scon.scon_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base entity class that provides common audit fields.
 * All JPA entities should extend this class to inherit:
 * <ul>
 *   <li>{@code id} - Auto-generated primary key</li>
 *   <li>{@code createdAt} - Timestamp when the entity was created</li>
 *   <li>{@code updatedAt} - Timestamp when the entity was last modified</li>
 * </ul>
 *
 * <p>Requires {@code @EnableJpaAuditing} configuration to be enabled
 * for automatic timestamp management.</p>
 *
 * @see vibe.scon.scon_backend.config.JpaConfig
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Primary key with auto-increment strategy.
     * Uses MySQL's AUTO_INCREMENT for ID generation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp when this entity was first persisted.
     * Automatically set by JPA Auditing on insert.
     * This field is immutable after creation.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this entity was last modified.
     * Automatically updated by JPA Auditing on every update.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

