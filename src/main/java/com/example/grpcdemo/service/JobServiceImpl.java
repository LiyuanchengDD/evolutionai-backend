package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CreateJobRequest;
import com.example.grpcdemo.proto.JobRequest;
import com.example.grpcdemo.proto.JobResponse;
import com.example.grpcdemo.proto.JobServiceGrpc;
import com.example.grpcdemo.proto.ListJobsRequest;
import com.example.grpcdemo.proto.ListJobsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class JobServiceImpl extends JobServiceGrpc.JobServiceImplBase {

    private final Map<String, JobResponse> jobs = new ConcurrentHashMap<>();

    @Override
    public void getJob(JobRequest request, StreamObserver<JobResponse> responseObserver) {
        JobResponse response = jobs.getOrDefault(
                request.getJobId(),
                JobResponse.newBuilder()
                        .setJobId(request.getJobId())
                        .setStatus("NOT_FOUND")
                        .build()
        );
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createJob(CreateJobRequest request, StreamObserver<JobResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        JobResponse response = JobResponse.newBuilder()
                .setJobId(id)
                .setJobTitle(request.getJobTitle())
                .setStatus("CREATED")
                .build();
        jobs.put(id, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<ListJobsResponse> responseObserver) {
        ListJobsResponse response = ListJobsResponse.newBuilder()
                .addAllJobs(new ArrayList<>(jobs.values()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

