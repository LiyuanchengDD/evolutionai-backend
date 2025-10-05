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

## 企业资料维护（企业端）

企业端资料页分左右两部分：左侧展示岗位卡/企业概况，右侧允许编辑企业资料及 HR 列表。以下接口均在完成四步引导后可用，由
`CompanyProfileController` 暴露。所有请求均接受可选 `Accept-Language` 头部（`zh`/`en`/`jp`），用于返回国家、城市与区号的本地化展示。【F:src/main/java/com/example/grpcdemo/controller/CompanyProfileController.java†L18-L74】【F:src/main/java/com/example/grpcdemo/service/CompanyProfileService.java†L37-L364】

### 1. 查询企业资料
- **Endpoint**：`GET /api/enterprise/profile?userId={ownerUserId}`
- **Response** (`CompanyProfileResponse`):
  - `companyId` —— 企业 ID。
  - `company` —— 与引导阶段相同的企业信息字段（含 `companyName`、`countryDisplayName`、`recruitingPositions` 等）。
  - `hrContacts` —— HR 列表，字段包含 `contactId`、`contactName`、`contactEmail`、`phoneCountryCode`、`phoneNumber`、`position`、`department`、`primary`、`userAccountId`。

### 2. 更新企业信息
- **Endpoint**：`PUT /api/enterprise/profile/company`
- **Request body** (`CompanyInfoUpdateRequest`):
  - `userId`（必填）—— 企业所有者账号 ID。
  - `companyName`、`companyShortName`、`socialCreditCode`、`country`、`city`、`employeeScale`、`annualHiringPlan`、`industry`、`website`、`description`、`detailedAddress`、`recruitingPositions[]` 等字段与四步引导一致。
- **Response**：更新后的 `CompanyProfileResponse`。
- **说明**：`recruitingPositions` 会整表覆盖（最多 50 条，自动去重、去空）。

### 3. 新增 HR 账号
- **Endpoint**：`POST /api/enterprise/profile/hr`
- **Request body** (`CreateHrRequest`):
  - `userId`（必填）—— 企业所有者账号 ID。
  - `contactName`、`contactEmail`、`phoneCountryCode`、`phoneNumber`（必填）。
  - `position`、`department`、`primary`（可选）。
  - `password`（可选，8~64 位）。若不传则后端自动生成一组强密码。
- **Response** (`HrContactResponse`):
  - `contact` —— 新增后的 HR 资料（含 `userAccountId`）。
  - `password` —— 新账号的明文密码（只返回一次，前端需提示用户妥善保存）。
- **行为说明**：会自动创建 `user_accounts` 登录记录，角色固定为 `company`，状态为 `ACTIVE`。若 `primary=true`，其余联系人会被取消主联系人标记。

### 4. 更新 HR 资料
- **Endpoint**：`PUT /api/enterprise/profile/hr/{contactId}`
- **Request body** (`HrContactUpdateRequest`):
  - `userId`（必填）—— 企业所有者账号 ID。
  - 其余字段与创建时相同，额外支持 `newPassword`（可选）。
- **Response** (`HrContactResponse`):
  - `contact` —— 更新后的联系人信息。
  - `password` —— 若提供 `newPassword` 则原样回传，便于前端提示用户密码已重置；否则为 `null`。
- **行为说明**：若联系人已绑定 `userAccountId`，修改邮箱或密码会同步更新登录账号；若此前未绑定且提供了 `newPassword`，系统会自动创建新账号并绑定。

### 5. 获取国际电话区号
- **Endpoint**：`GET /api/enterprise/profile/calling-codes`
- **Response**：数组元素为 `CallingCodeDto`：`countryCode`（ISO 3166-1）、`countryName`（按 `Accept-Language` 本地化）、`callingCode`（如 `+86`）。
- **实现**：后端基于 libphonenumber 提供的元数据生成完整列表，可直接渲染到前端下拉框中。

### 6. 生成强密码
- **Endpoint**：`GET /api/enterprise/profile/password/suggestion`
- **Response** (`PasswordSuggestionResponse`):
  - `password` —— 长度 12 的随机强密码（至少包含大小写字母、数字、符号各 1 个）。
- **用途**：前端在“生成密码”按钮点击时调用，配合创建/编辑 HR 流程使用。
3. **令牌管理**：`accessToken`、`refreshToken` 当前为占位 UUID，后续可替换为正式 JWT。前端应在 Pinia/Vuex 中妥善保存并在需要时附加到后续请求头。

