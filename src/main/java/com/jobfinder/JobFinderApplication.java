package com.jobfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
    net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
})
@EnableAsync
public class JobFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobFinderApplication.class, args);
    }
}
