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

## Enterprise onboarding（企业注册资料引导）

企业端完成邮箱注册并首次登录后，需要在浏览器里按四个步骤补全企业资料。所有接口都由
`EnterpriseOnboardingController` 暴露在 REST 路由 `/api/enterprise/onboarding/**` 下，可直接
用 Axios/`fetch` 访问。【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L18-L55】

### 1. 查询当前进度
- **Endpoint**：`GET /api/enterprise/onboarding/state?userId={userId}`
- **Response** (`OnboardingStateResponse`):
  - `currentStep` —— 当前应该展示的步骤编号（1=企业信息、2=HR 信息、3=邀约模版、4=验证码确认）。
  - `companyInfo`/`contactInfo`/`templateInfo` —— 若前端已提交过，对应字段会返回最近一次保存的草稿。
  - `records` —— 以列表形式返回后台临时 List 中的每一步快照，方便页面在“上一步”时回填。
  - `availableVariables` —— 邀约模版支持的动态变量列表，可用于渲染插入按钮。
  - `completed`+`companyId` —— 当 `completed=true` 时表示已经点击“验证并进入 Evolution AI”并完成入库。
  该对象的完整字段见 `OnboardingStateResponse` 定义。【F:src/main/java/com/example/grpcdemo/controller/dto/OnboardingStateResponse.java†L9-L58】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L129-L165】

### 2. 步骤一：企业基础信息
- **Endpoint**：`POST /api/enterprise/onboarding/step1`
- **Request body** (`EnterpriseStep1Request`):
  - `userId`（必填）—— 当前登录用户 ID。
  - `companyName`（必填）—— 企业全称。
  - `companyShortName`、`socialCreditCode`、`industry`、`website`、`description`（选填）。
  - `country`、`city`、`employeeScale`（必填）—— 所在国家/城市与规模枚举，枚举值见 `EmployeeScale`。
- **行为说明**：服务端会把本步骤数据写入会话专用的临时 List，并将 `currentStep` 推进到 2。【F:src/main/java/com/example/grpcdemo/controller/dto/EnterpriseStep1Request.java†L14-L93】【F:src/main/java/com/example/grpcdemo/onboarding/EmployeeScale.java†L6-L41】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L77-L111】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L402-L446】

### 3. 步骤二：HR 联系人信息
- **Endpoint**：`POST /api/enterprise/onboarding/step2`
- **Request body** (`EnterpriseStep2Request`): `userId`、`contactName`、`contactEmail`、`phoneCountryCode`、`phoneNumber` 必填，`position`、`department` 选填。
- **行为说明**：服务端要求步骤一先完成，否则返回 `MISSING_PREVIOUS_STEP`。保存成功后会覆盖临时 List 中步骤二的快照并推进到步骤 3。【F:src/main/java/com/example/grpcdemo/controller/dto/EnterpriseStep2Request.java†L14-L93】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L113-L146】【F:src/main/java/com/example/grpcdemo/onboarding/OnboardingErrorCode.java†L12-L23】

### 4. 步骤三：面试邀约模版
- **Endpoint**：`POST /api/enterprise/onboarding/step3`
- **Request body** (`EnterpriseStep3Request`): `userId`、`templateName`、`subject`、`body` 必填，`language` 默认 `zh`，与前端的 `Accept-Language` 请求头保持一致（支持 `zh`/`en`/`jp`，若字段为空则按请求头或中文兜底）。
- **行为说明**：后台会解析正文/主题中的 `[[...]]` 变量，并根据当前语言校验允许的占位符。支持的变量如下表，发送其他占位符会返回 `INVALID_TEMPLATE_VARIABLE`：

  | 语言 (`Accept-Language`) | 允许的占位符 |
  | ----------------------- | ------------ |
  | `zh` | `[[候选人姓名]]`、`[[岗位名称]]`、`[[企业全称]]`、`[[面试时间]]`、`[[面试地点]]`、`[[面试链接]]`、`[[联系人姓名]]`、`[[联系人电话]]`、`[[联系人邮箱]]` |
  | `en` | `[[Candidate Name]]`、`[[Job Title]]`、`[[Company Name]]`、`[[Interview Time]]`、`[[Interview Location]]`、`[[Interview Link]]`、`[[Contact Name]]`、`[[Contact Phone]]`、`[[Contact Email]]` |
  | `jp` | `[[候補者名]]`、`[[職位名]]`、`[[企業名]]`、`[[面接時間]]`、`[[面接場所]]`、`[[面接リンク]]`、`[[担当者名]]`、`[[担当者電話]]`、`[[担当者メール]]` |

  后端在所有 `OnboardingStateResponse` 响应中会根据 `Accept-Language` 请求头返回匹配语言的 `availableVariables` 列表，便于前端渲染按钮。保存成功后 `currentStep`=4，供验证码页面使用。【F:src/main/java/com/example/grpcdemo/controller/dto/EnterpriseStep3Request.java†L14-L67】【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L27-L55】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L62-L339】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L362-L608】

### 5. 最终校验并入库
- **Endpoint**：`POST /api/enterprise/onboarding/verify`
- **Request body** (`EnterpriseVerifyRequest`): 包含 `userId` 与 4–8 位的 `verificationCode`；可选 `email` 用于与验证码记录做二次比对。
- **行为说明**：
  1. 按 `userId`、`purpose=ENTERPRISE_ONBOARDING` 查找未消费且未过期的验证码记录；过期或不存在会返回 `INVALID_VERIFICATION_CODE`/`VERIFICATION_CODE_EXPIRED`。
  2. 校验通过后将验证码标记为已使用，并把三个步骤的草稿一次性写入正式表：`company_profiles`、`company_contacts`、`invitation_templates`。
  3. 返回的 `OnboardingStateResponse` 会带上 `completed=true` 和新生成的 `companyId`，临时 List 同时清空。【F:src/main/java/com/example/grpcdemo/controller/dto/EnterpriseVerifyRequest.java†L14-L55】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L187-L267】【F:src/main/java/com/example/grpcdemo/entity/CompanyProfileEntity.java†L16-L120】【F:src/main/java/com/example/grpcdemo/entity/CompanyContactEntity.java†L15-L113】【F:src/main/java/com/example/grpcdemo/entity/InvitationTemplateEntity.java†L15-L113】【F:src/main/java/com/example/grpcdemo/entity/VerificationTokenEntity.java†L15-L117】

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