## 企业岗位导入与编辑

企业端岗位页依赖以下 REST 接口，由 `CompanyJobController` 暴露在 `/api/enterprise/jobs` 路由下，核心业务逻辑封装在 `CompanyJobService` 中。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L15-L79】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L33-L213】

### 1. 判断是否展示引导弹窗
- **Endpoint**：`GET /api/enterprise/jobs/summary`
- **Query 参数**：
  - `companyId`（可选）—— 企业 ID。
  - `userId`（可选）—— 当前登录用户 ID；未传 `companyId` 时用于反查归属企业。
- **Response** (`JobSummaryResponse`):
  - `companyId` —— 解析后的企业 ID。
  - `totalPositions` —— 当前岗位数量。
  - `shouldShowOnboarding` —— 是否需要展示首次引导（岗位数量为 0 时为 `true`）。
- **备注**：`companyId` 与 `userId` 至少提供其一，否则后端返回 400。

### 2. 获取岗位卡片列表
- **Endpoint**：`GET /api/enterprise/jobs`
- **Query 参数**：同上。
- **Response**：数组元素为 `JobCardResponse`，字段包含 `positionId`、`positionName`、`positionLocation`、`publisherNickname`、`status`、`updatedAt`（ISO-8601 字符串）。
- **排序**：按 `updatedAt` 倒序返回，方便前端最新更新的岗位排在顶部。

### 3. 上传岗位 JD（PDF）并触发 AI 解析
- **Endpoint**：`POST /api/enterprise/jobs/import`
- **Content-Type**：`multipart/form-data`
- **表单字段**：
  - `companyId` 或 `userId` —— 与列表接口相同，用于定位企业。
  - `uploaderUserId`（必填）—— 当前上传人 ID，后端会写入解析记录。
  - `file`（必填）—— PDF 文件。
- **Response** (`JobDetailResponse`):
  - `card` —— 最新岗位卡片快照。
  - `source` —— 创建来源（目前固定 `AI_IMPORT`）。
  - `document` —— 解析详情（`JobDocumentResponse`），含原文件名、AI 提取的标题/地点/发布人、置信度、原始 JSON/错误信息，以及 `documentHtml` 字段（内嵌 `<iframe>` 字符串，直接放入页面即可预览上传的 PDF）。
- **失败处理**：
  - 文件为空或读取异常返回 400。
  - AI 解析失败时 `status` 会标记为 `PARSE_FAILED`，`document.aiRawResult` 返回错误信息，前端可提示用户转入手动编辑流程。【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L75-L143】
- **AI 服务**：默认实现使用 `RestJobDescriptionParser` 调用外部 AI（POST `/jobs:parse`），测试环境自动切换为 `StubJobDescriptionParser`，无需真实服务即可自测。【F:src/main/java/com/example/grpcdemo/service/RestJobDescriptionParser.java†L16-L71】【F:src/main/java/com/example/grpcdemo/service/StubJobDescriptionParser.java†L12-L28】

### 4. 查看岗位详情
- **Endpoint**：`GET /api/enterprise/jobs/{positionId}`
- **Response**：`JobDetailResponse`，结构同上，便于在编辑页面回填解析信息。`document.documentHtml` 会返回一个内嵌 `<iframe>` 的 HTML 字符串，可直接插入岗位描述区域展示原始 PDF。

### 5. 编辑岗位卡片
- **Endpoint**：`PATCH /api/enterprise/jobs/{positionId}`
- **Request body** (`JobUpdateRequest`):
  - `positionName`（可选）—— 岗位名称，若提供则不能为空字符串。
  - `positionLocation`（可选）—— 工作地点，允许置空。
  - `publisherNickname`（可选）—— 发布人昵称，允许置空。
  - `status`（可选）—— `RecruitingPositionStatus` 枚举之一（`READY`、`PUBLISHED`、`CLOSED` 等）。
- **Response**：更新后的 `JobDetailResponse`。若仅传空值、不造成任何变化，后端会返回当前数据库快照。
- **行为说明**：字段发生变化时会自动把岗位状态更新为 `READY` 并刷新 `updatedAt`，岗位卡列表可直接使用响应中的 `card` 回填 UI。【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L145-L205】

