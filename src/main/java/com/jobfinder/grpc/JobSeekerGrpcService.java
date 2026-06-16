package com.jobfinder.grpc;

// Import custom system roles
import com.jobfinder.enums.Role;
// Import the generated gRPC classes from proto specifications
import com.jobfinder.grpc.proto.*;
// Import the background queue payloads and queue service
import com.jobfinder.queue.ApplyToJobPayload;
import com.jobfinder.queue.DbWriteQueueService;
// Import gRPC status codes and StreamObserver callback interfaces
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
// Lombok constructor generation and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Annotation declaring this class as a gRPC global service bean
import net.devh.boot.grpc.server.service.GrpcService;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * gRPC implementation of JobSeekerService (jobseeker.proto).
 *
 * RPCs:
 *   ApplyToJob — requires JOB_SEEKER role
 *
 * Enqueues the application operation exactly as the REST controller does,
 * returning a queue job ID that the client can poll via QueueService.GetJobStatus.
 */
// Registers this class as a gRPC service bean
@GrpcService
// Generates standard constructor injecting dependencies
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class JobSeekerGrpcService extends JobSeekerServiceGrpc.JobSeekerServiceImplBase {

    // Inject DbWriteQueueService bean
    private final DbWriteQueueService queueService;

    // -----------------------------------------------------------------------
    // ApplyToJob — JOB_SEEKER required
    // -----------------------------------------------------------------------

    // Receives a job application request, enqueues the action asynchronously, and returns a queue job ID for status polling
    @Override
    public void applyToJob(ApplyToJobRequest request,
                           StreamObserver<QueuedOperationResponse> responseObserver) {
        // Retrieve authenticated user ID from gRPC context thread
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

        // Verify that the caller holds the JOB_SEEKER role prior to applying
        List<Role> roles = JwtServerInterceptor.ROLES_CTX_KEY.get();
        if (roles == null || !roles.contains(Role.JOB_SEEKER)) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("JOB_SEEKER role required")
                    .asRuntimeException());
            return;
        }

        try {
            String jobId = request.getJobId();
            // Validate presence of the required jobId field
            if (jobId == null || jobId.isBlank()) {
                responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("job_id is required").asRuntimeException());
                return;
            }

            // Build the queue message payload mapping the applicant, job, and cover letter
            ApplyToJobPayload payload = ApplyToJobPayload.builder()
                .type("apply-to-job")
                .userId(userId.toString())
                .jobId(jobId)
                .coverLetter(request.getCoverLetter().isBlank() ? null : request.getCoverLetter())
                .build();

            // Push the application task to the Redis backend queue and receive its unique job ID
            String queueJobId = queueService.enqueue(payload);

            // Construct the protobuf response carrying the tracking status and job ID
            QueuedOperationResponse response = QueuedOperationResponse.newBuilder()
                .setQueueJobId(queueJobId)
                .setStatus("queued")
                .build();

            // Return queued status response details to the gRPC client
            responseObserver.onNext(response);
            // Signal stream completion
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC ApplyToJob error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }
}
