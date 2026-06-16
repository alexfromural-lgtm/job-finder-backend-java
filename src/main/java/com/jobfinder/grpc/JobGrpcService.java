package com.jobfinder.grpc;

// Import our request and response DTO records
import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.response.JobResponse;
import com.jobfinder.dto.response.PagedJobResponse;
// Import custom system roles
import com.jobfinder.enums.Role;
// Import the generated gRPC classes from proto specifications
import com.jobfinder.grpc.proto.*;
// Import JobService bean to delegate business actions
import com.jobfinder.service.JobService;
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
 * gRPC implementation of JobService (job.proto).
 *
 * RPCs:
 *   ListJobs   — SERVER-SIDE STREAMING, public (no auth)
 *   GetJob     — unary, public
 *   CreateJob  — unary, RECRUITER role required
 *
 * ListJobs demonstrates gRPC streaming: each JobMessage is sent
 * to the client as it is fetched, rather than batching the whole page.
 */
// Registers this class as a gRPC service bean
@GrpcService
// Generates standard constructor injecting dependencies
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class JobGrpcService extends JobServiceGrpc.JobServiceImplBase {

    // Inject JobService bean
    private final JobService jobService;

    // -----------------------------------------------------------------------
    // ListJobs — server-side streaming (public)
    // -----------------------------------------------------------------------

    // Fetches and streams job listings back to the client page-by-page
    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<JobMessage> responseObserver) {
        try {
            // Apply default boundaries for pagination indices
            int page     = request.getPage()     > 0 ? request.getPage()     : 1;
            int pageSize = request.getPageSize()  > 0 ? request.getPageSize() : 10;

            // Normalize empty filter inputs to null parameters
            String category = blankToNull(request.getCategory());
            String location = blankToNull(request.getLocation());
            String search   = blankToNull(request.getSearch());

            // Query paginated search results from database
            PagedJobResponse paged = jobService.getAllJobs(category, location, search, page, pageSize);

            // Stream each job individually — the key demonstration of gRPC streaming
            for (JobResponse job : paged.jobs()) {
                responseObserver.onNext(toJobMessage(job));
            }

            // Signal stream completion
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC ListJobs error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // GetJob — unary (public)
    // -----------------------------------------------------------------------

    // Returns details of a specific job listing matching the request unique ID
    @Override
    public void getJob(JobIdRequest request, StreamObserver<JobMessage> responseObserver) {
        try {
            // Parse job UUID
            UUID jobId = UUID.fromString(request.getJobId());
            // Fetch job details using JobService
            JobResponse job = jobService.getJobById(jobId);
            // Return job payload
            responseObserver.onNext(toJobMessage(job));
            // Complete the stream pipeline
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            // Return INVALID_ARGUMENT error if ID parser fails
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid job ID format").asRuntimeException());
        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            // Return NOT_FOUND if job listing is missing
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetJob error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // CreateJob — unary (RECRUITER required)
    // -----------------------------------------------------------------------

    // Submits and registers a new job listing
    @Override
    public void createJob(com.jobfinder.grpc.proto.CreateJobRequest request,
                          StreamObserver<JobMessage> responseObserver) {
        // Retrieve authenticated user ID from gRPC context thread
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

        // Verify that the caller holds the RECRUITER role prior to creating
        List<Role> roles = JwtServerInterceptor.ROLES_CTX_KEY.get();
        if (roles == null || !roles.contains(Role.RECRUITER)) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("RECRUITER role required")
                    .asRuntimeException());
            return;
        }

        try {
            // Build the CreateJobRequest record payload
            CreateJobRequest svcReq = new CreateJobRequest(
                request.getTitle(),
                request.getDescription(),
                request.getRequirements(),
                request.getLocation(),
                request.getSalaryRange(),
                request.getCategory()
            );

            // Execute creation logic via JobService
            JobResponse created = jobService.createJob(userId, svcReq);
            // Return created job metadata to client
            responseObserver.onNext(toJobMessage(created));
            // Complete stream pipeline
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            // Return NOT_FOUND if recruiter profile is missing
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC CreateJob error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // Mapper
    // -----------------------------------------------------------------------

    // Maps JobResponse DTO records to Protobuf JobMessage builders
    private JobMessage toJobMessage(JobResponse job) {
        return JobMessage.newBuilder()
            .setJobId(job.id().toString())
            .setRecruiterId(job.recruiterId().toString())
            .setCompanyName(nullSafe(job.companyName()))
            .setIndustry(nullSafe(job.industry()))
            .setCompanyWebsite(nullSafe(job.companyWebsite()))
            .setTitle(nullSafe(job.title()))
            .setDescription(nullSafe(job.description()))
            .setRequirements(nullSafe(job.requirements()))
            .setLocation(nullSafe(job.location()))
            .setSalaryRange(nullSafe(job.salaryRange()))
            .setCategory(nullSafe(job.category()))
            .setIsActive(job.isActive())
            .setCreatedAt(job.createdAt() != null ? job.createdAt().toString() : "")
            .setUpdatedAt(job.updatedAt() != null ? job.updatedAt().toString() : "")
            .build();
    }

    // Normalizes empty string query parameter inputs to null values
    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // Ensures String properties are non-null inside protobuf response contexts
    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