### 6. 岗位候选人批量导入与管理
- **批量上传**：`POST /api/enterprise/jobs/{positionId}/candidates/import`
  - `multipart/form-data`，参数：`uploaderUserId`（必填）+ `files[]`（必填，支持多份简历）。
  - 后端逐份调用 AI 简历解析服务（生产：`RestResumeParser` → `POST /resumes:parse`，测试：`StubResumeParser` 直接从文件名生成占位数据）。
  - 返回 `JobCandidateImportResponse`，包含导入成功的候选人条目及当前岗位的状态统计。
  - 若未解析出邮箱，`inviteStatus=EMAIL_MISSING`，前端需提示补录；解析失败时 `importedCandidates[].resumeAvailable=true` 但 `summary.emailMissing` 会计数。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobCandidateController.java†L20-L40】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L60-L137】【F:src/main/java/com/example/grpcdemo/service/RestResumeParser.java†L16-L71】【F:src/main/java/com/example/grpcdemo/service/StubResumeParser.java†L13-L29】
- **岗位内检索**：`GET /api/enterprise/jobs/{positionId}/candidates?keyword=xxx`
  - `keyword` 同时匹配姓名/邮箱/电话，若不传则返回按导入时间倒序的全量列表。
  - 响应 `JobCandidateListResponse`，附带 `summary`（邀约状态 + 面试状态聚合），新增 `interviewAbandoned`、`interviewTimedOut` 两个统计字段。【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L139-L218】【F:src/main/java/com/example/grpcdemo/controller/dto/JobCandidateStatusSummary.java†L9-L52】
- **候选人详情**：`GET /api/enterprise/job-candidates/{jobCandidateId}/resume`
  - 返回 `JobCandidateResumeResponse`，包含基础信息、AI 解析出的 HTML 片段以及原始简历的 `<iframe>` HTML，可直接嵌入预览区。新增字段：`interviewRecordAvailable`、`aiEvaluationAvailable`、`interviewCompletedAt`、`interviewDeadlineAt`，用于控制“查看面试记录/AI 报告”按钮显示与倒计时。【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L168-L221】【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L24-L28】【F:src/main/java/com/example/grpcdemo/controller/dto/JobCandidateResumeResponse.java†L16-L78】
- **人工补录/更新**：`PATCH /api/enterprise/job-candidates/{jobCandidateId}`
  - 支持修改姓名、邮箱、电话、邀约状态、面试状态，以及可编辑的 `resumeHtml`。邮箱被清空会自动回退到 `EMAIL_MISSING`，补齐邮箱后默认切换为 `INVITE_PENDING`。【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L204-L236】
- **发送邀约邮件**：`POST /api/enterprise/job-candidates/{jobCandidateId}/invite`
  - 请求体可选 `templateId`（覆盖默认模版）与 `cc[]`。后端读取企业默认邀约模版，使用 `JavaMailSender` 发送成功后把状态更新为 `INVITE_SENT`，并记录 `lastInviteSentAt`/`interviewDeadlineAt=+15d`；发送失败会回写 `INVITE_FAILED` 并返回 500。【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L238-L320】【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L30-L39】
- **候选人列表卡片字段**：`JobCandidateItemResponse` 新增 `interviewRecordAvailable`、`aiEvaluationAvailable`、`interviewCompletedAt`、`interviewDeadlineAt`，前端可根据布尔值控制图标高亮，`interviewDeadlineAt` 可用于计算倒计时或展示“已超时”状态。【F:src/main/java/com/example/grpcdemo/controller/dto/JobCandidateItemResponse.java†L14-L137】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L618-L650】
- **获取面试记录**：`GET /api/enterprise/job-candidates/{jobCandidateId}/interview-record`
  - 返回 `CandidateInterviewRecordResponse`，字段含面试方式、起止时间、题目与问答列表、`transcriptRaw`（AI 原始转写 JSON）等；若不存在记录返回 404。前端点击“已面试”按钮后打开详情页使用该接口即可。【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L41-L45】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L322-L374】【F:src/main/java/com/example/grpcdemo/controller/dto/CandidateInterviewRecordResponse.java†L9-L71】
- **写入/更新面试记录**：`PUT /api/enterprise/job-candidates/{jobCandidateId}/interview-record`
  - 请求体 `CandidateInterviewRecordRequest` 包含题目数组 `questions[]`（每题序号、题干、候选人回答、AI 反馈等）与 `metadata` 扩展字段；供智能体或面试服务回调时使用，保存后会自动更新候选人状态与完成时间。【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L47-L52】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L334-L374】【F:src/main/java/com/example/grpcdemo/controller/dto/CandidateInterviewRecordRequest.java†L9-L77】
