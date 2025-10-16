# 前端对接指南

本指南整理了 Vue 客户端与 EvolutionAI 后端交互时需要遵循的接口约定，
涵盖控制器、服务层的最新行为，方便前端在联调或构建 Mock 时保持与正式环境一致。

## 1. 认证、授权与运行环境

### 1.1 API 默认安全策略
应用配置为 OAuth 2.0 资源服务器。除 `/public/**`、`/health` 与开发用 OTP 辅助接口外，
所有路由都要求携带 Bearer Token，并以无状态方式处理；`/auth/me` 是唯一不经过企业试用过滤器的受保护接口，
登录成功后应立即调用以初始化前端状态。【F:src/main/java/com/example/grpcdemo/config/SecurityConfig.java†L55-L78】

### 1.2 运行时配置
`src/main/resources/application.yml` 列出了 Supabase 项目参数、数据库连接、AI 服务地址以及认证和 14 天试用网关的特性开关。
保持 Vue 端 `.env.*` 文件中的变量名称与之对应，即可通过切换环境变量完成部署。【F:src/main/resources/application.yml†L1-L70】

### 1.3 认证模式
默认的 `APP_AUTH_MODE=supabase` 会校验 JWT 的签发方是否等于 `${SUPABASE_PROJECT_URL}/auth/v1`；
`APP_AUTH_MODE=dev-otp` 会启用本地 HMAC JWT 兜底，但当 `APP_ENV=prod` 时会被禁止以防止在生产环境暴露该后门。【F:src/main/java/com/example/grpcdemo/security/AppAuthProperties.java†L9-L137】

### 1.4 试用配置防护
固定验证码模式（`APP_TRIAL_MODE=dev-fixed`）同样无法在生产环境启用，发布前务必切回真实的邀请码流程。【F:src/main/java/com/example/grpcdemo/config/AppTrialProperties.java†L7-L74】

### 1.5 开发 OTP 辅助接口
在 `dev-otp` 模式下，后端暴露 `/dev/auth/send-otp` 与 `/dev/auth/verify-otp`：前者返回与邮箱关联的固定验证码，
后者交换为短期可复用的 Bearer Token，便于本地联调。【F:src/main/java/com/example/grpcdemo/controller/DevAuthController.java†L18-L55】

### 1.6 `/auth/me`
Supabase 或开发 Token 登录成功后调用 `/auth/me` 可获取当前用户 ID、角色与试用期快照。
当过滤器尚未写入试用状态时，控制器会回退到依据邮箱查询试用资格。若需要区分“个人 / 企业”入口，
前端需在拿到 access token 后调用 `PUT /auth/kind` 将 `kind` 设置为 `individual` 或 `enterprise`，接口会回传最新的 `/auth/me`
结构，便于前端一次性刷新状态。【F:src/main/java/com/example/grpcdemo/controller/AuthMeController.java†L39-L103】

### 1.7 试用期访问过滤器
所有 `/api/enterprise/**` 路由都会经过 `TrialAccessFilter`：过滤器会确认认证用户绑定了企业资料并拥有企业 ID，
若未找到有效且未过期的邀请码则直接返回错误码（`TRIAL_INVITE_NOT_SENT`、`TRIAL_INVITE_EXPIRED`、`NOT_ENTERPRISE_USER` 等），
以便前端统一提示。【F:src/main/java/com/example/grpcdemo/security/trial/TrialAccessFilter.java†L27-L120】【F:src/main/java/com/example/grpcdemo/security/trial/TrialErrorCode.java†L5-L26】

## 2. 邀请制试用流程

* **企业自助申请** —— `/trial/apply` 会保存公司名称、联系人邮箱及申请原因，使用认证用户 ID 作为申请人；
  `/trial/verify` 仅允许完成企业资料的所有者校验邀请码。【F:src/main/java/com/example/grpcdemo/controller/TrialController.java†L26-L82】
* **运营后台** —— `/admin/trial/applications` 列出待处理/已审批申请，`/admin/trial/review/{id}` 记录审核结论，
  `/admin/trial/send` 在确认企业 ID 后发送或重发邀请码。所有接口均要求管理员身份并带有额外校验（例如仅允许对已审批申请发送邮件）。【F:src/main/java/com/example/grpcdemo/controller/TrialAdminController.java†L31-L132】

