package com.jobfinder.config;

// Spring annotations to inject property values, define beans, and mark configuration classes
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Spring's implementation of java.util.concurrent.Executor for managing thread pools
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// Standard Java interface for executing submitted Runnable tasks
import java.util.concurrent.Executor;

// Indicates that this class declares one or more @Bean methods and may be processed by the Spring container
@Configuration
public class AsyncConfig {

    // Injects the value of the 'app.queue.concurrency' property from properties file, defaulting to 5 if not specified
    @Value("${app.queue.concurrency:5}")
    private int concurrency;

    /**
     * Dedicated thread pool for the Redis queue worker.
     * Concurrency matches Bull's QUEUE_CONCURRENCY option (default 5).
     */
    // Defines a bean named "queueWorkerExecutor" managed by the Spring context
    @Bean(name = "queueWorkerExecutor")
    public Executor queueWorkerExecutor() {
        // Instantiates a new ThreadPoolTaskExecutor to customize thread pool properties
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Sets the core (minimum) number of threads to keep in the pool, equal to the concurrency property value
        executor.setCorePoolSize(concurrency);
        // Sets the maximum allowed number of threads in the pool, matching the concurrency property value
        executor.setMaxPoolSize(concurrency);
        // Sets queue capacity to 0, creating a SynchronousQueue which dispatches tasks immediately to threads or rejects them
        executor.setQueueCapacity(0); // SynchronousQueue — tasks dispatched immediately or rejected
        // Prefixes the names of threads created by this executor for easier debugging and monitoring
        executor.setThreadNamePrefix("queue-worker-");
        // Instructs the executor to wait for remaining tasks in the queue to complete during application shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // Sets the maximum time (30 seconds) that the executor should wait for tasks to finish during shutdown
        executor.setAwaitTerminationSeconds(30);
        // Initializes the executor, creating the underlying thread pool based on the configured properties
        executor.initialize();
        // Returns the configured executor bean
        return executor;
    }

    /**
     * General-purpose async executor for lightweight async calls.
     */
    // Defines a bean named "taskExecutor" for standard, non-blocking asynchronous operations
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Instantiates a new ThreadPoolTaskExecutor for general async tasks
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Sets the core pool size to 4 threads
        executor.setCorePoolSize(4);
        // Sets the maximum number of threads in the pool to 10
        executor.setMaxPoolSize(10);
        // Sets the queue capacity to 50, holding pending tasks before threads become available
        executor.setQueueCapacity(50);
        // Sets the prefix for names of threads created by this executor
        executor.setThreadNamePrefix("async-task-");
        // Initializes the thread pool
        executor.initialize();
        // Returns the configured task executor bean
        return executor;
    }
}
