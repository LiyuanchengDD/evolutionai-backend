# Front-end Integration Guide

This guide summarizes the payloads that front-end clients need to send so that
requests reach the REST authentication controller (AuthController) and the gRPC
services (AuthServiceImpl, CandidateServiceImpl, etc.) backed by Spring Data
JPA.

## Authentication flows（REST 接口）

The authentication endpoints live under `/api/{segment}/auth/**` and are served
by `AuthController`. The `{segment}` path parameter maps the front-end entry to a
backend 角色：

| segment 示例 | 解析后的角色 |
| ------------ | ----------- |
| `b`、`company` | 企业端（`AuthRole.COMPANY`） |
| `c`、`engineer` | 工程师端（`AuthRole.ENGINEER`） |

All requests/响应均为 JSON，所有字段都带有后端校验（邮箱格式、最小长度等）。验证码
默认有效期 5 分钟，60 秒内重复请求会被拒绝。常见错误码参见
`AuthErrorCode`（会通过 HTTP 状态与中文错误信息返回）。【F:src/main/java/com/example/grpcdemo/controller/AuthController.java†L27-L68】【F:src/main/java/com/example/grpcdemo/auth/AuthManager.java†L22-L119】【F:src/main/java/com/example/grpcdemo/auth/AuthErrorCode.java†L7-L32】

### 1. 发送验证码（注册/忘记密码通用）
- **Endpoint**：`POST /api/{segment}/auth/password/reset/send-code`
- **Request body** (`SendCodeRequest`):
  - `email`（必填）——注册邮箱，需符合格式校验。
  - `purpose`（可选，默认 `REGISTER`）——验证码用途，可选值：`REGISTER`、`RESET_PASSWORD`。
- **Response** (`VerificationCodeResponseDto`):
  - `requestId`——验证码请求 ID，便于日志排查。
  - `expiresInSeconds`——验证码有效期秒数（默认 300 秒）。
- **行为说明**：
  - 注册场景会校验邮箱未被占用；被占用时返回 `USER_ALREADY_EXISTS`。
  - 忘记密码场景要求邮箱已存在，否则返回 `USER_NOT_FOUND`。
  - 60 秒内重复请求会返回 `CODE_REQUEST_TOO_FREQUENT`。

示例请求：

```json
{
  "email": "alice@example.com",
  "purpose": "RESET_PASSWORD"
}
```

示例响应：

```json
{
  "requestId": "7b463937-01d6-4f55-b789-7d0d49b9f3fa",
  "expiresInSeconds": 300
}
```

### 2. 注册账号
- **Endpoint**：`POST /api/{segment}/auth/register`
- **Request body** (`RegisterRequest`):
  - `email`（必填）——作为登录账号保存。
  - `password`（必填）——至少 6 位，后端保存为哈希。
  - `verificationCode`（必填）——步骤 1 发送的注册验证码。
- **Response** (`AuthResponseDto`): 包含 `userId`、`email`、`role`、`accessToken`、`refreshToken`。
- **行为说明**：验证码必须未过期且未被使用，验证成功后会立即标记为已使用并创建新用户记录。

示例请求：

```json
{
  "email": "alice@example.com",
  "password": "S3curePwd",
  "verificationCode": "184263"
}
```

示例响应：

```json
{
  "userId": "8f5b8cf1-6c4d-4e87-8f3c-e1b9513bb2db",
  "email": "alice@example.com",
  "role": "COMPANY",
  "accessToken": "<JWT access token>",
  "refreshToken": "<JWT refresh token>"
}
```

### 3. 登录
- **Endpoint**：`POST /api/{segment}/auth/login`
- **Request body** (`LoginRequest`):
  - `email`（必填）——注册时的邮箱。
  - `password`（必填）——明文密码，后端会与哈希比对。
- **Response** (`AuthResponseDto`): 同注册成功响应。
- **失败场景**：邮箱格式错误返回 `INVALID_EMAIL`；账号或密码错误返回 `INVALID_CREDENTIALS`。

示例请求：

```json
{
  "email": "alice@example.com",
  "password": "S3curePwd"
}
```

### 4. 忘记密码 / 重置密码
- **Endpoint**：`POST /api/{segment}/auth/password/reset`
- **Request body** (`ResetPasswordRequest`):
  - `email`（必填）——账号邮箱。
  - `verificationCode`（必填）——步骤 1 中 purpose=`RESET_PASSWORD` 获取的验证码。
  - `newPassword`（必填）——至少 6 位的新密码。
- **Response** (`SuccessResponse`):

```json
{
  "message": "密码重置成功"
}
```

- **流程提醒**：验证码使用一次即作废；超时或错误会返回 `CODE_EXPIRED`、`CODE_MISMATCH` 等错误码，前端需要根据消息提示用户重试或重新发送验证码。

### 前端流程建议
1. **注册**：发送 purpose=`REGISTER` 的验证码 → 输入验证码连同邮箱、密码一起提交注册 → 保存返回的 `userId` 与令牌。
2. **忘记密码**：发送 purpose=`RESET_PASSWORD` 的验证码 → 用户填写验证码与新密码 → 调用重置接口 → 引导回登录页重新登录。
3. **令牌管理**：`accessToken`、`refreshToken` 当前为占位 UUID，后续可替换为正式 JWT。前端应在 Pinia/Vuex 中妥善保存并在需要时附加到后续请求头。

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
- **身份认证使用 REST**：直接通过浏览器 `fetch`/`axios` 调用上述 `/api/{segment}/auth/**`
  接口即可，注意携带 `Content-Type: application/json`。
- **业务数据仍为 gRPC**：候选人、岗位、面试等服务依旧通过 gRPC 单次调用完成；为
  Vue 端生成 gRPC-Web/Connect 客户端时请以 `src/main/proto` 为准。
- **输入校验**：提交前在前端完成基本校验，避免因为空字段或格式错误触发后端 400
  响应。
- **令牌持久化**：后端返回的 `accessToken`、`refreshToken` 需要妥善保存，以便未来的
  认证拦截器启用后复用。
- **ID 记录**：用户 ID、候选人 ID 等均为 UUID 字符串，后续查询/更新/删除时需要原样
  传回。

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

  ```http
  # 发送验证码（注册）
  POST /api/b/auth/password/reset/send-code
  Content-Type: application/json

  {
    "email": "new.user@example.com",
    "purpose": "REGISTER"
  }

  # 注册成功响应
  POST /api/b/auth/register
  Content-Type: application/json

  {
    "userId": "8f5b8cf1-6c4d-4e87-8f3c-e1b9513bb2db",
    "email": "new.user@example.com",
    "role": "COMPANY",
    "accessToken": "<JWT access token>",
    "refreshToken": "<JWT refresh token>"
  }

  # 忘记密码 -> 重置密码
  POST /api/c/auth/password/reset
  Content-Type: application/json

  {
    "message": "密码重置成功"
  }
  ```

  ```json
  // CandidateService.CreateCandidate 创建请求示例（gRPC）
  {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+86-138-0000-0000"
  }

  // CandidateService.ListCandidates 响应片段示例（gRPC）
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