## 3. 企业入驻引导

首次登录企业端时，引导流程会把草稿保存在 `enterprise_onboarding_sessions` 表中，直到验证码校验通过才会落入正式表。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L145-L303】

1. **状态轮询** —— `GET /api/enterprise/onboarding/state` 会返回最新草稿或已完成的公司/联系人/模板，并根据 `Accept-Language`
   本地化可用占位符。所有步骤都会比对请求体中的 `userId` 与认证用户，防止跨账号提交。【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L24-L86】
2. **步骤一** —— `POST /step1` 校验 ISO 国家/城市编码、匹配地点字典，并保存去重后的岗位列表（最多 50 条）。
   本地化的国家/城市列表可通过 `/locations/countries` 与 `/locations/cities` 获取，并遵循 `Accept-Language`。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L145-L189】【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L305-L318】
3. **步骤二** —— `POST /step2` 要求步骤一已完成，保存主要 HR 联系人草稿并推进到步骤三。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L192-L214】
4. **步骤三** —— `POST /step3` 存储邀约模板、解析正文/主题中的占位符并跳转到验证码页面。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L217-L240】
5. **最终校验** —— `POST /verify` 会消耗一次性验证码，把企业资料、主联系人、默认模板及岗位列表写入正式表，
   返回时附带生成的 `companyId` 并清理临时会话。【F:src/main/java/com/example/grpcdemo/service/EnterpriseOnboardingService.java†L243-L303】

## 4. 入驻后的企业资料维护

所有资料接口位于 `/api/enterprise/profile`，在进入服务层前都会校验 `userId` 与当前登录用户一致。【F:src/main/java/com/example/grpcdemo/controller/CompanyProfileController.java†L42-L87】

* `PUT /company` —— 更新企业信息并重建岗位列表，响应会根据来访者语言重新渲染。【F:src/main/java/com/example/grpcdemo/service/CompanyProfileService.java†L91-L114】
* `POST /hr` —— 新增 HR 联系人，同时创建 `user_accounts` 登录记录；如未提供密码则自动生成强密码并仅在响应中返回一次。【F:src/main/java/com/example/grpcdemo/service/CompanyProfileService.java†L116-L158】
* `PUT /hr/{contactId}` —— 更新联系人资料、密码及主联系人标记，保持登录账号同步。【F:src/main/java/com/example/grpcdemo/service/CompanyProfileService.java†L160-L218】
* `GET /calling-codes` / `GET /password/suggestion` —— 返回国际电话区号列表与随机 12 位强密码，供前端下拉和“生成密码”按钮使用。【F:src/main/java/com/example/grpcdemo/controller/CompanyProfileController.java†L73-L81】【F:src/main/java/com/example/grpcdemo/service/CompanyProfileService.java†L50-L89】

## 5. 岗位管理与候选人导入

### 5.1 岗位列表
* `GET /api/enterprise/jobs/summary` 与 `GET /api/enterprise/jobs` 共用解析逻辑，可通过 `companyId` 或 `userId`
  锁定企业并返回岗位总数及是否需要引导提示。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L39-L50】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L63-L84】
* `GET /api/enterprise/jobs/{positionId}` 返回最新岗位卡片及 JD 文档，若岗位不存在会返回 404。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L52-L55】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L86-L92】
* `POST /api/enterprise/jobs/import` 接收 PDF，上传 Supabase Storage，调用配置的 JD 解析器并创建草稿卡片。
  解析失败会把状态标记为 `PARSE_FAILED` 但仍保留文档供手动编辑。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L57-L71】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L94-L176】
  默认实现会请求外部 `/jobs:parse`，`test` Profile 会改用本地 Stub 构造结果，方便无依赖自测。【F:src/main/java/com/example/grpcdemo/service/RestJobDescriptionParser.java†L15-L65】【F:src/main/java/com/example/grpcdemo/service/StubJobDescriptionParser.java†L12-L23】
* `PATCH /api/enterprise/jobs/{positionId}` 支持更新岗位名称、地点、发布人或状态。空名称会被拒绝，若未产生变更则直接返回当前快照。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobController.java†L74-L78】【F:src/main/java/com/example/grpcdemo/service/CompanyJobService.java†L178-L227】

