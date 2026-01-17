package vibe.scon.scon_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정.
 * 
 * <p>Spring @Async를 사용한 비동기 처리를 위한 Executor Bean을 설정합니다.</p>
 * 
 * <h3>Executor 종류:</h3>
 * <ul>
 *   <li>{@code taskExecutor}: 일반 비동기 작업 (큰 풀 크기)</li>
 *   <li>{@code dbExecutor}: DB 전용 작업 (작은 풀 크기, 커넥션 풀과 연동)</li>
 * </ul>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code Async Processing Plan Phase 1}: 인프라 구축</li>
 * </ul>
 * 
 * @see org.springframework.scheduling.annotation.Async
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 일반 비동기 작업용 Executor.
     * 
     * <p>큰 풀 크기와 큐를 가진 Executor로, 일반적인 비동기 작업에 사용됩니다.</p>
     * 
     * @return ThreadPoolTaskExecutor 인스턴스
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("TaskExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * DB 전용 비동기 작업 Executor.
     * 
     * <p>커넥션 풀 크기에 맞춘 작은 풀 크기의 Executor입니다.
     * SQLite의 경우 커넥션 풀 크기가 10개이므로, Executor는 5-7개로 제한됩니다.</p>
     * 
     * <p>참고: 커넥션 풀 크기의 50-70% 수준으로 설정합니다.</p>
     * 
     * @return ThreadPoolTaskExecutor 인스턴스
     */
    @Bean(name = "dbExecutor")
    public Executor dbExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 커넥션 풀 크기의 50-70% 수준으로 설정
        // 예: 커넥션 풀 10개 → Executor 5-7개
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(7);  // maximum-pool-size의 70%
        
        // 큐 크기는 커넥션 풀 크기의 10-20배
        executor.setQueueCapacity(100);
        
        executor.setThreadNamePrefix("async-db-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("DbExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}
