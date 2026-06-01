package com.jobfinder.grpc;

import com.jobfinder.dto.request.CreateJobRequest;
import com.jobfinder.dto.response.JobResponse;
import com.jobfinder.dto.response.PagedJobResponse;
import com.jobfinder.enums.Role;
import com.jobfinder.grpc.proto.*;
import com.jobfinder.service.JobService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

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
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class JobGrpcService extends JobServiceGrpc.JobServiceImplBase {

    private final JobService jobService;

    // -----------------------------------------------------------------------
    // ListJobs — server-side streaming (public)
    // -----------------------------------------------------------------------

    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<JobMessage> responseObserver) {
        try {
            int page     = request.getPage()     > 0 ? request.getPage()     : 1;
            int pageSize = request.getPageSize()  > 0 ? request.getPageSize() : 10;

            String category = blankToNull(request.getCategory());
            String location = blankToNull(request.getLocation());
            String search   = blankToNull(request.getSearch());

            PagedJobResponse paged = jobService.getAllJobs(category, location, search, page, pageSize);

            // Stream each job individually — the key demonstration of gRPC streaming
            for (JobResponse job : paged.jobs()) {
                responseObserver.onNext(toJobMessage(job));
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC ListJobs error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // GetJob — unary (public)
    // -----------------------------------------------------------------------

    @Override
    public void getJob(JobIdRequest request, StreamObserver<JobMessage> responseObserver) {
        try {
            UUID jobId = UUID.fromString(request.getJobId());
            JobResponse job = jobService.getJobById(jobId);
            responseObserver.onNext(toJobMessage(job));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription("Invalid job ID format").asRuntimeException());
        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetJob error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // CreateJob — unary (RECRUITER required)
    // -----------------------------------------------------------------------

    @Override
    public void createJob(com.jobfinder.grpc.proto.CreateJobRequest request,
                          StreamObserver<JobMessage> responseObserver) {
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

        List<Role> roles = JwtServerInterceptor.ROLES_CTX_KEY.get();
        if (roles == null || !roles.contains(Role.RECRUITER)) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("RECRUITER role required")
                    .asRuntimeException());
            return;
        }

        try {
            CreateJobRequest svcReq = new CreateJobRequest(
                request.getTitle(),
                request.getDescription(),
                request.getRequirements(),
                request.getLocation(),
                request.getSalaryRange(),
                request.getCategory()
            );

            JobResponse created = jobService.createJob(userId, svcReq);
            responseObserver.onNext(toJobMessage(created));
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC CreateJob error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // Mapper
    // -----------------------------------------------------------------------

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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