- **获取 AI 评估报告**：`GET /api/enterprise/job-candidates/{jobCandidateId}/ai-evaluation`
  - 返回 `CandidateAiEvaluationResponse`，含整体得分、评分等级、`strengths[]`、`weaknesses[]`、`riskAlerts[]`、`competencyScores{}`、`customMetrics{}` 以及原始 `rawPayload`，用于渲染第四张图展示的报告页面。【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L54-L57】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L376-L406】【F:src/main/java/com/example/grpcdemo/controller/dto/CandidateAiEvaluationResponse.java†L9-L90】
- **写入/更新 AI 评估报告**：`PUT /api/enterprise/job-candidates/{jobCandidateId}/ai-evaluation`
  - 请求体 `CandidateAiEvaluationRequest` 保留了分数、优缺点、风险提示、能力项评分、自定义指标、AI 模型版本及原始数据等字段，便于智能体服务直接落库；成功写入后同样会刷新候选人的面试状态与完成时间。【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L59-L64】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L408-L454】【F:src/main/java/com/example/grpcdemo/controller/dto/CandidateAiEvaluationRequest.java†L9-L89】
- **岗位搜索增强**：岗位列表接口新增 `keyword` 参数，可在后端按岗位名称模糊查询，未命中时仍返回原本的倒序列表，供“搜索岗位名称”输入框复用。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L38-L43】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L59-L73】

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
  该对象的完整字段见 `OnboardingStateResponse` 定义。【F:src/main/java/com/example/grpcdemo/controller/dto/OnboardingStateResponse.java†L9-L58】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L372-L424】

- **使用建议**：每次进入任一步骤前先调用一次 `GET /state` 并用返回的 `companyInfo`/`contactInfo`/`templateInfo` 回填表单字段；后端会把草稿同步写入 `enterprise_onboarding_sessions` 表，并在用户重新进入或返回上一步时重新加载。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L156-L339】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L548-L608】【F:src/main/java/com/example/grpcdemo/entity/EnterpriseOnboardingSessionEntity.java†L16-L99】
- **覆盖更新**：同一个 `userId` 重复调用 `POST /step{N}` 会覆盖该步骤之前的草稿并更新快照列表，方便用户修改后再次提交。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L156-L247】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L808-L908】

### 2. 步骤一：企业基础信息
- **Endpoint**：`POST /api/enterprise/onboarding/step1`
- **Request body** (`EnterpriseStep1Request`):
  - `userId`（必填）—— 当前登录用户 ID。
  - `companyName`（必填）—— 企业全称。
  - `companyShortName`、`socialCreditCode`、`industry`、`website`、`description`（选填）。
  - `detailedAddress`（选填）—— 企业详细地址，会回显在 `companyInfo.detailedAddress` 字段。
  - `country`、`city`、`employeeScale`、`annualHiringPlan`（均为必填）—— 国家与城市使用 ISO 代码（`country` 为 ISO 3166-1 alpha-2，`city` 为 ISO 3166-2），后端会校验组合合法性；企业规模枚举见 `EmployeeScale`，年度招聘计划枚举见 `AnnualHiringPlan`。
  - `recruitingPositions`（选填）—— 正在招聘的岗位名称列表，后端会去重、截断到 50 条，并在 `GET /state` 的 `companyInfo.recruitingPositions` 中返回；在最终校验通过时会写入 `company_recruiting_positions` 表。
- **行为说明**：服务端会把本步骤数据写入会话专用的临时 List，并将 `currentStep` 推进到 2。【F:src/main/java/com/example/grpcdemo/controller/dto/EnterpriseStep1Request.java†L14-L109】【F:src/main/java/com/example/grpcdemo/onboarding/EmployeeScale.java†L6-L41】【F:src/main/java/com/example/grpcdemo/onboarding/AnnualHiringPlan.java†L6-L41】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L129-L229】
  - 国家 & 城市下拉列表可通过新增接口获取：`GET /api/enterprise/onboarding/locations/countries` 返回所有国家（响应字段 `code`=`ISO 3166-1 alpha-2`），`GET /api/enterprise/onboarding/locations/cities?country={code}` 返回对应国家的 ISO 3166-2 省/州/直辖市列表。两个接口均支持 `Accept-Language` (`zh`/`en`/`jp`) 自动本地化显示名称。【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L32-L63】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L247-L310】【F:src/main/java/com/example/grpcdemo/location/LocationCatalog.java†L13-L125】

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
     - 企业详细地址会落库到 `company_profiles.detailed_address`，岗位列表会拆分为多条写入 `company_recruiting_positions`。
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

