package com.aitestagent.exploration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExplorationConfig {

    /**
     * Dedicated thread pool for Playwright crawl tasks.
     *
     * <p>Named {@code "crawlExecutor"} so that {@code @Async("crawlExecutor")} in
     * {@link com.aitestagent.exploration.service.AsyncCrawlService} uses this pool
     * instead of the Spring default simple async executor.</p>
     *
     * <ul>
     *   <li>Core size 2  — two concurrent crawls can run at any time.</li>
     *   <li>Max size 5   — burst capacity for up to 5 concurrent crawls.</li>
     *   <li>Queue 10     — buffer if all threads are busy.</li>
     * </ul>
     */
    @Bean(name = "crawlExecutor")
    public Executor crawlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("crawl-");
        executor.initialize();
        return executor;
    }
}
