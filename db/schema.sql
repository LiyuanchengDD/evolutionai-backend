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

-- Helper trigger function for automatically updating `updated_at` columns.
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
--  User accounts & authentication
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_accounts (
    user_id         uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           citext      NOT NULL,
    password_hash   text        NOT NULL,
    role            varchar(32) NOT NULL,
    status          varchar(16) NOT NULL DEFAULT 'PENDING',
    last_login_at   timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_accounts_email_key UNIQUE (email),
    CONSTRAINT user_accounts_role_check CHECK (role IN ('company', 'engineer', 'admin')),
    CONSTRAINT user_accounts_status_check CHECK (status IN ('ACTIVE', 'PENDING', 'LOCKED'))
);

CREATE INDEX IF NOT EXISTS user_accounts_email_idx
    ON public.user_accounts (email);

DROP TRIGGER IF EXISTS set_user_accounts_updated_at
    ON public.user_accounts;
CREATE TRIGGER set_user_accounts_updated_at
    BEFORE UPDATE ON public.user_accounts
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TYPE IF NOT EXISTS public.user_kind AS ENUM ('individual', 'enterprise');

CREATE TABLE IF NOT EXISTS public.user_profiles (
    id          uuid PRIMARY KEY,
    kind        public.user_kind NOT NULL DEFAULT 'individual',
    company_id  uuid,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.trial_invitations (
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id   uuid        NOT NULL,
    email        citext      NOT NULL,
    code         text        NOT NULL,
    sent_at      timestamptz NOT NULL DEFAULT now(),
    redeemed_at  timestamptz,
    redeemed_by  uuid,
    CONSTRAINT trial_invitations_company_fk FOREIGN KEY (company_id) REFERENCES public.company_profiles (company_id) ON DELETE CASCADE,
    CONSTRAINT trial_invitations_email_code_uidx UNIQUE (email, code)
);

CREATE INDEX IF NOT EXISTS trial_invitations_email_idx
    ON public.trial_invitations (email);

CREATE INDEX IF NOT EXISTS trial_invitations_sent_at_idx
    ON public.trial_invitations (sent_at);

CREATE INDEX IF NOT EXISTS trial_invitations_company_id_idx
    ON public.trial_invitations (company_id);

CREATE TYPE IF NOT EXISTS public.review_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE IF NOT EXISTS public.trial_applications (
    id                 uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    applicant_user_id  uuid        NOT NULL,
    company_name       text        NOT NULL,
    contact_email      citext      NOT NULL,
    reason             text,
    status             public.review_status NOT NULL DEFAULT 'PENDING',
    reviewed_by        uuid,
    reviewed_at        timestamptz,
    note               text,
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS trial_applications_status_idx
    ON public.trial_applications (status);

-- ============================================================================
--  Enterprise onboarding persistence
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.company_profiles (
    company_id           uuid PRIMARY KEY,
    owner_user_id        uuid        NOT NULL UNIQUE,
    company_name         varchar(255) NOT NULL,
    company_short_name   varchar(255),
    social_credit_code   varchar(64),
    employee_scale       varchar(64)  NOT NULL,
    annual_hiring_plan   varchar(64)  NOT NULL,
    industry             varchar(255),
    country_code         varchar(8)   NOT NULL,
    city_code            varchar(16)  NOT NULL,
    website              varchar(255),
    description          varchar(1000),
    detailed_address     varchar(255),
    status               varchar(32)  NOT NULL,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS set_company_profiles_updated_at
    ON public.company_profiles;
CREATE TRIGGER set_company_profiles_updated_at
    BEFORE UPDATE ON public.company_profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.company_contacts (
    contact_id          uuid PRIMARY KEY,
    company_id          uuid        NOT NULL REFERENCES public.company_profiles (company_id) ON DELETE CASCADE,
    user_account_id     uuid        REFERENCES public.user_accounts (user_id) ON DELETE SET NULL,
    contact_name        varchar(128) NOT NULL,
    contact_email       varchar(255) NOT NULL,
    phone_country_code  varchar(8)   NOT NULL,
    phone_number        varchar(32)  NOT NULL,
    position            varchar(128),
    department          varchar(128),
    is_primary          boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS company_contacts_company_id_idx
    ON public.company_contacts (company_id);

CREATE INDEX IF NOT EXISTS company_contacts_user_account_id_idx
    ON public.company_contacts (user_account_id);

DROP TRIGGER IF EXISTS set_company_contacts_updated_at
    ON public.company_contacts;
CREATE TRIGGER set_company_contacts_updated_at
    BEFORE UPDATE ON public.company_contacts
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.company_recruiting_positions (
    position_id    uuid PRIMARY KEY,
    company_id     uuid        NOT NULL REFERENCES public.company_profiles (company_id) ON DELETE CASCADE,
    position_name  varchar(255) NOT NULL,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS company_recruiting_positions_company_id_idx
    ON public.company_recruiting_positions (company_id);

DROP TRIGGER IF EXISTS set_company_recruiting_positions_updated_at
    ON public.company_recruiting_positions;
CREATE TRIGGER set_company_recruiting_positions_updated_at
    BEFORE UPDATE ON public.company_recruiting_positions
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.company_job_documents (
    document_id      uuid PRIMARY KEY,
    position_id      uuid        NOT NULL REFERENCES public.company_recruiting_positions (position_id) ON DELETE CASCADE,
    file_name        varchar(255),
    file_type        varchar(100),
    storage_bucket   varchar(128),
    storage_path     varchar(512),
    file_size_bytes  bigint,
    upload_user_id   uuid,
    ai_raw_result    text,
    parsed_title     varchar(255),
    parsed_location  varchar(255),
    parsed_publisher varchar(255),
    confidence       numeric(5, 2),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS company_job_documents_position_uidx
    ON public.company_job_documents (position_id);

DROP TRIGGER IF EXISTS set_company_job_documents_updated_at
    ON public.company_job_documents;
CREATE TRIGGER set_company_job_documents_updated_at
    BEFORE UPDATE ON public.company_job_documents
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.invitation_templates (
    template_id     uuid PRIMARY KEY,
    company_id      uuid        NOT NULL REFERENCES public.company_profiles (company_id) ON DELETE CASCADE,
    template_name   varchar(255) NOT NULL,
    subject         varchar(255) NOT NULL,
    body            varchar(5000) NOT NULL,
    language        varchar(32)  NOT NULL,
    is_default      boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS invitation_templates_company_id_idx
    ON public.invitation_templates (company_id);

DROP TRIGGER IF EXISTS set_invitation_templates_updated_at
    ON public.invitation_templates;
CREATE TRIGGER set_invitation_templates_updated_at
    BEFORE UPDATE ON public.invitation_templates
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS invitation_templates_company_default_uidx
    ON public.invitation_templates (company_id)
    WHERE is_default;

-- ============================================================================
--  AI 面试题模版配置
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.ai_question_templates (
    template_id   uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    language      varchar(32)  NOT NULL,
    content       varchar(2000) NOT NULL,
    display_order integer,
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ai_question_templates_language_idx
    ON public.ai_question_templates (language, is_active);

DROP TRIGGER IF EXISTS set_ai_question_templates_updated_at
    ON public.ai_question_templates;
CREATE TRIGGER set_ai_question_templates_updated_at
    BEFORE UPDATE ON public.ai_question_templates
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- ============================================================================
--  AI 智能体开放能力历史记录
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.ai_resume_extractions (
    extraction_id   uuid PRIMARY KEY,
    file_url        varchar(1000) NOT NULL,
    file_type       varchar(128),
    extracted_name  varchar(128),
    extracted_email varchar(255),
    extracted_phone varchar(64),
    raw_text        text,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.ai_job_extractions (
    extraction_id     uuid PRIMARY KEY,
    file_url          varchar(1000) NOT NULL,
    file_type         varchar(128),
    extracted_title   varchar(255),
    extracted_location varchar(255),
    raw_text          text,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.ai_interview_question_sets (
    record_id        uuid PRIMARY KEY,
    resume_url       varchar(1000) NOT NULL,
    jd_url           varchar(1000) NOT NULL,
    question_num     integer,
    questions_json   text,
    candidate_name   varchar(128),
    candidate_email  varchar(255),
    job_title        varchar(255),
    job_location     varchar(255),
    resume_snapshot  text,
    jd_snapshot      text,
    created_at       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.job_candidates (
    job_candidate_id uuid PRIMARY KEY,
    position_id      uuid        NOT NULL REFERENCES public.company_recruiting_positions (position_id) ON DELETE CASCADE,
    candidate_name   varchar(255),
    candidate_email  varchar(255),
    candidate_phone  varchar(64),
    invite_status    varchar(32)  NOT NULL,
    interview_status varchar(32)  NOT NULL,
    resume_id        uuid,
    interview_record_id uuid,
    ai_evaluation_id    uuid,
    last_invite_sent_at timestamptz,
    interview_deadline_at timestamptz,
    candidate_response_at timestamptz,
    interview_completed_at timestamptz,
    uploader_user_id uuid,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS job_candidates_position_id_idx
    ON public.job_candidates (position_id);

CREATE INDEX IF NOT EXISTS job_candidates_position_created_at_idx
    ON public.job_candidates (position_id, created_at DESC);

CREATE INDEX IF NOT EXISTS job_candidates_position_invite_status_idx
    ON public.job_candidates (position_id, invite_status, created_at DESC);

CREATE INDEX IF NOT EXISTS job_candidates_position_interview_status_idx
    ON public.job_candidates (position_id, interview_status, created_at DESC);

DROP TRIGGER IF EXISTS set_job_candidates_updated_at
    ON public.job_candidates;
CREATE TRIGGER set_job_candidates_updated_at
    BEFORE UPDATE ON public.job_candidates
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.job_candidate_resumes (
    resume_id       uuid PRIMARY KEY,
    job_candidate_id uuid       NOT NULL REFERENCES public.job_candidates (job_candidate_id) ON DELETE CASCADE,
    file_name       varchar(255),
    file_type       varchar(100),
    storage_bucket  varchar(128),
    storage_path    varchar(512),
    file_size_bytes bigint,
    parsed_name     varchar(255),
    parsed_email    varchar(255),
    parsed_phone    varchar(64),
    parsed_html     text,
    confidence      numeric(5, 2),
    ai_raw_result   text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS job_candidate_resumes_candidate_uidx
    ON public.job_candidate_resumes (job_candidate_id);

DROP TRIGGER IF EXISTS set_job_candidate_resumes_updated_at
    ON public.job_candidate_resumes;
CREATE TRIGGER set_job_candidate_resumes_updated_at
    BEFORE UPDATE ON public.job_candidate_resumes
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.candidate_interview_records (
    record_id             uuid PRIMARY KEY,
    job_candidate_id      uuid        NOT NULL REFERENCES public.job_candidates (job_candidate_id) ON DELETE CASCADE,
    interview_mode        varchar(32),
    interviewer_name      varchar(255),
    ai_session_id         varchar(64),
    precheck_status       varchar(32),
    precheck_report_json  text,
    precheck_completed_at timestamptz,
    room_entered_at       timestamptz,
    interview_started_at  timestamptz,
    interview_ended_at    timestamptz,
    answer_deadline_at    timestamptz,
    duration_seconds      integer,
    answer_elapsed_seconds integer,
    answer_resumed_at     timestamptz,
    answer_paused_at      timestamptz,
    current_question_sequence integer,
    questions_json        text,
    transcript_json       text,
    metadata_json         text,
    profile_photo_storage_bucket varchar(128),
    profile_photo_storage_path   varchar(512),
    profile_photo_file_name   varchar(255),
    profile_photo_content_type varchar(100),
    profile_photo_size_bytes   bigint,
    profile_photo_uploaded_at  timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS candidate_interview_records_candidate_idx
    ON public.candidate_interview_records (job_candidate_id);

DROP TRIGGER IF EXISTS set_candidate_interview_records_updated_at
    ON public.candidate_interview_records;
CREATE TRIGGER set_candidate_interview_records_updated_at
    BEFORE UPDATE ON public.candidate_interview_records
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.candidate_interview_audios (
    audio_id            uuid PRIMARY KEY,
    job_candidate_id    uuid        NOT NULL REFERENCES public.job_candidates (job_candidate_id) ON DELETE CASCADE,
    interview_record_id uuid        NOT NULL REFERENCES public.candidate_interview_records (record_id) ON DELETE CASCADE,
    question_sequence   integer,
    file_name           varchar(255),
    content_type        varchar(100),
    duration_seconds    integer,
    size_bytes          bigint,
    transcript          varchar(2000),
    storage_bucket      varchar(128) NOT NULL,
    storage_path        varchar(512) NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS candidate_interview_audios_candidate_idx
    ON public.candidate_interview_audios (job_candidate_id, interview_record_id);

DROP TRIGGER IF EXISTS set_candidate_interview_audios_updated_at
    ON public.candidate_interview_audios;
CREATE TRIGGER set_candidate_interview_audios_updated_at
    BEFORE UPDATE ON public.candidate_interview_audios
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.candidate_ai_evaluations (
    evaluation_id         uuid PRIMARY KEY,
    job_candidate_id      uuid        NOT NULL REFERENCES public.job_candidates (job_candidate_id) ON DELETE CASCADE,
    interview_record_id   uuid,
    ai_model_version      varchar(64),
    overall_score         numeric(5, 2),
    score_level           varchar(32),
    strengths_json        text,
    weaknesses_json       text,
    risk_alerts_json      text,
    recommendations_json  text,
    competency_scores_json text,
    custom_metrics_json   text,
    raw_payload           text,
    evaluated_at          timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS candidate_ai_evaluations_candidate_idx
    ON public.candidate_ai_evaluations (job_candidate_id);

DROP TRIGGER IF EXISTS set_candidate_ai_evaluations_updated_at
    ON public.candidate_ai_evaluations;
CREATE TRIGGER set_candidate_ai_evaluations_updated_at
    BEFORE UPDATE ON public.candidate_ai_evaluations
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.enterprise_onboarding_sessions (
    user_id       uuid PRIMARY KEY,
    current_step  integer      NOT NULL,
    step1_data    text,
    step2_data    text,
    step3_data    text,
    records_data  text,
    expires_at    timestamptz  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS set_enterprise_onboarding_sessions_updated_at
    ON public.enterprise_onboarding_sessions;
CREATE TRIGGER set_enterprise_onboarding_sessions_updated_at
    BEFORE UPDATE ON public.enterprise_onboarding_sessions
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.verification_tokens (
    token_id        uuid PRIMARY KEY,
    target_user_id  uuid        NOT NULL,
    target_email    varchar(255) NOT NULL,
    code            varchar(16)  NOT NULL,
    channel         varchar(16)  NOT NULL,
    purpose         varchar(64)  NOT NULL,
    expires_at      timestamptz  NOT NULL,
    consumed        boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS verification_tokens_lookup_idx
    ON public.verification_tokens (target_user_id, purpose, code, consumed, created_at);

DROP TRIGGER IF EXISTS set_verification_tokens_updated_at
    ON public.verification_tokens;
CREATE TRIGGER set_verification_tokens_updated_at
    BEFORE UPDATE ON public.verification_tokens
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- ============================================================================
--  Core recruiting domain tables
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.jobs (
    id          uuid PRIMARY KEY,
    job_title   varchar(255) NOT NULL,
    description text,
    status      varchar(50)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS set_jobs_updated_at
    ON public.jobs;
CREATE TRIGGER set_jobs_updated_at
    BEFORE UPDATE ON public.jobs
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.candidates (
    id         uuid PRIMARY KEY,
    name       varchar(255) NOT NULL,
    email      varchar(255),
    phone      varchar(50),
    status     varchar(50)  NOT NULL DEFAULT 'CREATED',
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS candidates_email_idx
    ON public.candidates (email);

DROP TRIGGER IF EXISTS set_candidates_updated_at
    ON public.candidates;
CREATE TRIGGER set_candidates_updated_at
    BEFORE UPDATE ON public.candidates
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.interviews (
    id             uuid PRIMARY KEY,
    candidate_id   uuid REFERENCES public.candidates (id) ON DELETE SET NULL,
    job_id         uuid REFERENCES public.jobs (id) ON DELETE SET NULL,
    scheduled_time varchar(64),
    status         varchar(50) NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS interviews_candidate_idx
    ON public.interviews (candidate_id);
CREATE INDEX IF NOT EXISTS interviews_job_idx
    ON public.interviews (job_id);

DROP TRIGGER IF EXISTS set_interviews_updated_at
    ON public.interviews;
CREATE TRIGGER set_interviews_updated_at
    BEFORE UPDATE ON public.interviews
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE IF NOT EXISTS public.reports (
    report_id          uuid PRIMARY KEY,
    interview_id       uuid NOT NULL REFERENCES public.interviews (id) ON DELETE CASCADE,
    content            text,
    score              real,
    evaluator_comment  text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS reports_interview_idx
    ON public.reports (interview_id);

DROP TRIGGER IF EXISTS set_reports_updated_at
    ON public.reports;
CREATE TRIGGER set_reports_updated_at
    BEFORE UPDATE ON public.reports
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

COMMIT;
