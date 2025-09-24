package com.example.grpcdemo.service;

import com.example.grpcdemo.model.Job;
import com.example.grpcdemo.proto.CreateJobRequest;
import com.example.grpcdemo.proto.JobRequest;
import com.example.grpcdemo.proto.JobResponse;
import com.example.grpcdemo.proto.JobServiceGrpc;
import com.example.grpcdemo.proto.ListJobsRequest;
import com.example.grpcdemo.proto.ListJobsResponse;
import com.example.grpcdemo.proto.UpdateJobRequest;
import com.example.grpcdemo.repository.JobRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for jobs backed by JPA.
 */
@GrpcService
public class JobServiceImpl extends JobServiceGrpc.JobServiceImplBase {

    private final JobRepository repository;

    public JobServiceImpl(JobRepository repository) {
        this.repository = repository;
    }

    @Override
    public void getJob(JobRequest request, StreamObserver<JobResponse> responseObserver) {
        JobResponse response = repository.findById(request.getJobId())
                .map(this::toResponse)
                .orElse(JobResponse.newBuilder()
                        .setJobId(request.getJobId())
                        .setStatus("NOT_FOUND")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createJob(CreateJobRequest request, StreamObserver<JobResponse> responseObserver) {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setJobTitle(request.getJobTitle());
        job.setDescription(request.getDescription());
        job.setStatus("CREATED");
        Job saved = repository.save(job);

        responseObserver.onNext(toResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    public void updateJob(UpdateJobRequest request, StreamObserver<JobResponse> responseObserver) {
        Job job = repository.findById(request.getJobId()).orElse(null);
        if (job == null) {
            responseObserver.onNext(JobResponse.newBuilder()
                    .setJobId(request.getJobId())
                    .setStatus("NOT_FOUND")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        if (!request.getJobTitle().isEmpty()) {
            job.setJobTitle(request.getJobTitle());
        }
        if (!request.getDescription().isEmpty()) {
            job.setDescription(request.getDescription());
        }
        if (!request.getStatus().isEmpty()) {
            job.setStatus(request.getStatus());
        }
        Job saved = repository.save(job);
        responseObserver.onNext(toResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteJob(JobRequest request, StreamObserver<JobResponse> responseObserver) {
        JobResponse response = repository.findById(request.getJobId())
                .map(job -> {
                    repository.delete(job);
                    return JobResponse.newBuilder()
                            .setJobId(request.getJobId())
                            .setStatus("DELETED")
                            .build();
                })
                .orElse(JobResponse.newBuilder()
                        .setJobId(request.getJobId())
                        .setStatus("NOT_FOUND")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<ListJobsResponse> responseObserver) {
        List<JobResponse> list = repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        responseObserver.onNext(ListJobsResponse.newBuilder().addAllJobs(list).build());
        responseObserver.onCompleted();
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.newBuilder()
                .setJobId(job.getId())
                .setJobTitle(job.getJobTitle())
                .setStatus(job.getStatus())
                .build();
    }
}

