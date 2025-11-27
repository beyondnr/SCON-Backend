package vibe.scon.scon_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration class.
 * Enables JPA Auditing for automatic timestamp management.
 * 
 * <p>With this configuration enabled, the following annotations in BaseEntity
 * will work automatically:</p>
 * <ul>
 *   <li>{@code @CreatedDate} - Automatically sets creation timestamp</li>
 *   <li>{@code @LastModifiedDate} - Automatically updates modification timestamp</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.entity.BaseEntity
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing configuration is enabled via @EnableJpaAuditing annotation.
    // No additional beans required for basic auditing functionality.
}

