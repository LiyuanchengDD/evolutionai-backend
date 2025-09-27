# Front-end Integration Guide

This guide summarizes the payloads that front-end clients need to send so that
requests reach the gRPC services (AuthServiceImpl, CandidateServiceImpl, etc.)
and get persisted through Spring Data JPA.

## Authentication flows

### Register user
- **RPC**: `AuthService.RegisterUser`
- **Request message** (`RegisterUserRequest`):
  - `username` – required, non-empty string used as the login name.
  - `password` – required plain-text password; the backend hashes it before
    storage.
  - `role` – required role identifier (e.g. `ADMIN`, `RECRUITER`, `CANDIDATE`).
  - `email` – optional; the backend currently stores it as part of the user
    response.
- **Success response** (`UserResponse`): contains generated `user_id`, echoed
  `username`, `role`, and generated `access_token` / `refresh_token` strings.
- **Validation**: the server rejects requests with missing/blank fields or a
  duplicate `(username, role)` pair.

### Login user
- **RPC**: `AuthService.LoginUser`
- **Request message** (`LoginRequest`):
  - `username` – required.
  - `password` – required; matched against the stored password hash.
  - `role` – required; helps distinguish different account personas with the
    same username.
- **Success response**: same `UserResponse` structure as registration.
- **Failure cases**: blank fields or incorrect credentials return
  `INVALID_ARGUMENT` / `UNAUTHENTICATED` errors.

## Candidate management flows

### Create candidate
- **RPC**: `CandidateService.CreateCandidate`
- **Request message** (`CreateCandidateRequest`):
  - `name` – required candidate display name.
  - `email` – required contact email (persisted as plain text).
  - `phone` – required contact number.
- **Server behavior**: backend generates the candidate ID and default status
  `CREATED` before persisting.
- **Success response** (`CandidateResponse`): contains generated `candidateId`
  plus all stored fields.

### Get candidate
- **RPC**: `CandidateService.GetCandidate`
- **Request message** (`CandidateRequest`): `candidateId` returned from the
  create call.
- **Response**: `CandidateResponse`; if not found the status is `NOT_FOUND`.

### Update candidate
- **RPC**: `CandidateService.UpdateCandidate`
- **Request message** (`UpdateCandidateRequest`):
  - `candidateId` – required.
  - Any combination of `name`, `email`, `phone`, `status` – send only the fields
    that need updates; empty strings are ignored and leave the stored values
    unchanged.
- **Response**: updated `CandidateResponse`.

### Delete candidate
- **RPC**: `CandidateService.DeleteCandidate`
- **Request**: `CandidateRequest` with `candidateId`.
- **Response**: `CandidateResponse` with status `DELETED` if the record existed;
  otherwise `NOT_FOUND`.

### List candidates
- **RPC**: `CandidateService.ListCandidates`
- **Request**: empty `ListCandidatesRequest`.
- **Response**: `ListCandidatesResponse` containing repeated
  `CandidateResponse` entries.

## Client integration tips
- All calls are unary gRPC requests. Use the generated gRPC stub in the front
  end (or via an API gateway) to send messages with the above fields.
- Trim or validate user input before sending to avoid blank-field validation
  errors from the backend.
- Persist and reuse the returned `access_token` / `refresh_token` for subsequent
  authenticated calls once authentication middleware is in place.
- Backend IDs (`user_id`, `candidateId`) are UUID strings; store them client-side
  if you need to query/update/delete the same entities later.

## Hand-off package for front-end mocking

When a front-end teammate wants to mock or jointly debug against the backend,
share the following artefacts so they can mirror real payloads without waiting
for the backend runtime:

1. **Protobuf contracts** – zip or link to the files under
   `src/main/proto/` (for example `auth.proto`, `candidate.proto`). These are the
   source of truth for RPC names, request/response fields, enums, and error
   status codes. They can generate TypeScript stubs for gRPC-Web mocking or be
   used to derive JSON payloads for REST/gateway mocks.
2. **Endpoint environment** – document the current test host/port and whether
   the team will call the service through gRPC-Web (`https://dev-api.example.com`
   via Envoy) or a REST proxy (`https://dev-rest.example.com/api`). Provide any
   required headers (e.g. `Authorization: Bearer <token>` once login succeeds).
