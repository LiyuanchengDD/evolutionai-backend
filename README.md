## REST→gRPC 网关部署

该项目同时提供一个将 HTTP REST 请求转发到内部 gRPC 服务的最小可用网关：

1. **编译与运行**
   ```bash
   ./mvnw -DskipTests package
   java -jar target/grpc-demo-1.0.0.jar
   ```
   默认会监听 `0.0.0.0:8080` 并透传到本机 `127.0.0.1:9091` 的 gRPC 服务，可通过
   `SERVER_ADDRESS`、`SERVER_PORT`、`GRPC_CLIENT_USERSVC_ADDRESS` 等环境变量覆盖。

2. **systemd 部署**
   - 将 `deploy/rest-gateway.service` 拷贝到 `/etc/systemd/system/`。
   - 创建 `/etc/evolutionai/rest-gateway.env` 写入需要的覆盖变量，例如：
     ```ini
     SERVER_PORT=8080
     GRPC_CLIENT_USERSVC_ADDRESS=static://127.0.0.1:9091
     GRPC_CLIENT_USERSVC_NEGOTIATIONTYPE=PLAINTEXT
     APP_CORS_ALLOWED_ORIGINS=https://frontend.example.com
     ```
   - 准备运行目录 `/opt/evolutionai/rest-gateway` 并放置构建出的 `rest-gateway.jar`（可对
     `target/grpc-demo-1.0.0.jar` 进行重命名）。
   - 执行 `sudo systemctl daemon-reload && sudo systemctl enable --now rest-gateway` 启动。

3. **一键自测脚本**
   - 确保机器安装了 `curl`、`grpcurl` 与 `python3`。
   - 运行 `scripts/healthcheck.sh`，脚本会同时验证 REST `/api/health` 与 gRPC `grpc.health.v1.Health/Check`
     并输出详细日志，全部成功后会打印 `PASS`。

## 登录与鉴权

项目已切换为 **Supabase + Spring Security OAuth2 Resource Server** 模式，后端仅负责
校验前端携带的 JWT：

* `APP_AUTH_MODE=supabase`（正式 / 预发）：只接受 Supabase 签发的访问令牌，`iss` 必须为
  `${SUPABASE_PROJECT_URL}/auth/v1`。
* `APP_AUTH_MODE=dev-otp`（本地 / 联调占位）：优先校验 Supabase JWT，失败后允许使用
  `/dev/auth/send-otp`、`/dev/auth/verify-otp` 生成的开发 token。`APP_ENV=prod` 时禁止启用。

开发模式下可通过 `DEV_OTP_CODE`、`DEV_JWT_SECRET` 等环境变量调整验证码与 token 配置，详见
`src/main/resources/application.yml` 与 `docs/frontend-integration.md`。

公共用户信息接口：`GET /auth/me` 会返回 `userId`、`email`、`dev` 标记以及试用期状态
(`active`/`not_sent`/`expired`)。【F:src/main/java/com/example/grpcdemo/controller/AuthMeController.java†L7-L37】

试用期仍依赖 `trial_invitations` 表，拦截逻辑迁移至 `TrialAccessFilter`，对所有业务接口
生效（白名单：`/public/**`、`/health`、`/auth/me`、`/dev/auth/**`）。【F:src/main/java/com/example/grpcdemo/security/trial/TrialAccessFilter.java†L18-L78】

## 数据库概览

    `user_accounts` 表
  | 字段                          | 类型                                | 说明        |
  | `user_id`                   | UUID PRIMARY KEY                 | 主键        |
  | `email`                     | CITEXT UNIQUE                     | 登录邮箱（忽略大小写） |
  | `password_hash`             | TEXT                              | BCrypt 哈希 |
  | `role`                      | VARCHAR(32)                       | 角色（company/engineer/admin，对应 `AuthRole.alias()`）|
  | `status`                    | VARCHAR(16)                       | 状态（`ACTIVE`/`PENDING`/`LOCKED`）|
  | `created_at` / `updated_at` | TIMESTAMPTZ                       | 创建/更新时间（由触发器维护）|
  | `last_login_at`             | TIMESTAMPTZ                       | 最近登录      |

    验证码表 `auth_verification_codes`（已废弃）
  | 字段             | 类型            | 说明 |
  | `code_id`       | UUID PRIMARY KEY | **已移除**，迁移至 Supabase Auth OTP 流程 |

