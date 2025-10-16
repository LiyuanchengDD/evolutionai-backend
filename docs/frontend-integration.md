# 前端联调指引

后端已切换为 **Supabase 身份验证 + Spring Security 资源服务器** 的模式。所有业务
接口都要求前端在 `Authorization` 头部携带 `Bearer <access_token>`，并会根据 JWT 中的
`sub`（用户 ID）与 `email` 自动建立登录态。14 天试用资格仍通过 `trial_invitations`
表校验，未发放或已过期都会返回 403 拦截。

## 认证模式说明

后端通过 `APP_AUTH_MODE` 和 `APP_ENV` 两个环境变量控制行为：

| APP_AUTH_MODE | 说明 | 典型场景 |
| ------------- | ---- | -------- |
| `supabase`    | 只接受来自 Supabase 的 JWT。`iss` 必须等于 `${SUPABASE_PROJECT_URL}/auth/v1`。 | 正式 / 预发环境 |
| `dev-otp`     | 先尝试 Supabase JWT，失败后回退到本地 HMAC256 生成的开发 token。 | 本地或联调占位 |

`APP_ENV=prod` 时若误把 `APP_AUTH_MODE` 设为 `dev-otp`，应用会在启动阶段直接抛错退出
避免进入生产。相关配置均定义在 `app.auth.*` 节点，可参考 `src/main/resources/application.yml`。

### Supabase 正式模式

1. 前端使用 `supabase-js` 走 OTP 登录：`signInWithOtp` → `verifyOtp`。
2. 调用 `supabase.auth.getSession()` 拿到 `access_token`。
3. 之后所有请求都带上 `Authorization: Bearer <access_token>` 即可访问后端。

后端仅负责验证 Supabase 签发的 token，不再提供注册/登录接口。

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

   其中 `access_token` 为 HMAC256 生成的 JWT，`iss=app://dev`、`role=authenticated`，有效期默认 24 小时（可通过
   `DEV_JWT_EXPIRES_MIN` 调整）。验证码错误会返回 `400 INVALID_OTP`。

3. 携带 `Authorization: Bearer <dev-jwt>` 请求业务接口。

切换回 `APP_AUTH_MODE=supabase` 后，这两个 `/dev/auth/*` 接口自动下线。

## 公共用户信息接口

`GET /auth/me` 在两种模式下均可使用，用于前端渲染登录用户信息：

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "email": "dev@example.com",
  "dev": true,
  "trial": {
    "status": "active",
    "expiresAt": "2024-05-01T12:00:00Z"
  }
}
```

`trial.status` 取值：`active`、`not_sent`、`expired`。`/auth/me` 不受试用期拦截影响，
以便前端在 403 前获取状态提示。【F:src/main/java/com/example/grpcdemo/controller/AuthMeController.java†L7-L37】

## Token 校验失败 & 试用期拦截

* 401 `UNAUTHORIZED`：缺少或非法的 Bearer token。
* 401 `TOKEN_EXPIRED`：`exp` 已过期。
* 403 `TRIAL_INVITE_NOT_SENT`：`trial_invitations` 无记录。
* 403 `TRIAL_INVITE_EXPIRED`：发送时间超过 14 天。

后端在成功解析 JWT 后会触发试用期过滤器，未通过校验时直接返回 JSON 错误体并终止
请求链路。【F:src/main/java/com/example/grpcdemo/security/trial/TrialAccessFilter.java†L18-L78】

## 企业端接口注意事项

大部分企业端 API 依然保留原有请求体，但后端会将 token 中的 `userId` 与请求参数/负载
中的 `userId` 做比对，不一致会返回 403 `用户身份不匹配`。请务必从 `/auth/me` 获取
`userId` 并附带到请求中。

以企业资料维护为例：

* `GET /api/enterprise/profile?userId={ownerUserId}` —— 必须同时带上 Bearer token，后端会
  校验 token 内的 `sub` 与 `userId` 是否一致。
* `PUT /api/enterprise/profile/company`、`POST /api/enterprise/profile/hr`、`PUT /api/enterprise/profile/hr/{contactId}`
  的请求体中也需要携带 `userId` 字段，后端会做同样的比对。

实现参考：`CompanyProfileController` 与 `EnterpriseOnboardingController` 均注入了
`@AuthenticationPrincipal` 来读取当前用户，并在进入服务层前执行一致性校验。
【F:src/main/java/com/example/grpcdemo/controller/CompanyProfileController.java†L36-L87】【F:src/main/java/com/example/grpcdemo/controller/EnterpriseOnboardingController.java†L35-L86】

## 数据库调整

`db/schema.sql` 已移除历史验证码表 `auth_verification_codes`，保留 `user_accounts` 供企业
联系人等业务场景使用。Supabase 用户 ID 应直接写入业务表，后端不再自行生成登录凭证。
【F:db/schema.sql†L21-L78】

## 调试小结

1. 本地默认 `APP_AUTH_MODE=dev-otp`，可直接通过 `/dev/auth/*` 获取联调用 token。
2. 部署到预发/生产时切换回 `APP_AUTH_MODE=supabase`，前端改走 Supabase 官方登录流程。
3. 无论哪种模式，`/auth/me` 和试用期拦截逻辑保持一致，业务层无感知差异。

