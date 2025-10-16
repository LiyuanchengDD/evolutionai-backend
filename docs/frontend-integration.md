# 前端联调指引

后端已切换为 **Supabase 身份验证 + Spring Security 资源服务器** 的模式。所有业务
接口都要求前端在 `Authorization` 头部携带 `Bearer <access_token>`，并会根据 JWT 中的
`sub`（用户 ID）与 `email` 自动建立登录态。企业端路由（`/api/enterprise/**`）会统一经过
14 天试用网关拦截，根据 `trial_invitations` 记录判定是否放行。

## 认证与环境变量

后端通过以下环境变量控制行为：

```bash
# Supabase 相关
SUPABASE_PROJECT_URL=https://<project>.supabase.co
SUPABASE_JWKS_URL=https://<project>.supabase.co/auth/v1/jwks
SUPABASE_SERVICE_ROLE_KEY=...           # 后端使用，请勿下发前端

# 数据库连接
DB_HOST=aws-1-ap-northeast-1.pooler.supabase.com
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=<secret>
DATABASE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require&preferQueryMode=simple

# 试用 / 邀请码配置
TRIAL_VALID_DAYS=14
APP_TRIAL_MODE=dev-fixed               # dev-fixed | prod
APP_ENV=dev                            # prod 环境禁止 dev-fixed
DEV_TRIAL_CODE=EA-TRIAL-123456
```

当 `APP_ENV=prod` 且 `APP_TRIAL_MODE=dev-fixed` 时应用会直接拒绝启动，避免误把联调固定码
部署到生产环境。【F:src/main/java/com/example/grpcdemo/config/AppTrialProperties.java†L8-L44】

### 认证模式切换

| APP_AUTH_MODE | 说明 | 典型场景 |
| ------------- | ---- | -------- |
| `supabase`    | 只接受来自 Supabase 的 JWT，`iss` 必须等于 `${SUPABASE_PROJECT_URL}/auth/v1`。 | 正式 / 预发环境 |
| `dev-otp`     | 先尝试 Supabase JWT，失败后回退到本地 HMAC256 生成的开发 token。 | 本地或联调占位 |

`APP_ENV=prod` 时若误把 `APP_AUTH_MODE` 设为 `dev-otp`，应用会在启动阶段直接抛错退出，避免
进入生产。相关配置均定义在 `app.auth.*` 与 `app.trial.*` 节点，可参考
`src/main/resources/application.yml`。【F:src/main/resources/application.yml†L36-L55】

### Supabase 正式模式

1. 前端使用 `supabase-js` 走 OTP 登录：`signInWithOtp` → `verifyOtp`。
2. 调用 `supabase.auth.getSession()` 拿到 `access_token`。
3. 之后所有请求都带上 `Authorization: Bearer <access_token>` 即可访问后端。

后端仅负责验证 Supabase 签发的 token，不再提供注册/登录接口。

### 用户类型同步

Supabase 的 OTP 登录不会在 token 中携带“个人 / 企业”标记。前端在拿到 access token 后需要额外
调用 `PUT /auth/kind` 告诉后端当前登录入口，例如：

```http
PUT /auth/kind
Authorization: Bearer <access_token>
Content-Type: application/json

{ "kind": "enterprise" }
```

`kind` 仅接受 `individual` 或 `enterprise`，未调用时默认会按个人用户处理，无法访问
`/api/enterprise/**` 路由。接口会返回与 `GET /auth/me` 相同结构，可直接复用以刷新前端状态。

### dev-otp 占位模式

便于前端在未接入 Supabase 之前完成接口联调。流程：

1. **请求验证码** `POST /dev/auth/send-otp`

   ```json
   { "email": "dev@example.com" }
   ```

   响应：

   ```json
   { "dev": true, "code": "123456" }
   ```

   验证码固定，可直接展示在前端或日志。只有在 `APP_AUTH_MODE=dev-otp` 时存在。

2. **验证验证码** `POST /dev/auth/verify-otp`

   ```json
   { "email": "dev@example.com", "code": "123456" }
   ```

   成功响应：

   ```json
   {
     "access_token": "<dev-jwt>",
     "token_type": "bearer",
     "expires_in": 86400
   }
   ```

   其中 `access_token` 为 HMAC256 生成的 JWT，`iss=app://dev`、`role=authenticated`，有效期默认
   24 小时（可通过 `DEV_JWT_EXPIRES_MIN` 调整）。验证码错误会返回 `400 INVALID_OTP`。

3. 携带 `Authorization: Bearer <dev-jwt>` 请求业务接口。

切换回 `APP_AUTH_MODE=supabase` 后，这两个 `/dev/auth/*` 接口自动下线。

## 公共用户信息接口