3. **Example payloads** – give the front end a small catalogue of sample
   requests/responses for the most important flows. These can live in a shared
   Postman/Insomnia collection or be pasted directly into the docs. For example:

  ```json
  // AuthService.RegisterUser 注册请求示例
   {
     "username": "alice",
     "password": "S3curePwd",
     "role": "RECRUITER",
     "email": "alice@example.com"
   }

  // AuthService.LoginUser 响应示例
   {
     "userId": "8f5b8cf1-6c4d-4e87-8f3c-e1b9513bb2db",
     "username": "alice",
     "role": "RECRUITER",
     "accessToken": "<JWT access token>",
     "refreshToken": "<JWT refresh token>"
   }
   ```

  ```json
  // CandidateService.CreateCandidate 创建请求示例
   {
     "name": "John Doe",
     "email": "john.doe@example.com",
     "phone": "+86-138-0000-0000"
   }

  // CandidateService.ListCandidates 响应片段示例
   {
     "candidates": [
       {
         "candidateId": "d3ea6a97-83d6-43e6-88aa-0bd0f023e3b9",
         "name": "John Doe",
         "email": "john.doe@example.com",
         "phone": "+86-138-0000-0000",
         "status": "CREATED"
       }
     ]
   }
   ```

4. **Mocking instructions** – outline how the front end can stand up mocks:
   - *gRPC-Web*: use the generated TypeScript client with a mock transport (e.g.
     `@bufbuild/connect-web`'s `createPromiseClient`) and feed it the sample
     payloads above. This lets them emulate server responses before the backend
     is reachable.
   - *REST gateway*: if the team prefers REST, provide equivalent HTTP method +
     path mappings (e.g. `POST /api/auth/login`) and status codes so they can
     stub out `fetch`/`axios` calls.

5. **Workflow checklist** – describe the handshake for joint debugging:
   1. Confirm the environment variables/endpoints.
   2. Exchange test credentials or seed data if the backend expects existing
      records.
   3. Front-end mocks requests based on the sample payloads.
   4. Switch the mock transport/base URL to the live backend when both sides are
      ready, keeping the same request schema.

With these assets, the front-end engineer can implement realistic mocks, wire
their forms to the expected contract, and flip to the actual backend without
schema changes when joint testing begins.

## Vue front-end considerations

Because browsers cannot open raw HTTP/2 gRPC connections, a Vue application has
to call the backend through one of the following adapters:

- **gRPC-Web**: generate TypeScript stubs with `protoc` + the `grpc-web`
  plugin, deploy an Envoy/Traefik gRPC-Web proxy in front of the Spring Boot
  service, and invoke RPCs with `@improbable-eng/grpc-web` or the official
  `grpc-web` client. This keeps the protobuf contracts end to end.
- **REST/gRPC gateway**: expose REST endpoints (for example with Spring MVC or
  Envoy's JSON transcoding) that internally forward to the gRPC services. The
  Vue code can then rely on `axios`/`fetch` without extra tooling. Ensure the
  REST payloads map 1:1 with the request/response shapes documented above.

When wiring the UI:

- **Form handling** – Validate inputs before making the RPC/REST call (e.g.
  using Vuelidate or VueUse form helpers) to guarantee `username`, `password`,
  candidate fields, etc., are non-empty.
- **State management** – Store authentication tokens and user metadata in a
  central store such as Pinia/Vuex. If you keep tokens in localStorage, protect
  them against XSS; alternatively, configure the backend to set HTTP-only
  cookies.
- **Error feedback** – Surface gRPC status codes or HTTP errors in the UI. For
  example, map `INVALID_ARGUMENT` to inline validation errors and
  `UNAUTHENTICATED` to login failure messages.
- **Environment configuration** – Keep the gateway base URL in environment
  files (`.env.development`, `.env.production`) so different deployments can
  target the right proxy without code changes.
- **Navigation guards** – Use Vue Router guards to check the presence/expiry of
  access tokens before entering authenticated routes, refreshing via the
  `refresh_token` when necessary.

