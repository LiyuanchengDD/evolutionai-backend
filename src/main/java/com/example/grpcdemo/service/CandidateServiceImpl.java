package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CandidateRequest;
import com.example.grpcdemo.proto.CandidateResponse;
import com.example.grpcdemo.proto.CreateCandidateRequest;
import com.example.grpcdemo.proto.ListCandidatesRequest;
import com.example.grpcdemo.proto.ListCandidatesResponse;
import com.example.grpcdemo.proto.CandidateServiceGrpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GrpcService
public class CandidateServiceImpl extends CandidateServiceGrpc.CandidateServiceImplBase {

    private final List<CandidateResponse> candidateStore = new ArrayList<>();

    @Override
    public void createCandidate(CreateCandidateRequest request,
                                StreamObserver<CandidateResponse> responseObserver) {
        String candidateId = UUID.randomUUID().toString();

        CandidateResponse candidate = CandidateResponse.newBuilder()
                .setCandidateId(candidateId)
                .setName(request.getName())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setStatus("Registered")
                .build();

        candidateStore.add(candidate);

        responseObserver.onNext(candidate);
        responseObserver.onCompleted();
    }

    @Override
    public void getCandidate(CandidateRequest request,
                             StreamObserver<CandidateResponse> responseObserver) {
        CandidateResponse candidate = candidateStore.stream()
                .filter(c -> c.getCandidateId().equals(request.getCandidateId()))
                .findFirst()
                .orElse(CandidateResponse.newBuilder()
                        .setCandidateId(request.getCandidateId())
                        .setName("Not Found")
                        .setEmail("-")
                        .setPhone("-")
                        .setStatus("Unknown")
                        .build());

        responseObserver.onNext(candidate);
        responseObserver.onCompleted();
    }

    @Override
    public void listCandidates(ListCandidatesRequest request,
                               StreamObserver<ListCandidatesResponse> responseObserver) {
        ListCandidatesResponse response = ListCandidatesResponse.newBuilder()
                .addAllCandidates(candidateStore)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