`GET /auth/me` 在任意模式下均可使用，用于前端渲染登录用户信息：

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "kind": "enterprise",
  "trial": {
    "status": "valid",
    "sentAt": "2024-04-15T12:00:00Z",
    "expiresAt": "2024-04-29T12:00:00Z"
  }
}
```

`kind` 来源于 `user_profiles.kind`，默认 `individual`。`trial.status` 取值：

* `valid`：存在有效期内的邀请码记录。
* `redeemed`：邀请码已成功核销，仍可访问企业路由。
* `not_sent`：未找到发送记录。
* `expired`：超过 `TRIAL_VALID_DAYS` 有效期。

`/auth/me` 不受试用期拦截影响，便于前端在 403 前获取状态提示。实现见
`AuthMeController`。【F:src/main/java/com/example/grpcdemo/controller/AuthMeController.java†L39-L103】

## 试用申请与固定码联调

### 用户端接口

* `POST /trial/apply`
  * 入参：`{ companyName, contactEmail, reason? }`
  * 需登录。若当前用户已有 `PENDING` 申请则幂等返回原记录。
  * 响应：`{ id, status: "PENDING" }`
* `POST /trial/verify`
  * 入参：`{ code }`，企业账号输入邀请码。
  * `dev-fixed` 模式下仅需传入 `DEV_TRIAL_CODE` 即可完成写入与核销；正式模式会校验最近
    14 天内发送的记录与验证码是否匹配。
  * 成功返回 `{ status, sentAt, expiresAt }`，并标记 `trial_invitations.redeemed_*` 字段。

具体实现参考 `TrialController` 与 `TrialService`。【F:src/main/java/com/example/grpcdemo/controller/TrialController.java†L7-L58】【F:src/main/java/com/example/grpcdemo/security/trial/TrialService.java†L20-L162】

### 后台接口

管理员需携带具备 `ROLE_ADMIN` 的 token：

* `GET /admin/trial/applications?status=PENDING|APPROVED|REJECTED`
  * 返回试用申请列表，包含申请人、邮箱、审核状态等字段。
* `POST /admin/trial/review/{id}`
  * Body: `{ "approve": true|false, "note"?: "..." }`
  * 将申请标记为通过/拒绝并记录审核人。
* `POST /admin/trial/send`
  * 支持 `{ applicationId }`（需先审批通过）或 `{ email, companyId }` 两种入参。
  * `dev-fixed` 模式始终返回固定码 `EA-TRIAL-123456`；`prod` 模式生成随机码并返回
    `{ "code": null, "status": "valid", ... }`，以免泄露真实邀请码。
  * 14 天内存在发送记录会直接返回原记录的 `sentAt/expiresAt`，避免重复轰炸邮箱。

控制器实现详见 `TrialAdminController`。【F:src/main/java/com/example/grpcdemo/controller/TrialAdminController.java†L7-L137】

## Token 校验失败 & 试用期拦截

* 401 `UNAUTHORIZED`：缺少或非法的 Bearer token。
* 401 `TOKEN_EXPIRED`：`exp` 已过期。
* 403 `TRIAL_INVITE_NOT_SENT`：尚未发送邀请码。
* 403 `TRIAL_INVITE_EXPIRED`：邀请码超过有效期。
* 403 `NOT_ENTERPRISE_USER`：尝试访问企业路由但用户类型为个人或缺少企业信息。

试用网关仅作用于 `/api/enterprise/**`，并在首次放行时自动将邀请码标记为已核销。逻辑详见
`TrialAccessFilter`。【F:src/main/java/com/example/grpcdemo/security/trial/TrialAccessFilter.java†L18-L97】

## 企业端接口注意事项

企业端 API 仍保留原有请求体，但后端会将 token 中的 `userId` 与请求参数 / 负载中的
`userId` 做比对，不一致会返回 403 `用户身份不匹配`。请务必从 `/auth/me` 获取 `userId` 并附带到
请求中。实现参考 `CompanyProfileController` 与 `EnterpriseOnboardingController`，均通过
`@AuthenticationPrincipal` 读取当前用户并在进入服务层前执行一致性校验。
【F:src/main/java/com/example/grpcdemo/controller/CompanyProfileController.java†L36-L87】【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L35-L86】

## 数据库调整

`db/schema.sql` 新增 `user_profiles`、`trial_applications` 表以及扩展后的 `trial_invitations`
结构，用于记录企业用户类型、试用申请及邀请码核销信息。请在 Supabase 数据库中执行该脚本
保持结构同步。【F:db/schema.sql†L21-L78】

## 调试小结

1. 本地默认 `APP_AUTH_MODE=dev-otp`、`APP_TRIAL_MODE=dev-fixed`，可直接通过固定码 `EA-TRIAL-123456`
   完成企业入口联调。
2. 部署到预发/生产时需切换 `APP_TRIAL_MODE=prod`，后端会生成随机邀请码并（预留）发送邮件。
3. 无论哪种模式，`/auth/me` 与试用期拦截逻辑保持一致，业务层无感知差异。

