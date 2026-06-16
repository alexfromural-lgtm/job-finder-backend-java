package com.jobfinder.grpc;

// Import background job status DTO records
import com.jobfinder.dto.response.QueueJobStatusResponse;
// Import the generated gRPC classes from proto specifications
import com.jobfinder.grpc.proto.*;
// Import the background write queue service bean
import com.jobfinder.queue.DbWriteQueueService;
// Import gRPC status codes and StreamObserver callback interfaces
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
// Lombok constructor generation and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Annotation declaring this class as a gRPC global service bean
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
// Registers this class as a gRPC service bean
@GrpcService
// Generates standard constructor injecting dependencies
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class QueueGrpcService extends QueueServiceGrpc.QueueServiceImplBase {

    // Inject DbWriteQueueService bean
    private final DbWriteQueueService queueService;

    // -----------------------------------------------------------------------
    // GetJobStatus — public
    // -----------------------------------------------------------------------

    // Fetches execution status updates for a background job payload
    @Override
    public void getJobStatus(JobStatusRequest request,
                             StreamObserver<JobStatusResponse> responseObserver) {
        String queueJobId = request.getQueueJobId();
        // Validate presence of the required queue_job_id field
        if (queueJobId == null || queueJobId.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("queue_job_id is required").asRuntimeException());
            return;
        }

        try {
            // Retrieve background job tracking status from database/Redis hash
            QueueJobStatusResponse svc = queueService.getJobStatus(queueJobId);

            // Construct the protobuf JobStatusResponse containing tracking properties
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

            // Return status details to client
            responseObserver.onNext(builder.build());
            // Signal stream completion
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            // Return NOT_FOUND status code if job is missing
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetJobStatus error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // Ensures String properties are non-null inside protobuf response contexts
    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
