# Microservice API Definitions

This document outlines the gRPC interfaces for core microservices in the EvolutionAI interview platform.

## Auth Service
- `rpc RegisterUser(RegisterUserRequest) returns (UserResponse)`
- `rpc LoginUser(LoginRequest) returns (LoginResponse)`

## Job Service
- `rpc CreateJob(CreateJobRequest) returns (JobResponse)`
- `rpc GetJob(JobRequest) returns (JobResponse)`
- `rpc ListJobs(ListJobsRequest) returns (ListJobsResponse)`

## Candidate Service
- `rpc CreateCandidate(CreateCandidateRequest) returns (CandidateResponse)`
- `rpc GetCandidate(CandidateRequest) returns (CandidateResponse)`
- `rpc ListCandidates(ListCandidatesRequest) returns (ListCandidatesResponse)`

## Interview Service
- `rpc StartInterview(StartInterviewRequest) returns (InterviewResponse)`
- `rpc SubmitAnswer(SubmitAnswerRequest) returns (InterviewResponse)`
- `rpc GetInterview(InterviewRequest) returns (InterviewResponse)`

## Report Service
- `rpc GenerateReport(GenerateReportRequest) returns (ReportResponse)`
- `rpc GetReport(GetReportRequest) returns (ReportResponse)`

## Notification Service
- `rpc SendInvitation(SendInvitationRequest) returns (SendInvitationResponse)`

Each service maintains its own data store and communicates over gRPC to ensure loose coupling and scalability.