### 5.2 候选人导入与后台操作
* `POST /api/enterprise/jobs/{positionId}/candidates/import` 支持批量上传简历，接入 AI 解析并写入存储，响应包含导入成功的候选人及最新状态统计。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobCandidateController.java†L38-L44】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L229-L233】
* `GET /api/enterprise/jobs/{positionId}/candidates` 提供关键词搜索、状态筛选与分页（每页最多 200 条），始终返回汇总计数以同步标签角标。【F:src/main/java/com/example/grpcdemo/controller/CompanyJobCandidateController.java†L46-L58】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L235-L270】
* `/api/enterprise/job-candidates` 下的详情接口涵盖简历预览/下载、可编辑字段（姓名/邮箱/电话/状态/简历 HTML）、发送邀约（可带 CC）、面试记录 CRUD、音频下载与 AI 评估存储。缺少必要数据时会返回明确的 HTTP 错误，方便前端提示用户补充。【F:src/main/java/com/example/grpcdemo/controller/JobCandidateController.java†L33-L141】【F:src/main/java/com/example/grpcdemo/service/CompanyJobCandidateService.java†L272-L609】

## 6. 候选人面试门户

候选人端依赖 `/api/candidate/interview-sessions/**`：控制器覆盖邀约列表、设备预检、进入 AI 面试、上传语音答案、暂停/恢复计时、完成或放弃面试以及头像处理；
头像与音频下载会设置行内 Content-Disposition 以便 `<audio>` 直接播放。【F:src/main/java/com/example/grpcdemo/controller/CandidateInterviewController.java†L37-L151】
服务层提供：

* 本地化企业/联系人信息与状态统计的邀约聚合。【F:src/main/java/com/example/grpcdemo/service/CandidateInterviewPortalService.java†L147-L190】
* 邀约详情快照：计算是否已准备就绪（预检通过且已上传头像），并在返回前刷新面试时间窗口。【F:src/main/java/com/example/grpcdemo/service/CandidateInterviewPortalService.java†L192-L227】
* 设备预检持久化：满足要求后自动将候选人状态切换为 `SCHEDULED`。【F:src/main/java/com/example/grpcdemo/service/CandidateInterviewPortalService.java†L229-L262】
* 开始/暂停/作答/完成链路：严格校验状态流转、处理超时、将音频保存到 Supabase，并维护倒计时与转写元数据。【F:src/main/java/com/example/grpcdemo/service/CandidateInterviewPortalService.java†L263-L520】
* 头像上传/删除及音频、头像的二进制下载，确保候选人可完整管理所有素材。【F:src/main/java/com/example/grpcdemo/service/CandidateInterviewPortalService.java†L559-L734】

## 7. AI 辅助接口

`/api/ai/questions_generator` 会基于下载的简历与 JD 生成面试题并落库备查；`/api/ai/extract` 负责轻量级字段抽取（简历联系方式或 JD 地点/标题），并保留原始文本以便排错。
传入非法提取类型会返回 400。【F:src/main/java/com/example/grpcdemo/controller/AiAssistantController.java†L18-L41】【F:src/main/java/com/example/grpcdemo/service/AiAssistantService.java†L49-L200】

## 8. gRPC 服务

仓库仍保留后台使用的 protobuf 契约。`candidate.proto` 定义了 `CreateCandidate`、`GetCandidate`、`UpdateCandidate`、`DeleteCandidate`、`ListCandidates` 等 RPC，
所有资源均以 UUID 作为主键，前端在修改时需原样带回。【F:src/main/proto/candidate.proto†L1-L42】
遗留的 `AuthService` 实现展示了当前 access/refresh token 仍为 UUID 占位，REST 客户端需视其为不透明字符串。【F:src/main/java/com/example/grpcdemo/service/AuthServiceImpl.java†L32-L131】
在生成 gRPC-Web Stub 或搭建 Mock 时，可直接共享这些 proto。

## 9. 数据库参考

`db/schema.sql` 罗列了所有 JPA 实体使用的表结构（用户账号、试用申请/邀请、企业资料、联系人、岗位、入驻会话等），
可作为初始化 Supabase 或编写迁移脚本时的权威依据。【F:db/schema.sql†L1-L160】
