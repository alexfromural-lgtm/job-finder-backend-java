package com.jobfinder.grpc;

import com.jobfinder.dto.response.QueueJobStatusResponse;
import com.jobfinder.grpc.proto.*;
import com.jobfinder.queue.DbWriteQueueService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC implementation of QueueService (queue.proto).
 *
 * RPCs:
 *   GetJobStatus — public (no auth required), mirrors GET /api/queue/job/{jobId}
 *
 * Clients poll this RPC after calling ApplyToJob to check whether the
 * async DB write has completed.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class QueueGrpcService extends QueueServiceGrpc.QueueServiceImplBase {

    private final DbWriteQueueService queueService;

    // -----------------------------------------------------------------------
    // GetJobStatus — public
    // -----------------------------------------------------------------------

    @Override
    public void getJobStatus(JobStatusRequest request,
                             StreamObserver<JobStatusResponse> responseObserver) {
        String queueJobId = request.getQueueJobId();
        if (queueJobId == null || queueJobId.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("queue_job_id is required").asRuntimeException());
            return;
        }

        try {
            QueueJobStatusResponse svc = queueService.getJobStatus(queueJobId);

            JobStatusResponse.Builder builder = JobStatusResponse.newBuilder()
                .setId(svc.id())
                .setType(nullSafe(svc.type()))
                .setStatus(nullSafe(svc.status()))
                .setAttemptsMade(svc.attemptsMade())
                .setCreatedAt(svc.createdAt() != null ? svc.createdAt().toString() : "")
                .setFailedReason(nullSafe(svc.failedReason()));

            // Serialize the result object to a JSON string for the proto field
            if (svc.result() != null) {
                builder.setResult(svc.result().toString());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetJobStatus error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
