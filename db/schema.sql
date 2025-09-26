-- EvolutionAI backend database schema
--
-- This script targets PostgreSQL (Supabase) and captures the tables used by the
-- Spring Data JPA entities in this repository. Run inside the target database
-- to create the required structures.

BEGIN;

-- Optional extensions for nicer data types / functions. They are safe to run on
-- Supabase as they are available by default.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS citext;

-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_accounts (
    user_id         uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        citext      NOT NULL,
    password_hash   text        NOT NULL,
    role            varchar(32) NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_accounts_username_role_key UNIQUE (username, role),
    CONSTRAINT user_accounts_role_check CHECK (role IN ('company', 'engineer'))
);

CREATE INDEX IF NOT EXISTS user_accounts_username_idx
    ON public.user_accounts (username);

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_user_accounts_updated_at
    ON public.user_accounts;
CREATE TRIGGER set_user_accounts_updated_at
    BEFORE UPDATE ON public.user_accounts
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- ============================================================================
--  Core recruiting domain tables
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS app_public;

CREATE TABLE IF NOT EXISTS app_public.jobs (
    id          uuid PRIMARY KEY,
    job_title   varchar(255) NOT NULL,
    description text,
    status      varchar(50)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS set_jobs_updated_at
    ON app_public.jobs;
CREATE TRIGGER set_jobs_updated_at
    BEFORE UPDATE ON app_public.jobs
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS app_public.candidates (
    id         uuid PRIMARY KEY,
    name       varchar(255) NOT NULL,
    email      varchar(255),
    phone      varchar(50),
    status     varchar(50)  NOT NULL DEFAULT 'CREATED',
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS candidates_email_idx
    ON app_public.candidates (email);

DROP TRIGGER IF EXISTS set_candidates_updated_at
    ON app_public.candidates;
CREATE TRIGGER set_candidates_updated_at
    BEFORE UPDATE ON app_public.candidates
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS app_public.interviews (
    id             uuid PRIMARY KEY,
    candidate_id   uuid REFERENCES app_public.candidates (id) ON DELETE SET NULL,
    job_id         uuid REFERENCES app_public.jobs (id) ON DELETE SET NULL,
    scheduled_time timestamptz,
    status         varchar(50) NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS interviews_candidate_idx
    ON app_public.interviews (candidate_id);
CREATE INDEX IF NOT EXISTS interviews_job_idx
    ON app_public.interviews (job_id);

DROP TRIGGER IF EXISTS set_interviews_updated_at
    ON app_public.interviews;
CREATE TRIGGER set_interviews_updated_at
    BEFORE UPDATE ON app_public.interviews
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS app_public.reports (
    report_id          uuid PRIMARY KEY,
    interview_id       uuid NOT NULL REFERENCES app_public.interviews (id) ON DELETE CASCADE,
    content            text,
    score              real,
    evaluator_comment  text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS reports_interview_idx
    ON app_public.reports (interview_id);

DROP TRIGGER IF EXISTS set_reports_updated_at
    ON app_public.reports;
CREATE TRIGGER set_reports_updated_at
    BEFORE UPDATE ON app_public.reports
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

COMMIT;
