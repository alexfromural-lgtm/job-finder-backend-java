package com.jobfinder.grpc;

import com.jobfinder.enums.Role;
import com.jobfinder.grpc.proto.*;
import com.jobfinder.queue.ApplyToJobPayload;
import com.jobfinder.queue.DbWriteQueueService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

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
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobSeekerGrpcService extends JobSeekerServiceGrpc.JobSeekerServiceImplBase {

    private final DbWriteQueueService queueService;

    // -----------------------------------------------------------------------
    // ApplyToJob — JOB_SEEKER required
    // -----------------------------------------------------------------------

    @Override
    public void applyToJob(ApplyToJobRequest request,
                           StreamObserver<QueuedOperationResponse> responseObserver) {
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

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
            if (jobId == null || jobId.isBlank()) {
                responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("job_id is required").asRuntimeException());
                return;
            }

            ApplyToJobPayload payload = ApplyToJobPayload.builder()
                .type("apply-to-job")
                .userId(userId.toString())
                .jobId(jobId)
                .coverLetter(request.getCoverLetter().isBlank() ? null : request.getCoverLetter())
                .build();

            String queueJobId = queueService.enqueue(payload);

            QueuedOperationResponse response = QueuedOperationResponse.newBuilder()
                .setQueueJobId(queueJobId)
                .setStatus("queued")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC ApplyToJob error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }
}
