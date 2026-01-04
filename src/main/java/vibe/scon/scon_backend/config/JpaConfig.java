package vibe.scon.scon_backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
 * <p>Additionally, this configuration ensures that the SQLite database directory
 * exists before the application starts, preventing connection errors.</p>
 * 
 * @see vibe.scon.scon_backend.entity.BaseEntity
 */
@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    /**
     * 애플리케이션 시작 시 SQLite 데이터베이스 디렉토리를 자동 생성합니다.
     * 데이터베이스 파일 경로에서 디렉토리 부분을 추출하여 생성합니다.
     * 
     * <p>이 메서드는 Spring 컨텍스트 초기화 후, 데이터소스 연결 전에 실행됩니다.</p>
     */
    @PostConstruct
    public void ensureDatabaseDirectoryExists() {
        try {
            // jdbc:sqlite:./data/scon_dev.db 형식에서 경로 추출
            String dbPath = datasourceUrl.replace("jdbc:sqlite:", "");
            
            // 상대 경로인 경우 현재 작업 디렉토리 기준으로 절대 경로 변환
            Path dbFilePath = Paths.get(dbPath);
            if (!dbFilePath.isAbsolute()) {
                String workingDir = System.getProperty("user.dir");
                dbFilePath = Paths.get(workingDir, dbPath);
            }
            
            // 디렉토리 경로 추출
            Path dbDirectory = dbFilePath.getParent();
            
            if (dbDirectory != null && !Files.exists(dbDirectory)) {
                Files.createDirectories(dbDirectory);
                log.info("Created database directory: {}", dbDirectory.toAbsolutePath());
            } else if (dbDirectory != null && Files.exists(dbDirectory)) {
                log.debug("Database directory already exists: {}", dbDirectory.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create database directory: {}", e.getMessage(), e);
            // 디렉토리 생성 실패해도 애플리케이션은 계속 시작 시도
            // (다른 방법으로 해결될 수 있음)
        }
    }
}

