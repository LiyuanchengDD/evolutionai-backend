# Microservice API Definitions

## Domain Breakdown
| 领域 | 核心职责 | 主要实体 |
| --- | --- | --- |
| 鉴权与用户管理 | 账户注册、登录、角色与权限（企业管理员、候选人等） | User, Role |
| 企业与岗位管理 | 企业信息维护、岗位创建与发布、岗位状态管理 | Company, Job |
| 候选人管理 | 简历解析、候选人信息维护、岗位与候选人关联 | Candidate, Resume |
| 面试流程管理 | 面试题目生成、答题记录、进度追踪 | Interview, Question, Answer |
| 报告生成与查看 | 面试结果汇总、评估报告生成、下载/分享 | Report |
| 通知与消息服务 | 邀约邮件、面试提醒、系统通知 | EmailTemplate, Notification |
| AI 服务接口 | 题目生成、语音转文字、答案评估 | AI Engine (外部/独立服务) |

## Key Use Cases
- 用户与权限：企业注册、候选人注册/登录、权限控制（企业端 vs 候选人端 vs 管理后台）
- 岗位生命周期：创建岗位 → 编辑/发布 → 状态流转（招募中/已完成） → 删除
- 候选人生命周期：简历上传与解析、候选人与岗位关联、状态管理（未面试/面试中/已完成）
- 面试流程：面试邀请（邮件 + 链接 + 初始密码）、候选人登录 → 答题说明 → 智能出题 → 逐题作答 → 提交完成
- 报告与决策：根据答题内容自动生成评估报告，企业端在线查看、下载或分享，招聘团队依据报告做出录用决策
- 通知与消息：面试邀请、提醒、结果反馈等自动化通知，支持自定义模板、多语言输出
- AI 能力支撑：简历信息提取、面试题目生成、语音识别、自动评估，统一以 API 形式对外提供服务

## gRPC Interfaces

### Auth Service
- `rpc RegisterUser(RegisterUserRequest) returns (UserResponse)`
- `rpc LoginUser(LoginRequest) returns (UserResponse)`

### Job Service
- `rpc CreateJob(CreateJobRequest) returns (JobResponse)`
- `rpc GetJob(JobRequest) returns (JobResponse)`
- `rpc ListJobs(ListJobsRequest) returns (ListJobsResponse)`

### Candidate Service
- `rpc CreateCandidate(CreateCandidateRequest) returns (CandidateResponse)`
- `rpc GetCandidate(CandidateRequest) returns (CandidateResponse)`
- `rpc ListCandidates(ListCandidatesRequest) returns (ListCandidatesResponse)`

### Interview Service
- `rpc ScheduleInterview(ScheduleInterviewRequest) returns (InterviewResponse)`
- `rpc ConfirmInterview(ConfirmInterviewRequest) returns (InterviewResponse)`
- `rpc GetInterviewsByCandidate(GetInterviewsByCandidateRequest) returns (InterviewsResponse)`
- `rpc GetInterviewsByJob(GetInterviewsByJobRequest) returns (InterviewsResponse)`

### Report Service
- `rpc GenerateReport(GenerateReportRequest) returns (ReportResponse)`
- `rpc GetReport(GetReportRequest) returns (ReportResponse)` — 若报告不存在返回 `NOT_FOUND`

`ReportResponse` 字段：
- `reportId`
- `interviewId`
- `content`
- `score`
- `evaluatorComment`
- `createdAt`（毫秒级时间戳）

### Notification Service
- `rpc SendInvitation(SendInvitationRequest) returns (SendInvitationResponse)` — 发送面试邀约邮件

`SendInvitationRequest` 字段：
- `email`
- `subject`
- `content`

`SendInvitationResponse` 字段：
- `success`
- `message`

Each service maintains its own data store and communicates over gRPC to ensure loose coupling and scalability.