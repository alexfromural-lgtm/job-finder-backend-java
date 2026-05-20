-- =============================================================
-- V1__initial_schema.sql
-- Full DDL: all tables, indexes, constraints
-- Replaces Prisma migrations directory
-- =============================================================

-- Enable pgcrypto for gen_random_uuid() if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================
-- ENUMS
-- =============================================================

CREATE TYPE role AS ENUM ('JOB_SEEKER', 'RECRUITER', 'ADMIN');
CREATE TYPE application_status AS ENUM ('submitted', 'shortlisted', 'rejected', 'under_review');
CREATE TYPE notification_type AS ENUM ('application_update', 'system');
CREATE TYPE report_status AS ENUM ('open', 'reviewed', 'dismissed');

-- =============================================================
-- USERS
-- =============================================================

CREATE TABLE users (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- =============================================================
-- USER ROLES (join table for @ElementCollection)
-- =============================================================

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role    VARCHAR(20) NOT NULL,

    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================
-- JOB SEEKER PROFILES
-- =============================================================

CREATE TABLE job_seeker_profiles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    bio         TEXT,
    location    VARCHAR(255),
    education   VARCHAR(500),
    experience  TEXT,
    resume_url  VARCHAR(500),

    CONSTRAINT pk_job_seeker_profiles PRIMARY KEY (id),
    CONSTRAINT uq_job_seeker_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_job_seeker_profiles_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================
-- JOB SEEKER SKILLS (join table for @ElementCollection)
-- =============================================================

CREATE TABLE job_seeker_skills (
    profile_id  UUID        NOT NULL,
    skill       VARCHAR(100) NOT NULL,

    CONSTRAINT fk_job_seeker_skills_profile FOREIGN KEY (profile_id)
        REFERENCES job_seeker_profiles(id) ON DELETE CASCADE
);

-- =============================================================
-- RECRUITER PROFILES
-- =============================================================

CREATE TABLE recruiter_profiles (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL,
    company_name     VARCHAR(255) NOT NULL,
    company_website  VARCHAR(500),
    description      TEXT,
    industry         VARCHAR(100),

    CONSTRAINT pk_recruiter_profiles PRIMARY KEY (id),
    CONSTRAINT uq_recruiter_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_recruiter_profiles_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================
-- JOBS
-- =============================================================

CREATE TABLE jobs (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    recruiter_id UUID        NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT        NOT NULL,
    requirements TEXT        NOT NULL,
    location     VARCHAR(255) NOT NULL,
    salary_range VARCHAR(100),
    category     VARCHAR(100),
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_jobs PRIMARY KEY (id),
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_id)
        REFERENCES recruiter_profiles(id) ON DELETE CASCADE
);

-- Performance indexes matching Prisma @@index declarations
CREATE INDEX idx_job_is_active        ON jobs(is_active);
CREATE INDEX idx_job_is_active_cat    ON jobs(is_active, category);
CREATE INDEX idx_job_recruiter_ts     ON jobs(recruiter_id, created_at DESC);
CREATE INDEX idx_job_active_ts        ON jobs(is_active, created_at DESC);

-- =============================================================
-- APPLICATIONS
-- =============================================================

CREATE TABLE applications (
    id            UUID              NOT NULL DEFAULT gen_random_uuid(),
    job_id        UUID              NOT NULL,
    job_seeker_id UUID              NOT NULL,
    cover_letter  TEXT,
    status        application_status NOT NULL DEFAULT 'submitted',
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id)
        REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_seeker FOREIGN KEY (job_seeker_id)
        REFERENCES job_seeker_profiles(id) ON DELETE CASCADE
);

-- =============================================================
-- SAVED JOBS
-- =============================================================

CREATE TABLE saved_jobs (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    job_id        UUID        NOT NULL,
    job_seeker_id UUID        NOT NULL,
    saved_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_saved_jobs PRIMARY KEY (id),
    CONSTRAINT uq_saved_job_seeker UNIQUE (job_id, job_seeker_id),
    CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_id)
        REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_jobs_seeker FOREIGN KEY (job_seeker_id)
        REFERENCES job_seeker_profiles(id) ON DELETE CASCADE
);

-- =============================================================
-- NOTIFICATIONS
-- =============================================================

CREATE TABLE notifications (
    id         UUID              NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID              NOT NULL,
    type       notification_type NOT NULL,
    is_read    BOOLEAN           NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================
-- REPORTS
-- =============================================================

CREATE TABLE reports (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    reporter_id      UUID          NOT NULL,
    reported_user_id UUID,
    reported_job_id  UUID,
    reason           TEXT          NOT NULL,
    status           report_status NOT NULL DEFAULT 'open',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reported_user FOREIGN KEY (reported_user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_reported_job FOREIGN KEY (reported_job_id)
        REFERENCES jobs(id) ON DELETE SET NULL
);