# 二,企业端注册四步引导功能
  ## 1.功能概述
    适用角色：已完成邮箱注册并首次登录的企业用户（B 端）
  ## 2.API设计
    | 方法     | URI                                 | 描述                  |
    | `GET`  | `/api/enterprise/onboarding/state`  | 查询当前进度、已保存数据与可用变量列表 |
    | `POST` | `/api/enterprise/onboarding/step1`  | 保存企业基础信息            |
    | `POST` | `/api/enterprise/onboarding/step2`  | 保存 HR 联系人信息         |
    | `POST` | `/api/enterprise/onboarding/step3`  | 保存邀约邮件模板            |
    | `POST` | `/api/enterprise/onboarding/verify` | 校验邮箱验证码并完成引导        |
    | `GET`  | `/api/enterprise/onboarding/locations/countries` | 获取国家下拉列表（ISO 3166-1 alpha-2） |
    | `GET`  | `/api/enterprise/onboarding/locations/cities` | 获取指定国家的 ISO 3166-2 城市/省份 |

    - `EmployeeScale` 六档：50 人以下 / 50-200 人 / 200-500 人 / 500-1000 人 / 1000-5000 人 / 5000 人以上。
    - `AnnualHiringPlan` 六档：1-10 人 / 10-50 人 / 50-100 人 / 100-200 人 / 200-300 人 / 300 人以上。
  ## 3.数据库设计
    company_profiles 表（企业主体信息）
      | 字段               | 类型            | 说明 |
      | `company_id`       | CHAR(36) (PK)   | 企业 ID（UUID） |
      | `owner_user_id`    | CHAR(36)        | 注册账号的用户 ID，用于去重 |
      | `company_name`     | VARCHAR(255)    | 企业全称 |
      | `company_short_name` | VARCHAR(255)  | 企业简称 |
      | `social_credit_code` | VARCHAR(64)   | 统一社会信用代码 |
      | `employee_scale`     | ENUM            | 企业规模（`EmployeeScale` 枚举）|
      | `annual_hiring_plan` | ENUM            | 年度招聘计划（`AnnualHiringPlan` 枚举）|
      | `industry`           | VARCHAR(255)    | 行业 |
      | `country_code`       | VARCHAR(8)      | 国家代码（ISO 3166-1 alpha-2）|
      | `city_code`          | VARCHAR(16)     | 城市/省份代码（ISO 3166-2）|
      | `website`          | VARCHAR(255)    | 官网地址 |
      | `description`      | VARCHAR(1000)   | 企业简介 |
      | `detailed_address` | VARCHAR(255)    | 详细地址 |
      | `status`           | ENUM            | `ONBOARDING` / `ACTIVE` |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
    company_contacts 表（企业联系人）
      | 字段                 | 类型          | 说明 |
      | `contact_id`         | CHAR(36) (PK) | 联系人 ID |
      | `company_id`         | CHAR(36)      | 关联企业 ID |
      | `user_account_id`    | CHAR(36)      | 对应登录账号 ID，可空 |
      | `contact_name`       | VARCHAR(128)  | 联系人姓名 |
      | `contact_email`      | VARCHAR(255)  | 联系人邮箱 |
      | `phone_country_code` | VARCHAR(8)    | 区号，例如 `+86` |
      | `phone_number`       | VARCHAR(32)   | 手机号/座机号 |
      | `position`           | VARCHAR(128)  | 职位 |
      | `department`         | VARCHAR(128)  | 部门 |
      | `is_primary`         | BOOLEAN       | 是否为主联系人 |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
    company_recruiting_positions 表（企业在招岗位）
      | 字段                 | 类型           | 说明 |
      | `position_id`       | CHAR(36) (PK)  | 岗位记录 ID |
      | `company_id`        | CHAR(36)       | 关联企业 ID |
      | `position_name`     | VARCHAR(255)   | 岗位名称（AI 解析或人工编辑）|
      | `position_location` | VARCHAR(255)   | 工作地点，可空 |
      | `publisher_nickname` | VARCHAR(255)  | 发布人昵称，可空 |
      | `position_status`   | ENUM           | `DRAFT_PARSED` / `READY` / `PUBLISHED` / `CLOSED` / `PARSE_FAILED` |
      | `position_source`   | ENUM           | 创建来源：`AI_IMPORT` / `MANUAL` |
      | `document_id`       | CHAR(36)       | 关联 `company_job_documents` 的解析记录，可空 |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
    company_job_documents 表（岗位解析快照）
      | 字段                | 类型           | 说明 |
      | `document_id`      | CHAR(36) (PK)  | 文档记录 ID |
      | `position_id`      | CHAR(36)       | 关联岗位 ID |
      | `file_name`        | VARCHAR(255)   | 上传时的原始文件名 |
      | `file_type`        | VARCHAR(100)   | MIME 类型，例如 `application/pdf` |
      | `storage_bucket`   | VARCHAR(128)   | Supabase Object Storage 桶名 |
      | `storage_path`     | VARCHAR(512)   | Supabase 对象路径，用于下载 |
      | `file_size_bytes`  | BIGINT         | 原始文件大小，单位字节 |
      | `upload_user_id`   | CHAR(36)       | 上传者用户 ID |
      | `ai_raw_result`    | TEXT           | AI 原始返回 JSON 或错误信息 |
      | `parsed_title`     | VARCHAR(255)   | AI 解析出的岗位名称，可空 |
      | `parsed_location`  | VARCHAR(255)   | AI 解析出的地点，可空 |
      | `parsed_publisher` | VARCHAR(255)   | AI 解析出的发布人昵称，可空 |
      | `confidence`       | DECIMAL(5,2)   | AI 置信度，可空 |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
    invitation_templates 表（邀约模版）
      | 字段            | 类型           | 说明 |
      | `template_id`   | CHAR(36) (PK)  | 模版 ID |
      | `company_id`    | CHAR(36)       | 关联企业 ID |
      | `template_name` | VARCHAR(255)   | 模版名称 |
      | `subject`       | VARCHAR(255)   | 邮件主题 |
      | `body`          | VARCHAR(5000)  | 邮件正文（可包含变量占位符）|
      | `language`      | VARCHAR(32)    | 语言标识，默认 `zh`（支持 `zh`/`en`/`jp`） |
      | `is_default`    | BOOLEAN        | 是否为默认模版 |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
    verification_tokens 表（验证码记录）
      | 字段             | 类型           | 说明 |
      | `token_id`       | CHAR(36) (PK)  | 验证码记录 ID |
      | `target_user_id` | CHAR(36)       | 触发验证码的用户 ID |
      | `target_email`   | VARCHAR(255)   | 收码邮箱 |
      | `code`           | VARCHAR(16)    | 验证码内容 |
      | `channel`        | ENUM           | 发送渠道（EMAIL/SMS）|
      | `purpose`        | ENUM           | 验证用途，企业引导为 `ENTERPRISE_ONBOARDING` |
      | `expires_at`     | TIMESTAMP      | 过期时间 |
      | `consumed`       | BOOLEAN        | 是否已使用 |
      | `created_at`/`updated_at` | TIMESTAMP | 审计字段 |
  ## 4.错误处理
    | 场景                 | 返回码 | 错误码                     | 说明 |
    | -------------------- | ------ | ------------------------- | ---- |
    | 参数校验失败         | 400    | `VALIDATION_ERROR`        | 字段未满足 Bean Validation |
    | 未按顺序提交步骤     | 400    | `MISSING_PREVIOUS_STEP`   | 需要先完成上一阶段 |
    | 模版包含未支持变量   | 400    | `INVALID_TEMPLATE_VARIABLE` | `[[...]]` 不在允许列表 |
    | 国家/城市编码无效   | 400    | `INVALID_LOCATION`         | 国家或城市不在支持列表 |
    | 验证码不存在或不匹配 | 400    | `INVALID_VERIFICATION_CODE` | 未找到有效验证码 |
    | 验证码已过期         | 400    | `VERIFICATION_CODE_EXPIRED` | 过期时间早于当前时间 |
    | 已经完成过引导       | 409    | `ONBOARDING_ALREADY_COMPLETED` | 重复提交 |

  # 三,企业资料维护页面
   ## 1.功能概述
     入口：企业端左上角“资料信息”，分为左侧岗位概览与右侧资料编辑区域。
     - 左栏沿用 `company_profiles` 与 `company_recruiting_positions` 数据，展示企业信息及岗位卡列表。
     - 右栏支持修改企业主体信息、维护 HR 列表、新增 HR 账号、生成安全密码、下拉选择国际区号。
     所有接口需要传入企业所有者的 `userId` 用于权限校验，支持 `Accept-Language`（`zh`/`en`/`jp`）返回本地化国家/城市/区号名称。

   ## 2.API 设计
     | 方法 | URI | 描述 |
     | `GET` | `/api/enterprise/profile` | 根据 `userId` 返回企业概览与 HR 列表 |
     | `PUT` | `/api/enterprise/profile/company` | 更新企业主体信息与在招岗位列表 |
     | `POST` | `/api/enterprise/profile/hr` | 新增 HR（自动创建登录账号，可选自定义密码）|
     | `PUT` | `/api/enterprise/profile/hr/{contactId}` | 更新既有 HR 信息，可重置密码 |
     | `GET` | `/api/enterprise/profile/calling-codes` | 返回国家/地区电话区号列表 |
     | `GET` | `/api/enterprise/profile/password/suggestion` | 生成一组强密码供前端展示 |

   ## 3.请求与返回说明
     - 企业信息更新 `CompanyInfoUpdateRequest`：字段与引导步骤 1 保持一致，`recruitingPositions[]` 最多 50 项，后端会自动去重、裁剪空白。
     - HR 创建 `CreateHrRequest`：必填 `contactName`、`contactEmail`、`phoneCountryCode`、`phoneNumber`，`password` 可空（为空时后端生成 12 位强密码并在响应返回一次）。
     - HR 更新 `HrContactUpdateRequest`：除基础字段外新增 `newPassword`，若填写则同步更新/创建登录账号并在响应 `password` 字段透出。
     - `CompanyProfileResponse` 返回企业 ID、企业信息快照（含 `countryDisplayName`、`recruitingPositions`）、`hrContacts[]`（含 `userAccountId`、`primary` 标记）。
     - 区号列表 `CallingCodeDto`：`countryCode`（ISO 3166-1）、`countryName`（按 `Accept-Language` 转换）、`callingCode`（形如 `+81`）。

   ## 4.交互补充
     - 新建/重置密码会自动使用 BCrypt 加密写入 `user_accounts`，角色固定为 `company`，状态 `ACTIVE`。
     - `primary=true` 时会自动取消其他联系人主联系人标记。
     - 区号列表使用 libphonenumber 元数据即时生成，无需额外配置。

   ## 5.数据库设计更新
     `company_contacts` 表新增字段 `user_account_id UUID REFERENCES public.user_accounts(user_id) ON DELETE SET NULL`，并创建索引 `company_contacts_user_account_id_idx`，用于映射可登录的 HR 账号。

  # 四,企业端（岗位引导与卡片管理）
   ## 1.功能概述
      触发条件：企业用户首次登录且岗位列表为空，系统需弹出三步引导。上传 JD 后由 AI 自动抽取“岗位名称 / 工作地点 / 发布人昵称”，若解析异常则允许 HR 手动修正并保存。
   ## 2.API 设计
      所有接口由 `/api/enterprise/jobs` 提供：
      | 方法 | URI | 描述 |
      | --- | --- | --- |
      | `GET` | `/summary` | 根据 `companyId` 或 `userId` 返回岗位总数与是否展示引导弹窗。|
      | `GET` | `/`（根路径） | 返回岗位卡片列表，包含名称、地点、发布人昵称、状态、更新时间，按更新时间倒序。|
      | `POST` | `/import` | `multipart/form-data` 上传 PDF，调用 AI 解析生成岗位草稿并返回详情。|
      | `GET` | `/{positionId}` | 获取单个岗位详情，附带 AI 解析文档快照。|
      | `PATCH` | `/{positionId}` | 更新岗位名称/地点/发布人或状态，保存后同步刷新更新时间。|
      > `companyId` 与 `userId` 至少提供其一；解析失败时返回的状态为 `PARSE_FAILED`，前端需提示进入手动编辑流程。

   ## 3.数据与状态
      - 岗位数据落在 `company_recruiting_positions` 表，新增字段 `position_location`、`publisher_nickname`、`position_status`、`position_source`、`document_id` 与 UI 展示保持一致。
      - 每次导入都会写入一条 `company_job_documents` 记录，保存上传文件元数据、PDF 二进制内容与 AI 原始返回，便于排查解析质量并支撑岗位详情页预览原始文档。
      - 岗位详情接口 `JobDetailResponse.document.documentHtml` 返回 `<iframe>` HTML 字符串，前端可直接插入岗位描述区域展示 HR 上传的 PDF。
      - 岗位状态枚举：
        - `DRAFT_PARSED`：AI 已解析完成，待 HR 校验。
        - `READY`：HR 已确认字段，等待发布。
        - `PUBLISHED`：已正式上线。
        - `CLOSED`：已下线。
        - `PARSE_FAILED`：AI 解析失败，需人工录入。
      - 创建来源 `position_source` 区分 `AI_IMPORT` 与 `MANUAL`，便于后续数据分析。

   ## 4.AI 解析服务
      - 默认通过 `RestJobDescriptionParser` 调用外部 HTTP 服务（`ai.job-parser.base-url`，默认 `http://localhost:8090`），请求体为 Base64 编码的 PDF 内容。
      - 测试环境自动切换为 `StubJobDescriptionParser`，直接从文件名生成占位标题，方便本地联调。
      - 解析成功返回结构化字段写入岗位卡片；若抛出异常，系统仍创建岗位记录但标记为 `PARSE_FAILED` 并在文档表中记录错误信息。
  
  # 五,企业端岗位候选人导入与管理
   ## 1.功能概述
     在企业端岗位详情页支持批量导入候选人简历，自动解析并维护候选人信息、状态、邮件邀约与面试记录。
   ## 2.状态机定义
     ### 邀约状态（`JobCandidateInviteStatus`）
     | 状态代码 | 描述 | 维护服务 |
     | --- | --- | --- |
     | `INVITE_PENDING` | 已解析出邮箱，等待发送邀约。 | `CompanyJobCandidateService.importCandidates` 在导入成功且邮箱有效时设置；清空邮箱或手动改为缺失状态时也会恢复为该值。|
     | `INVITE_SENT` | 已成功发送邀约邮件。 | `CompanyJobCandidateService.sendInvite` 调用邮件服务成功后写入。|
     | `INVITE_FAILED` | 邮件发送失败，需重试。 | `CompanyJobCandidateService.sendInvite` 捕获 `MailException` 时写入。|
     | `EMAIL_MISSING` | 缺少邮箱或邮箱无效，无法发送邀约。 | 导入解析未取到邮箱时由 `importCandidates` 设置；更新候选人信息时如果邮箱被清空，也会改为该状态。|

     ### 面试状态（`JobCandidateInterviewStatus`）
     | 状态代码 | 描述 | 维护服务 |
     | --- | --- | --- |
     | `NOT_INTERVIEWED` | 尚未安排面试。 | 导入候选人时由 `CompanyJobCandidateService.importCandidates` 设置默认值。|
     | `SCHEDULED` | 已安排面试待执行。 | 目前通过企业后台的 `updateCandidate` 接口手动维护；后续与 `InterviewService` 打通后由面试排期流程更新。|
     | `IN_PROGRESS` | 面试进行中。 | 预留给在线面试流程，通过 `updateCandidate` 或未来的实时同步更新。|
    | `COMPLETED` | 面试已完成。 | `CompanyJobCandidateService.upsertInterviewRecord` / `upsertAiEvaluation` 或后台手动更新时自动写入。|
    | `CANCELLED` | 面试取消或企业撤销。 | 由企业后台或面试服务在流程取消时写入。|
    | `ABANDONED` | 候选人主动放弃。 | 候选人端点击“放弃面试”或企业手动更新。|
    | `TIMED_OUT` | 候选人超过 15 天未响应邀约。 | `sendInvite` 会记录 `lastInviteSentAt`，`CompanyJobCandidateService` 读取列表/详情时自动将超时记录标记为该状态。|
  ## 3.API 设计
     1）候选人批量导入
       - 接口：`POST /api/enterprise/jobs/{positionId}/candidates/import`
       - `multipart/form-data`，参数：`uploaderUserId`（必填）+ `files[]`（必填，可多份）。
       - 后端逐份调用 AI 简历解析器，产出姓名/邮箱/电话/HTML 文本；若解析失败依旧建档但标记邮箱缺失。
    2）查询岗位候选人列表
      - 接口：`GET /api/enterprise/jobs/{positionId}/candidates`
      - 查询参数：
        - `keyword` —— 姓名 / 邮箱 / 电话模糊搜索，忽略大小写。
        - `status` —— 固定的五个标签之一：`WAITING_INVITE`（待邀约）、`NOT_INTERVIEWED`（未面试）、`INTERVIEWED`（已面试）、`DROPPED`（已放弃）、`ALL`（全部），缺省等同 `ALL`。
        - `page`、`pageSize` —— 从 0 开始的页码与单页数量（默认 0 / 20，上限 200），支撑前端下拉分页加载。
      - 返回 `JobCandidateListResponse`，`summary` 提供 `waitingInvite`、`notInterviewed`、`interviewed`、`dropped`、`all` 五项计数，并新增 `statusCounts{JobCandidateListStatus→数量}`，方便前端直接渲染顶部 5 个标签；分页字段 `total`、`page`、`pageSize`、`hasMore`、`nextPage` 与此前一致。记录按创建时间倒序返回，保证最新导入的候选人置顶。
     3）更新候选人基础信息
       - 接口：`PATCH /api/enterprise/job-candidates/{jobCandidateId}`
       - 请求体：`JobCandidateUpdateRequest`，支持更新姓名、邮箱、电话、邀约状态、面试状态及可编辑的 `resumeHtml`。
       - 邮箱清空会自动重置 `inviteStatus=EMAIL_MISSING`，补齐邮箱默认回到 `INVITE_PENDING`。
    4）获取候选人简历详情
      - 接口：`GET /api/enterprise/job-candidates/{jobCandidateId}/resume`
      - 返回 `JobCandidateResumeResponse`，包含候选人基础信息、岗位 `positionId`/`positionName`、AI 解析 HTML 片段，以及可直接 `<iframe>` 展示的原始 PDF；同时返回 `interviewRecordAvailable`、`aiEvaluationAvailable`、`interviewCompletedAt`、`interviewDeadlineAt` 等字段供前端控制按钮与倒计时展示。
    5）发送面试邀约邮件
      - 接口：`POST /api/enterprise/job-candidates/{jobCandidateId}/invite`
      - 请求体：`JobCandidateInviteRequest`，可指定 `templateId` 或附加 `cc[]`；若不传则使用企业默认模版。
      - 成功后写入 `inviteStatus=INVITE_SENT`，记录 `lastInviteSentAt` 与 `interviewDeadlineAt=+15d`，失败写入 `INVITE_FAILED` 并返回 500。
    6）查询候选人面试记录
      - 接口：`GET /api/enterprise/job-candidates/{jobCandidateId}/interview-record`
      - 返回 `CandidateInterviewRecordResponse`，包含面试方式、岗位 `positionId`/`positionName`、起止时间、答题截止时间 `answerDeadlineAt`、题目与问答列表、AI 会话 ID、音频片段 `audios[]`（含 `downloadUrl`、`durationSeconds`、`contentType`、`sizeBytes`、`transcript`）、`timedOut` 标记及原始转写 JSON。
      - 若尚未生成面试记录返回 404。
    7）下载候选人面试音频
      - 接口：`GET /api/enterprise/job-candidates/{jobCandidateId}/interview-record/audios/{audioId}`
      - 返回对应音频的二进制内容（带文件名与 `Content-Type`），可直接喂给 `<audio>` 播放；音频不存在时返回 404。
    8）写入/更新候选人面试记录
      - 接口：`PUT /api/enterprise/job-candidates/{jobCandidateId}/interview-record`
      - 请求体：`CandidateInterviewRecordRequest` 支持传入题目数组 `questions[]`、扩展 `metadata`、原始转写 `transcriptRaw`，并新增 `audios[]` 数组（每项包含 `audioId`、`questionSequence`、`fileName`、`contentType`、`durationSeconds`、`transcript`、`base64Content`）。
      - 供 AI 智能体或面试服务调用：保存时会先清除旧音频再写入最新片段，同时同步 `job_candidates.interview_record_id`、`interview_completed_at` 并将状态切到 `COMPLETED`（除非已处于 `ABANDONED/TIMED_OUT/CANCELLED`）。
    9）查询候选人 AI 评估报告
      - 接口：`GET /api/enterprise/job-candidates/{jobCandidateId}/ai-evaluation`
      - 返回 `CandidateAiEvaluationResponse`，字段包含岗位 `positionId`/`positionName`、整体得分、评分等级、优点/待改进项列表、风险提示、能力项评分、AI 模型版本及原始 payload。
    10）写入/更新候选人 AI 评估报告
      - 接口：`PUT /api/enterprise/job-candidates/{jobCandidateId}/ai-evaluation`
      - 请求体：`CandidateAiEvaluationRequest`，可按需传入 `overallScore`、`strengths[]`、`weaknesses[]`、`competencyScores{}`、`customMetrics{}` 等占位字段。
      - 保存后会补齐 `job_candidates.ai_evaluation_id` 并在需要时更新 `interview_completed_at` 与面试状态。
    10）岗位搜索增强
      - `GET /api/enterprise/jobs` 新增 `keyword` 参数，后端按岗位名称模糊筛选。
   ## 4.AI服务交互
    | 场景     | 调用方向                                      | 接口                 | 说明                               |
    | 简历解析   | Candidate Service → AI Resume Parser      | `POST /internal/ai/resume-parser`      | 输入简历文件 URL，输出姓名/邮箱/电话/教育经历等结构化数据 |
    | 面试题目生成 | Interview Service → AI Question Generator | `POST /internal/ai/generate-questions` | 根据岗位和候选人特征生成 15 道题目|
    | 语音转文字  | Interview Service → AI Speech-to-Text     | `POST /internal/ai/stt`                | 上传音频，返回转写文本 |
    | 面试评估   | Report Service → AI Evaluation            | `POST /internal/ai/evaluate`           | 输入问答内容，返回评分和评语 |
   ## 5.数据库设计
    job_candidates 表（岗位-候选人关联）
      | 字段 | 类型 | 说明 |
      | `job_candidate_id` | UUID (PK) | 候选人记录 ID |
      | `position_id` | UUID | 关联岗位 ID，外键指向 `company_recruiting_positions` |
      | `candidate_name` | VARCHAR(255) | 候选人姓名（AI 解析或人工维护）|
      | `candidate_email` | VARCHAR(255) | 邮箱，缺失时 `invite_status=EMAIL_MISSING` |
      | `candidate_phone` | VARCHAR(64) | 联系电话 |
      | `invite_status` | ENUM | `INVITE_PENDING` / `INVITE_SENT` / `INVITE_FAILED` / `EMAIL_MISSING` |
      | `interview_status` | ENUM | `NOT_INTERVIEWED` / `SCHEDULED` / `IN_PROGRESS` / `COMPLETED` / `CANCELLED` / `ABANDONED` / `TIMED_OUT` |
      | `resume_id` | UUID | 关联 `job_candidate_resumes`（1:1，可空）|
      | `interview_record_id` | UUID | 最新面试记录 ID（指向 `candidate_interview_records`）|
      | `ai_evaluation_id` | UUID | 最新 AI 评估 ID（指向 `candidate_ai_evaluations`）|
      | `last_invite_sent_at` | TIMESTAMP | 最近一次成功发送邀约的时间 |
      | `interview_deadline_at` | TIMESTAMP | 系统推算的邀约响应截止时间（默认邀约 +15 天）|
      | `candidate_response_at` | TIMESTAMP | 候选人实际回应时间（接受/放弃时写入）|
      | `interview_completed_at` | TIMESTAMP | 最近一次面试完成时间（记录或评估写入时自动更新）|
      | `uploader_user_id` | UUID | 导入人用户 ID |
      | `created_at` / `updated_at` | TIMESTAMP | 审计字段 |
      - 索引：`job_candidates_position_created_at_idx`（岗位 + 创建时间倒序）、`job_candidates_position_invite_status_idx`、`job_candidates_position_interview_status_idx` 支持状态筛选与滚动分页；均为 `btree` 索引，兼容 Supabase/PostgreSQL。

    job_candidate_resumes 表（简历原文及解析快照）
      | 字段 | 类型 | 说明 |
      | `resume_id` | UUID (PK) | 简历记录 ID |
      | `job_candidate_id` | UUID | 外键，级联删除 |
      | `file_name` / `file_type` | VARCHAR | 上传原始文件信息 |
      | `storage_bucket` / `storage_path` | VARCHAR | Supabase 存储桶与对象路径 |
      | `file_size_bytes` | BIGINT | 原始文件大小（字节）|
      | `parsed_name` / `parsed_email` / `parsed_phone` | VARCHAR | AI 解析出的关键信息 |
      | `parsed_html` | TEXT | AI 解析的 HTML 富文本（可编辑）|
      | `confidence` | DECIMAL(5,2) | AI 置信度 |
      | `ai_raw_result` | TEXT | AI 返回的原始 JSON 或错误消息 |
      | `created_at` / `updated_at` | TIMESTAMP | 审计字段 |

    candidate_interview_records 表（候选人面试问答快照）
      | 字段 | 类型 | 说明 |
      | `record_id` | UUID (PK) | 面试记录 ID |
      | `job_candidate_id` | UUID | 外键，关联 `job_candidates` |
      | `interview_mode` | VARCHAR(32) | 面试方式，例如 `VIDEO` / `AUDIO` |
      | `interviewer_name` | VARCHAR(255) | 面试官或 AI 助手名称 |
      | `ai_session_id` | VARCHAR(64) | 智能体内部会话 ID，便于追踪 |
      | `precheck_status` | VARCHAR(32) | 设备检测状态（`PASSED`/`FAILED`/自定义），配合前端禁用进入按钮 |
      | `precheck_report_json` | TEXT | 设备检测详情（包含各检查项结果）|
      | `precheck_completed_at` | TIMESTAMP | 完成检测的时间 |
      | `room_entered_at` | TIMESTAMP | 候选人进入面试房间的时间（尚未开始作答）|
      | `interview_started_at` / `interview_ended_at` | TIMESTAMP | 面试起止时间 |
      | `answer_deadline_at` | TIMESTAMP | 面试作答截止时间（进入房间后 +60 分钟）|
      | `duration_seconds` | INTEGER | 面试总时长（秒）|
      | `current_question_sequence` | INTEGER | 当前/最近作答的题目序号 |
      | `questions_json` | TEXT | 序列化后的问答数组（`CandidateInterviewQuestionDto[]`）|
      | `transcript_json` | TEXT | 完整对话或语音转写结果（JSON 字符串）|
      | `metadata_json` | TEXT | 自由扩展字段（题目分类、得分明细等）|
      | `profile_photo_storage_bucket` / `profile_photo_storage_path` | VARCHAR | Supabase 桶与对象路径 |
      | `profile_photo_file_name` / `profile_photo_content_type` | VARCHAR | 头像文件名与 MIME |
      | `profile_photo_size_bytes` | BIGINT | 头像大小（字节）|
      | `profile_photo_uploaded_at` | TIMESTAMP | 头像上传时间 |
      | `created_at` / `updated_at` | TIMESTAMP | 审计字段 |

    candidate_interview_audios 表（候选人面试音频片段）
      | 字段 | 类型 | 说明 |
      | `audio_id` | UUID (PK) | 音频片段 ID |
      | `job_candidate_id` | UUID | 外键，关联 `job_candidates`，级联删除 |
      | `interview_record_id` | UUID | 外键，关联 `candidate_interview_records`，级联删除 |
      | `question_sequence` | INTEGER | 对应题目序号，可空 |
      | `file_name` | VARCHAR(255) | 原始文件名，可空 |
      | `content_type` | VARCHAR(100) | MIME 类型，如 `audio/mpeg` |
      | `duration_seconds` | INTEGER | 音频时长（秒）|
      | `size_bytes` | BIGINT | 文件大小（字节）|
      | `transcript` | VARCHAR(2000) | 音频摘要/转写，可空 |
      | `storage_bucket` / `storage_path` | VARCHAR | Supabase 桶名与对象路径 |
      | `created_at` / `updated_at` | TIMESTAMP | 审计字段 |

    candidate_ai_evaluations 表（AI 评估报告）
      | 字段 | 类型 | 说明 |
      | `evaluation_id` | UUID (PK) | 评估记录 ID |
      | `job_candidate_id` | UUID | 外键，关联 `job_candidates` |
      | `interview_record_id` | UUID | 关联的面试记录，可空 |
      | `ai_model_version` | VARCHAR(64) | 智能体模型或版本号 |
      | `overall_score` | DECIMAL(5,2) | 综合得分，占位支持百分制 |
      | `score_level` | VARCHAR(32) | 评分等级（如 A/B/C）|
      | `strengths_json` / `weaknesses_json` / `risk_alerts_json` | TEXT | AI 生成的优势、短板、风险提示列表（JSON 数组）|
      | `recommendations_json` | TEXT | AI 建议（如岗位匹配、培养方向）|
      | `competency_scores_json` | TEXT | 能力项得分映射（JSON Map）|
      | `custom_metrics_json` | TEXT | 其他自定义指标（JSON Map）|
      | `raw_payload` | TEXT | 原始评估数据（完整 JSON，便于调试）|
      | `evaluated_at` | TIMESTAMP | 评估生成时间 |
      | `created_at` / `updated_at` | TIMESTAMP | 审计字段 |
  
  # 六,候选人后端功能
   ## 1.功能概述
     目标：为候选人提供“看到哪些岗位邀请 → 进行设备预检 → 上传头像 → 在线作答 → 完成或放弃”的完整闭环，并保持与企业端共享的候选人、面试记录数据一致。
   ## 2.API 设计（均为 REST）
    | 方法 | URI | 描述 |
    | --- | --- | --- |
    | `GET` | `/api/candidate/interview-sessions/{jobCandidateId}/invitations?status=&keyword=` | 邀约列表。`status` 支持 `ALL`/`WAITING`/`IN_PROGRESS`/`COMPLETED`/`ABANDONED`/`CANCELLED`/`TIMED_OUT`，`keyword` 同时按岗位与企业名称模糊查询。返回的 `CandidateInterviewInvitationItem` 额外包含候选人邮箱、头像元数据（含下载地址）、答题截止时间以及失败原因等字段，便于列表直接展示。|
    | `GET` | `/api/candidate/interview-sessions/{jobCandidateId}` | 单个邀约详情：面试记录快照、预检/头像状态、技术要求列表以及 `answerDeadlineAt`（进入房间后 60 分钟截止时间）。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/precheck` | 上传设备检测结果，`CandidateInterviewPrecheckRequest` 中携带 `summary` 与 `requirements[]`。写入 `precheck_*` 字段并把候选人状态切换为 `SCHEDULED`。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/start` | 进入面试房间，可选 `locale`/`context{}`/`refreshQuestions`。首次进入仅写入 `room_entered_at` 并返回题目列表，尚未开始计时；若该记录已超时直接返回 410。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/begin-answering` | 候选人点击“开始录音”后调用，无需请求体。会校验头像是否上传、是否在答题窗口内，并写入 `interview_started_at` 与 `answer_deadline_at=+60min`，同时将状态切换为 `IN_PROGRESS`。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/profile-photo` | 上传头像（Base64），未上传前 `answers` 接口会返回 409。`GET /profile-photo` 可下载预览。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/answers` | 逐题作答，`CandidateInterviewAnswerRequest` 需携带题号、音频 Base64、前端语音转写文本等。调用前需完成头像上传并执行过 `begin-answering`。接口会在 60 分钟窗口内校验、落库音频并更新 `transcript_json`。返回 `CandidateInterviewAnswerResponse`（含最新面试记录）。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/complete` | 标记面试完成，支持可选总时长与额外 metadata。若仍在答题窗口内则状态切换为 `COMPLETED`；若已超时则保留超时前的问答并返回 `timedOut=true` 供前端提示。|
    | `POST` | `/api/candidate/interview-sessions/{jobCandidateId}/abandon` | 候选人放弃，记录原因并将状态设为 `ABANDONED`。|
    | `GET` | `/api/candidate/interview-sessions/{jobCandidateId}/audios/{audioId}` | 下载单题音频，供回放或质检使用。|
   ## 3.状态与标签
    - 数据库仍沿用 `JobCandidateInterviewStatus`：`NOT_INTERVIEWED`、`SCHEDULED`、`IN_PROGRESS`、`COMPLETED`、`ABANDONED`、`CANCELLED`、`TIMED_OUT`。
    - 前端左侧标签基于聚合：
    - `WAITING` = `NOT_INTERVIEWED` ∪ `SCHEDULED`
    - `IN_PROGRESS` = `IN_PROGRESS`
    - `COMPLETED` = `COMPLETED`
    - 其余标签一一对应枚举值。
    - 头像、预检未完成时，详情接口 `readyForInterview=false`，前端需禁止“开始答题”。
    - 候选人进入房间后 60 分钟内未提交完成会被自动标记为 `TIMED_OUT`，系统会补齐结束时间并在 metadata 中写入 `timeout` 信息；此时头像/答题接口会返回 410，但仍可调用 `complete` 获取超时前的问答快照并提示重新预约。
   ## 4.数据结构
     - 上述接口全部基于 `job_candidates`、`candidate_interview_records`、`candidate_interview_audios` 表；头像、预检结果与作答倒计时 `answer_deadline_at` 均落在 `candidate_interview_records`。
     - AI 题目通过 `InterviewQuestionClient` 调用；默认实现 `RestInterviewQuestionClient` 使用 `POST /interviews/questions:generate`，测试环境切换到 `StubInterviewQuestionClient` 生成固定题目。配置项：`ai.interview-service.base-url`（`application.yml` 默认 `http://localhost:8092`）。
   ## 5.与企业端的联动
     - `candidate_interview_records`、`candidate_interview_audios`、`candidate_ai_evaluations` 仍由企业端和候选人端共享，企业可直接查看候选人答题、下载音频。
     - 候选人完成或放弃后会即时刷新 `job_candidates.interview_status`，方便企业端卡片状态同步。

