package com.example.grpcdemo.service;

import com.example.grpcdemo.model.Candidate;
import com.example.grpcdemo.proto.CandidateRequest;
import com.example.grpcdemo.proto.CandidateResponse;
import com.example.grpcdemo.proto.CandidateServiceGrpc;
import com.example.grpcdemo.proto.CreateCandidateRequest;
import com.example.grpcdemo.proto.ListCandidatesRequest;
import com.example.grpcdemo.proto.ListCandidatesResponse;
import com.example.grpcdemo.repository.CandidateRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for candidates backed by JPA.
 */
@GrpcService
public class CandidateServiceImpl extends CandidateServiceGrpc.CandidateServiceImplBase {

    private final CandidateRepository repository;

    public CandidateServiceImpl(CandidateRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createCandidate(CreateCandidateRequest request, StreamObserver<CandidateResponse> responseObserver) {
        Candidate candidate = new Candidate();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setStatus("CREATED");
        Candidate saved = repository.save(candidate);

        CandidateResponse response = toResponse(saved);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCandidate(CandidateRequest request, StreamObserver<CandidateResponse> responseObserver) {
        CandidateResponse response = repository.findById(request.getCandidateId())
                .map(this::toResponse)
                .orElse(CandidateResponse.newBuilder()
                        .setCandidateId(request.getCandidateId())
                        .setStatus("NOT_FOUND")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listCandidates(ListCandidatesRequest request, StreamObserver<ListCandidatesResponse> responseObserver) {
        List<CandidateResponse> list = repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        responseObserver.onNext(ListCandidatesResponse.newBuilder().addAllCandidates(list).build());
        responseObserver.onCompleted();
    }

    private CandidateResponse toResponse(Candidate candidate) {
        return CandidateResponse.newBuilder()
                .setCandidateId(candidate.getId())
                .setName(candidate.getName())
                .setEmail(candidate.getEmail())
                .setPhone(candidate.getPhone())
                .setStatus(candidate.getStatus())
                .build();
    }
}

