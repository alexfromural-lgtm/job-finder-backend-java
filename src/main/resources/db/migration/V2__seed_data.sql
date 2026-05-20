-- =============================================================
-- V2__seed_data.sql
-- Ported from prisma/seed.ts
-- Passwords are pre-hashed with BCrypt cost 10
-- =============================================================

-- =============================================================
-- ADMIN USER
-- password: "admin"
-- =============================================================

INSERT INTO users (id, name, email, password, is_active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Admin User',
    'admin@example1.com',
    '$2b$10$hjnpZPHs6i043InaLIZAbeYQ6L6VOSKJW3vhFaxvzVA8uxoUTufNm',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN');

-- =============================================================
-- RECRUITER USER + PROFILE + JOB
-- password: "recruiter123"
-- =============================================================

INSERT INTO users (id, name, email, password, is_active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Recruiter Jane',
    'recruiter@example.com',
    '$2b$10$EuObiGEbOpiAPHeik7iVjuTjv9V1cLPSRU8Kff8rGw2gwWQ4P.qDO',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role) VALUES
    ('00000000-0000-0000-0000-000000000002', 'RECRUITER');

INSERT INTO recruiter_profiles (id, user_id, company_name, company_website, description, industry)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000002',
    'Tech Corp',
    'https://techcorp.com',
    'A fast-growing tech company',
    'Software'
);

INSERT INTO jobs (id, recruiter_id, title, description, requirements, location, salary_range, category, is_active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000010',
    'Senior FullStack Developer',
    'React developer needed',
    '8+ years of experience in React and Node.js',
    'Remote',
    '$130,000 - $180,000',
    'Software',
    TRUE,
    NOW(),
    NOW()
);

-- =============================================================
-- JOB SEEKER USER + PROFILE + APPLICATION + SAVED JOB
-- password: "seeker123"
-- =============================================================

INSERT INTO users (id, name, email, password, is_active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'Job Seeker John',
    'seeker@example.com',
    '$2b$10$3Wlt.7AfpHqJzFN0CmkE6egYRfTMCsKPhdIYSfQc/Nia6l7EeI5ve',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role) VALUES
    ('00000000-0000-0000-0000-000000000003', 'JOB_SEEKER');

INSERT INTO job_seeker_profiles (id, user_id, bio, location, education, experience, resume_url)
VALUES (
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000003',
    'Passionate about frontend development',
    'Sydney',
    'BSc in Computer Science',
    '2 years at Webify',
    'https://example.com/resume/john.pdf'
);

INSERT INTO job_seeker_skills (profile_id, skill) VALUES
    ('00000000-0000-0000-0000-000000000011', 'React'),
    ('00000000-0000-0000-0000-000000000011', 'TypeScript'),
    ('00000000-0000-0000-0000-000000000011', 'HTML'),
    ('00000000-0000-0000-0000-000000000011', 'CSS');

INSERT INTO applications (id, job_id, job_seeker_id, cover_letter, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000030',
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000011',
    'I''m very interested in this opportunity!',
    'submitted',
    NOW(),
    NOW()
);

INSERT INTO saved_jobs (id, job_id, job_seeker_id, saved_at)
VALUES (
    '00000000-0000-0000-0000-000000000040',
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000011',
    NOW()
);
