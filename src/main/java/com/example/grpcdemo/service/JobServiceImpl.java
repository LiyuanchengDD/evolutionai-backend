package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.JobRequest;
import com.example.grpcdemo.proto.JobResponse;
import com.example.grpcdemo.proto.CreateJobRequest;
import com.example.grpcdemo.proto.ListJobsRequest;
import com.example.grpcdemo.proto.ListJobsResponse;
import com.example.grpcdemo.proto.JobServiceGrpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GrpcService  // 让 Spring Boot 自动注册这个 gRPC 服务
public class JobServiceImpl extends JobServiceGrpc.JobServiceImplBase {

    // 内存中的岗位数据（模拟数据库）
    private final List<JobResponse> jobStore = new ArrayList<>();

    @Override
    public void getJob(JobRequest request, StreamObserver<JobResponse> responseObserver) {
        // 在内存里查找岗位
        JobResponse job = jobStore.stream()
                .filter(j -> j.getJobId().equals(request.getJobId()))
                .findFirst()
                .orElse(JobResponse.newBuilder()
                        .setJobId(request.getJobId())
                        .setJobTitle("Not Found")
                        .setStatus("Unknown")
                        .build());

        responseObserver.onNext(job);
        responseObserver.onCompleted();
    }

    @Override
    public void createJob(CreateJobRequest request, StreamObserver<JobResponse> responseObserver) {
        // 生成随机 jobId
        String jobId = UUID.randomUUID().toString();

        JobResponse newJob = JobResponse.newBuilder()
                .setJobId(jobId)
                .setJobTitle(request.getJobTitle())
                .setStatus("Created")
                .build();

        // 保存到内存
        jobStore.add(newJob);

        responseObserver.onNext(newJob);
        responseObserver.onCompleted();
    }

    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<ListJobsResponse> responseObserver) {
        ListJobsResponse response = ListJobsResponse.newBuilder()
                .addAllJobs(jobStore)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