# 七,AI 智能体接口
  ## 1.服务概述
    - 服务监听 `0.0.0.0:5001`，REST 前缀统一为 `/api/ai`。
    - 所有响应统一为 `{ "code": 200, "data": {...}, "message": "SUCCESS" }` 结构，异常时根据 HTTP 状态返回 `errorCode` 与描述。

  ## 2.生成面试问题接口
    | 方法 | URI | 描述 |
    | --- | --- | --- |
    | `POST` | `/api/ai/questions_generator` | 根据简历与职位描述生成面试问题 |

    - **请求体** `AiQuestionGenerationRequest`
      | 字段 | 类型 | 必填 | 说明 |
      | --- | --- | --- | --- |
      | `resumeUrl` | string | 是 | 简历文件 URL |
      | `jdUrl` | string | 是 | 职位描述 URL |
      | `questionNum` | integer | 否 | 期望题目数量，默认 10（1~50）|
    - **响应体** `AiQuestionGenerationResponse`
      | 字段 | 类型 | 说明 |
      | --- | --- | --- |
      | `questions[]` | string | 生成的问题列表 |
    - **实现说明**：问题文本来自 `ai_question_templates` 表的动态模版。支持占位符 `{{candidateName}}`、`{{jobTitle}}`、`{{jobLocation}}`、`{{sequence}}`、`{{repeatIndex}}`、`{{repeatSuffix}}`，便于运营在数据库中调整题库内容；若模版被停用或为空，接口会返回 502 并提示配置缺失。

  ## 3.文件内容提取接口
    | 方法 | URI | 描述 |
    | --- | --- | --- |
    | `POST` | `/api/ai/extract` | 解析简历或 JD 文档核心信息 |

    - **请求体** `AiExtractionRequest`
      | 字段 | 类型 | 必填 | 说明 |
      | --- | --- | --- | --- |
      | `fileUrl` | string | 是 | 文件 URL |
      | `extractType` | string | 是 | `1` 表示简历，`2` 表示 JD |
    - **响应体**：
      - `extractType=1` 返回 `ResumeExtractionResponse`（`email`、`name`、`telephone`）。
      - `extractType=2` 返回 `JobExtractionResponse`（`location`、`title`）。

  ## 4.异常处理
    - Bean Validation 未通过返回 `400 VALIDATION_ERROR`。
    - 主动抛出的 `ResponseStatusException` 保留原有 HTTP 状态与提示。
    - 未捕获异常统一返回 `500 服务器内部错误`。

  ## 5.数据库结构
    - `ai_resume_extractions`：保存每次简历解析的来源、提取字段与原文快照。
    - `ai_job_extractions`：保存 JD 解析结果（标题、地点等）。
    - `ai_interview_question_sets`：保存题目生成记录，包含简历/JD 快照与问题列表。