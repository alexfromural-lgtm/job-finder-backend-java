package com.jobfinder;

// Spring Boot core imports for application bootstrapping and auto-configuration
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Spring scheduling import to enable asynchronous task execution capability
import org.springframework.scheduling.annotation.EnableAsync;

// Declares a Spring Boot application. We exclude the default gRPC server security auto-configuration
// so that custom authentication and security logic can be applied to gRPC services.
@SpringBootApplication(exclude = {
    net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
})
// Enables Spring's asynchronous method execution capability, allowing methods annotated with @Async to run in a background thread pool
@EnableAsync
public class JobFinderApplication {

    // The main entry point of the Java application, invoked by the JVM
    public static void main(String[] args) {
        // Launches the Spring Boot application context, initializing the application with the specified arguments
        SpringApplication.run(JobFinderApplication.class, args);
    }
}
