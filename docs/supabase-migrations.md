# Supabase 数据库设置

本文档介绍如何让 Supabase（PostgreSQL）项目与本仓库中的实体保持一致，并使用 Supabase CLI 管理未来的架构变更。

## 数据表概览

该应用使用 Spring Data JPA，包含以下持久化聚合：

| Java 类型 | 表 | 作用 |
|-----------|----|------|
| `UserAccountEntity` | `public.user_accounts` | 存储注册的登录账号。字段包含登录邮箱 `email`、`password_hash`、`role`、`status`（`ACTIVE/PENDING/LOCKED`）以及最近登录时间 `last_login_at`，邮箱字段全局唯一。 |
| `CompanyProfileEntity` | `public.company_profiles` | 保存企业引导完成后的主体信息，包括企业规模、所在地等。 |
| `CompanyContactEntity` | `public.company_contacts` | 保存企业 HR 联系人及其电话、邮箱等信息。 |
| `InvitationTemplateEntity` | `public.invitation_templates` | 保存企业配置的邀约邮件模版，并通过部分唯一索引保证每个企业只有一个默认模版。 |
| `VerificationTokenEntity` | `public.verification_tokens` | 保存企业引导流程中发送的验证码，索引覆盖 `(target_user_id, purpose, code, consumed)` 以支撑查询。 |
| `Job` | `public.jobs` | 表示通过 gRPC job 服务对外暴露的职位信息。 |
| `Candidate` | `public.candidates` | 表示导入系统的候选人。 |
| `Interview` | `public.interviews` | 将候选人与职位关联，并保存面试的排期状态。 |
| `ReportEntity` | `public.reports` | 保存完成面试后的 AI 评估结果。 |

字段类型、长度限制以及约束定义都在 [`db/schema.sql`](../db/schema.sql) 中给出。

> **登录模块如何落地？** `public.user_accounts` 承担了注册与登录所需的全部字段（邮箱、哈希后的密码、角色、账号状态、最近登录时间及审计时间戳），其结构与 Spring Security/JPA 中的 `UserAccountEntity` 一一对应。脚本同时会创建 `set_user_accounts_updated_at` 触发器来维护更新时间戳；执行 `db/schema.sql` 后就具备了后端代码完成注册、鉴权的最低要求。

> **企业引导如何持久化？** `public.company_profiles`、`public.company_contacts`、`public.invitation_templates` 与 `public.verification_tokens` 共同覆盖引导流程落库所需的全部字段。触发器会自动维护 `updated_at` 字段，索引保证查询效率，并利用部分唯一索引确保企业默认模版唯一。

## 应用数据库结构

1. 认证 Supabase CLI 并关联到你的项目：
   ```bash
   supabase login
   supabase link --project-ref <your-project-ref>
   ```
2. 将数据库结构推送到远程数据库：
   ```bash
   supabase db push --file db/schema.sql
   ```
   该命令会在项目的主数据库上执行 `db/schema.sql` 中的 SQL，并在 CLI 的历史记录中保留部署信息。

你也可以打开 Supabase SQL 编辑器，粘贴 `db/schema.sql` 的内容来手动创建这些数据表。

> **注意权限问题**：Supabase 的 `auth` schema 由平台内部管理，普通数据库角色（如 `postgres`、`service_role`）默认没有在该 schema 下创建表或函数的权限。如果执行旧版本的脚本出现 `permission denied for schema auth`，请改用当前版本的 `db/schema.sql`，它只会在 `public` schema 中创建业务所需的对象。

## 使用迁移跟踪后续变更

Supabase CLI 支持可纳入 Git 版本管理的迁移脚本：

```bash
supabase migration new add_status_column_to_jobs
```

该命令会在 `supabase/migrations` 目录下创建一个带时间戳的 SQL 文件。将 DDL 语句（例如 `ALTER TABLE app_public.jobs ADD COLUMN ...`）写入该文件，提交后再通过 `supabase db push` 推送到数据库。

将架构变更纳入版本控制，可以确保云端数据库与 Java 实体在项目演进过程中保持同步。
