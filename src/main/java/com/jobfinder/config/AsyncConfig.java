package com.jobfinder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${app.queue.concurrency:5}")
    private int concurrency;

    /**
     * Dedicated thread pool for the Redis queue worker.
     * Concurrency matches Bull's QUEUE_CONCURRENCY option (default 5).
     */
    @Bean(name = "queueWorkerExecutor")
    public Executor queueWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(0); // SynchronousQueue — tasks dispatched immediately or rejected
        executor.setThreadNamePrefix("queue-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * General-purpose async executor for lightweight async calls.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }
}
