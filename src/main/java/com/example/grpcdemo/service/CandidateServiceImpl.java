package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CandidateRequest;
import com.example.grpcdemo.proto.CandidateResponse;
import com.example.grpcdemo.proto.CandidateServiceGrpc;
import com.example.grpcdemo.proto.CreateCandidateRequest;
import com.example.grpcdemo.proto.ListCandidatesRequest;
import com.example.grpcdemo.proto.ListCandidatesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class CandidateServiceImpl extends CandidateServiceGrpc.CandidateServiceImplBase {

    private final Map<String, CandidateResponse> candidates = new ConcurrentHashMap<>();

    @Override
    public void createCandidate(CreateCandidateRequest request, StreamObserver<CandidateResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        CandidateResponse response = CandidateResponse.newBuilder()
                .setCandidateId(id)
                .setName(request.getName())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setStatus("CREATED")
                .build();
        candidates.put(id, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCandidate(CandidateRequest request, StreamObserver<CandidateResponse> responseObserver) {
        CandidateResponse found = candidates.getOrDefault(
                request.getCandidateId(),
                CandidateResponse.newBuilder()
                        .setCandidateId(request.getCandidateId())
                        .setStatus("NOT_FOUND")
                        .build()
        );
        responseObserver.onNext(found);
        responseObserver.onCompleted();
    }

    @Override
    public void listCandidates(ListCandidatesRequest request, StreamObserver<ListCandidatesResponse> responseObserver) {
        ListCandidatesResponse response = ListCandidatesResponse.newBuilder()
                .addAllCandidates(new ArrayList<>(candidates.values()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

