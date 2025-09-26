# Supabase 数据库设置

本文档介绍如何让 Supabase（PostgreSQL）项目与本仓库中的实体保持一致，并使用 Supabase CLI 管理未来的架构变更。

## 数据表概览

该应用使用 Spring Data JPA，包含以下持久化聚合：

| Java 类型 | 表 | 作用 |
|-----------|----|------|
| `UserAccountEntity` | `public.user_accounts` | 存储注册的登录账号。代码需要 `username`、`password_hash` 和 `role` 字段，并在 `(username, role)` 上设置唯一约束。 |
| `Job` | `app_public.jobs` | 表示通过 gRPC job 服务对外暴露的职位信息。 |
| `Candidate` | `app_public.candidates` | 表示导入系统的候选人。 |
| `Interview` | `app_public.interviews` | 将候选人与职位关联，并保存面试的排期状态。 |
| `ReportEntity` | `app_public.reports` | 保存完成面试后的 AI 评估结果。 |

字段类型、长度限制以及约束定义都在 [`db/schema.sql`](../db/schema.sql) 中给出。

> **登录模块如何落地？** 上表中的 `public.user_accounts` 承担了注册与登录所需的全部字段（用户名、哈希后的密码、角色及审计时间戳），其结构与 Spring Security/JPA 中的 `UserAccountEntity` 一一对应。脚本还包含触发器 `set_user_accounts_updated_at` 用于维护更新时间戳；执行 `db/schema.sql` 后就具备了后端代码完成注册、鉴权的最低要求，不需要额外手动建表。

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

> **注意权限问题**：Supabase 的 `auth` schema 由平台内部管理，普通数据库角色（如 `postgres`、`service_role`）默认没有在该 schema 下创建表或函数的权限。如果执行旧版本的脚本出现 `permission denied for schema auth`，请改用当前版本的 `db/schema.sql`，它会在 `public` 与 `app_public` schema 中创建业务所需的对象。

## 使用迁移跟踪后续变更

Supabase CLI 支持可纳入 Git 版本管理的迁移脚本：

```bash
supabase migration new add_status_column_to_jobs
```

该命令会在 `supabase/migrations` 目录下创建一个带时间戳的 SQL 文件。将 DDL 语句（例如 `ALTER TABLE app_public.jobs ADD COLUMN ...`）写入该文件，提交后再通过 `supabase db push` 推送到数据库。

将架构变更纳入版本控制，可以确保云端数据库与 Java 实体在项目演进过程中保持同步。
