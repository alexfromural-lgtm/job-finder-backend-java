# ⚡ Job Finder — Backend API (Spring Boot)

A role-based job board REST API built with **Java 21**, **Spring Boot 3.4**, **PostgreSQL**, **Spring Data JPA**, and **Redis** message queuing. Supports Job Seeker and Recruiter roles with **dual HTTP-only cookie auth** — both the short-lived access token and the long-lived refresh token are set as `HttpOnly; SameSite=Lax` cookies, so no token ever touches JavaScript. High-traffic write operations (apply to job, save job) are handled asynchronously via a Redis-backed queue with exponential-backoff retry.

> This is a full Java/Spring Boot port of [`job-finder-backend-customized`](../job-finder-backend-customized) (Node.js / Express / Prisma / Bull). All API endpoints, response shapes, cookie behaviour, and queue semantics are preserved.

---

## 🛠 Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 (LTS) | Runtime |
| Spring Boot 3.4 | Application framework |
| Spring Web MVC | HTTP server & routing (`@RestController`) |
| Spring Data JPA + Hibernate | Database ORM (replaces Prisma) |
| PostgreSQL | Relational database |
| Flyway | Database migrations (replaces Prisma migrate) |
| Spring Data Redis (Lettuce) | Redis client — message broker for async queue |
| Spring Security | Auth filter chain, CORS, role-based access |
| JJWT 0.12.6 | JWT access & refresh token signing/verification |
| BCryptPasswordEncoder | Password hashing (cost 10) |
| Jakarta Bean Validation | Request validation with `@Valid` (replaces Zod) |
| Bucket4j | In-memory per-IP rate limiting (replaces express-rate-limit) |
| Lombok | Boilerplate reduction (`@Data`, `@Builder`, `@Slf4j`) |
| Docker / Docker Compose | Containerised dev & production environment |

---

## 🏗 Project Structure

```
src/main/java/com/jobfinder/
├── JobFinderApplication.java            # @SpringBootApplication entry point
│
├── config/
│   ├── SecurityConfig.java              # Spring Security: CORS, cookie filter, public routes
│   ├── RedisConfig.java                 # Lettuce connection factory (parses redis:// URL)
│   ├── AsyncConfig.java                 # queueWorkerExecutor + taskExecutor thread pools
│   └── RateLimitConfig.java             # Bucket4j per-IP token buckets
│
├── domain/                              # JPA entities (replaces Prisma models)
│   ├── User.java                        # roles → @ElementCollection (user_roles table)
│   ├── JobSeekerProfile.java            # skills → @ElementCollection (job_seeker_skills table)
│   ├── RecruiterProfile.java
│   ├── Job.java                         # 4 performance indexes (matching Prisma @@index)
│   ├── Application.java
│   ├── SavedJob.java                    # unique constraint (job_id, job_seeker_id)
│   ├── Notification.java
│   └── Report.java
│
├── enums/
│   ├── Role.java                        # JOB_SEEKER, RECRUITER, ADMIN
│   ├── ApplicationStatus.java           # submitted, shortlisted, rejected, under_review
│   ├── NotificationType.java
│   └── ReportStatus.java
│
├── repository/                          # Spring Data JPA interfaces
│   ├── UserRepository.java              # findByEmail, existsByEmail
│   ├── JobSeekerProfileRepository.java  # findByUserId
│   ├── RecruiterProfileRepository.java  # findByUserId
│   ├── JobRepository.java               # @Query for paginated/filtered job search
│   ├── ApplicationRepository.java       # findByJobIdAndJobSeekerId, etc.
│   ├── SavedJobRepository.java
│   ├── NotificationRepository.java
│   └── ReportRepository.java
│
├── security/
│   ├── JwtService.java                  # sign/verify tokens, set/clear HTTP-only cookies
│   ├── CookieAuthFilter.java            # reads accessToken cookie → SecurityContext
│   └── UserDetailsServiceImpl.java      # loads UserDetails from DB by email
│
├── exception/
│   ├── AppException.java                # base: message + HttpStatus
│   ├── ResourceNotFoundException.java   # 404
│   ├── ConflictException.java           # 409
│   ├── ForbiddenException.java          # 403
│   ├── UnauthorizedException.java       # 401
│   └── GlobalExceptionHandler.java      # @ControllerAdvice — { error, fields? } shape
│
├── dto/
│   ├── request/                         # 9 request DTOs with @Valid annotations
│   └── response/                        # 6 response DTOs
│
├── service/
│   ├── AuthService.java                 # signup, login, refresh, upgrade, getCurrentUser
│   ├── JobService.java                  # CRUD + paginated filtered listing
│   ├── JobSeekerService.java            # profile, apply, applications, saved jobs
│   └── RecruiterService.java            # profile, applications for job, update status
│
├── queue/
│   ├── QueueJobPayload.java             # marker interface (discriminator: getType())
│   ├── ApplyToJobPayload.java
│   ├── SaveJobPayload.java
│   ├── QueueJobRecord.java              # Redis hash metadata per job
│   ├── DbWriteQueueService.java         # LPUSH enqueue + status polling
│   └── DbWriteWorker.java               # @Async BRPOP loops, retry + backoff
│
└── controller/
    ├── AuthController.java              # 7 endpoints
    ├── JobController.java               # 6 endpoints
    ├── JobSeekerController.java         # 8 endpoints (apply/save → 202 Accepted)
    ├── RecruiterController.java         # 4 endpoints
    └── QueueController.java             # GET /api/queue/job/{jobId}

src/main/resources/
├── application.yml                      # base config (port 5002, JPA, Flyway, Redis, JWT)
├── application-dev.yml                  # SQL logging + DEBUG levels
├── application-prod.yml                 # minimal logging
└── db/migration/
    ├── V1__initial_schema.sql           # full DDL — all tables, enums, indexes, constraints
    └── V2__seed_data.sql                # seed data with pre-computed BCrypt hashes
```

---

## 🔐 Roles & Permissions

| Role | Description |
|------|-------------|
| `JOB_SEEKER` | Browse jobs, apply, save jobs, manage profile |
| `RECRUITER` | Post/edit/delete jobs, view applicants, manage company profile |
| `ADMIN` | Seed-only; no admin routes exposed yet |

---

## 🚀 Setup & Running Locally

### Prerequisites
- Docker & Docker Compose (recommended)
- JDK 21 + Maven 3.9+ (only needed if running without Docker)

### 1. Configure environment

```bash
cp .env.sample .env
# Then edit .env with your actual values
```

**Key `.env` variables:**

```env
DB_NAME=job-finder
DB_USER=job_finder_user
DB_PASS=secure_password_123
ACCESS_TOKEN_SECRET=<generate a 64-char random string>
REFRESH_TOKEN_SECRET=<generate a 64-char random string>
REDIS_URL=redis://job-finder-redis:6379
QUEUE_CONCURRENCY=5
CORS_ORIGIN=http://localhost:3000
```

**Generate secrets:**

```bash
node -e "const c=require('crypto');console.log(c.randomBytes(32).toString('base64'));"
# or
openssl rand -base64 48
```

### 2. Start with Docker

```bash
docker-compose up --build
```

This starts:
- `job-finder-backend-java` on **port 5002** (Spring Boot, Java 21)
- `job-finder-db-java` (PostgreSQL 16) on **port 5432**
- `job-finder-redis-java` (Redis 7) on **port 6379**
- `job-finder-pgadmin-java` on **port 5050**

> **No manual migration step needed.** Flyway runs `V1__initial_schema.sql` and `V2__seed_data.sql` automatically on every startup. The schema is validated against JPA entities via `ddl-auto: validate`.

### 3. Seed accounts (created by V2 automatically)

| Email | Password | Role |
|-------|----------|------|
| `admin@example1.com` | `admin` | ADMIN |
| `recruiter@example.com` | `recruiter123` | RECRUITER |
| `seeker@example.com` | `seeker123` | JOB_SEEKER |

### 4. Build without Docker (local JDK)

```bash
mvn clean package -DskipTests
java -jar target/job-finder-backend-1.0.0.jar
```

### 5. Running with GraalVM Native Image (AOT Compilation)

For production deployment or extremely fast container restart times, you can compile the Java application Ahead-of-Time (AOT) to a standalone native binary using **GraalVM Native Image**.

* **Benefits**: Startup/restart time is reduced to milliseconds (typically ~0.08s), and runtime memory footprint is extremely low since no traditional JVM is loaded.
* **Trade-off**: The build process is slow and memory-intensive (takes 3–8 minutes on the first run and requires significant RAM allocation in Docker).

To build and run the native stack:

1. **Ensure the shared network exists:**
   ```bash
   docker network create job-finder-network
   ```

2. **Start the native stack:**
   ```bash
   docker compose -f docker-compose.native.yml up --build
   ```

3. **Verify instant restarts:**
   ```bash
   docker compose -f docker-compose.native.yml restart backend
   ```
   Check the container logs to observe the startup time measured in milliseconds!

---

## 🧪 API Reference

Base URL: `http://localhost:5002/api`

> **Response shape:** Successful responses return the payload directly. Error responses always return `{ "error": "<message>" }`. Validation errors return `{ "error": "Validation failed", "fields": { "<field>": "<message>" } }`.

---

### Auth — `/api/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/signup/jobseeker` | — | Register as Job Seeker |
| `POST` | `/signup/recruiter` | — | Register as Recruiter (includes company fields) |
| `POST` | `/login` | — | Login; sets `accessToken` + `refreshToken` HTTP-only cookies |
| `POST` | `/logout` | — | Clears both `accessToken` and `refreshToken` cookies |
| `POST` | `/refresh` | Cookie | Rotates both tokens; sets fresh cookies |
| `GET`  | `/me` | Cookie (`accessToken`) | Get current authenticated user |
| `POST` | `/upgrade/recruiter` | Cookie (JOB_SEEKER) | Upgrade Job Seeker account to Recruiter |

**Signup — Job Seeker** `POST /api/auth/signup/jobseeker`
```json
{ "name": "John Doe", "email": "john@example.com", "password": "secret123" }
```

**Signup — Recruiter** `POST /api/auth/signup/recruiter`
```json
{
  "name": "Jane Smith",
  "email": "jane@corp.com",
  "password": "secret123",
  "companyName": "Acme Corp",
  "companyWebsite": "https://acme.com",
  "industry": "Technology",
  "description": "We build cool stuff."
}
```

**Login** `POST /api/auth/login`
```json
{ "email": "john@example.com", "password": "secret123" }
```
Response: `{ "userId": "...", "roles": ["JOB_SEEKER"] }` + two cookies:
- `Set-Cookie: accessToken=<JWT>; HttpOnly; SameSite=Lax` (15 min)
- `Set-Cookie: refreshToken=<JWT>; HttpOnly; SameSite=Lax` (7 days)

> The `accessToken` is **never returned in the response body** — it is inaccessible to JavaScript, eliminating XSS-based token theft.
> Deactivated accounts (`isActive: false`) are rejected with `403 Forbidden`.

**Refresh** `POST /api/auth/refresh` _(requires `refreshToken` HTTP-only cookie)_

Response: `{ "userId": "...", "roles": [...] }` + two refreshed cookies.
> Rotates both tokens. Called automatically by the frontend interceptor on `401` responses.

**Get Me** `GET /api/auth/me` _(requires `accessToken` HTTP-only cookie)_
```json
{
  "id": "uuid",
  "name": "John Doe",
  "email": "john@example.com",
  "roles": ["JOB_SEEKER"],
  "isActive": true,
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

**Upgrade to Recruiter** `POST /api/auth/upgrade/recruiter` _(requires `JOB_SEEKER` cookie)_
- Body: same shape as recruiter signup.
- Returns `409 Conflict` if the user already has a recruiter profile.
- Re-issues fresh tokens immediately so the new `RECRUITER` role is active without a separate login.

---

### Jobs — `/api/jobs`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/all` | — | List all active jobs (supports `?search=`, `?category=`, `?location=`, `?page=`, `?pageSize=`) |
| `GET` | `/:id` | — | Get a single job by ID |
| `GET` | `/recruiter` | Cookie (RECRUITER) | Get jobs posted by the authenticated recruiter |
| `POST` | `/` | Cookie (RECRUITER) | Create a new job posting |
| `PUT` | `/:id` | Cookie (RECRUITER) | Update a job (owner only) |
| `DELETE` | `/:id` | Cookie (RECRUITER) | Delete a job (owner only) |

**`?search=`** matches against `title`, `description`, and `requirements` (case-insensitive `ILIKE`).

**Get All Jobs** `GET /api/jobs/all` response:
```json
{
  "jobs": [ { "id": "...", "title": "...", "companyName": "...", ... } ],
  "total": 42,
  "page": 1,
  "pageSize": 10,
  "totalPages": 5
}
```

**Create / Update Job body:**
```json
{
  "title": "Senior React Developer",
  "description": "We are looking for...",
  "requirements": "5+ years React experience",
  "location": "Remote",
  "salaryRange": "$120,000 – $160,000",
  "category": "Engineering"
}
```

---

### Job Seeker — `/api/jobseeker`

All endpoints require an `accessToken` HTTP-only cookie and role `JOB_SEEKER`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/profile` | Get own Job Seeker profile |
| `PATCH` | `/profile` | Update profile (all fields optional) |
| `POST` | `/apply/:jobId` | **Enqueue** an application — returns `202 Accepted` + `jobId` |
| `GET` | `/applications` | List own applications |
| `DELETE` | `/applications/:id` | Withdraw an application |
| `POST` | `/saved/:jobId` | **Enqueue** a save-job write — returns `202 Accepted` + `jobId` |
| `GET` | `/saved` | List saved jobs |
| `DELETE` | `/saved/:jobId` | Remove a saved job |

> **Async writes:** `POST /apply/:jobId` and `POST /saved/:jobId` immediately enqueue the work via Redis and return `202 Accepted`. The client must poll `GET /api/queue/job/:jobId` to track completion.

**Profile update body** (`PATCH /profile`) — all fields optional:
```json
{
  "bio": "Full-stack developer with 5 years experience.",
  "location": "San Francisco, CA",
  "skills": ["TypeScript", "React", "Node.js"],
  "education": "B.Sc. Computer Science",
  "experience": "Senior Engineer at Acme Corp (2022–present)",
  "resumeUrl": "https://example.com/resume.pdf"
}
```

**Apply / Save response (202):**
```json
{ "jobId": "<queue-job-id>", "message": "Application queued for processing" }
```

---

### Recruiter — `/api/recruiter`

All endpoints require an `accessToken` HTTP-only cookie and role `RECRUITER`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/profile` | Get own Recruiter/company profile |
| `PATCH` | `/profile` | Update company profile |
| `GET` | `/jobs/:jobId/applications` | List applicants for a specific job |
| `PATCH` | `/applications/:id/status` | Update application status |

**Application statuses:** `submitted` · `shortlisted` · `under_review` · `rejected`

**Update status body** (`PATCH /applications/:id/status`):
```json
{ "status": "shortlisted" }
```

---

### Queue — `/api/queue`

Public polling endpoint — no auth required.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/job/:jobId` | Poll the status of a queued write operation |

**Response:**
```json
{
  "id": "<queue-job-id>",
  "type": "apply-to-job",
  "status": "completed",
  "attemptsMade": 1,
  "createdAt": "2026-05-11T18:00:00Z",
  "result": { /* Application object on completion */ },
  "failedReason": null
}
```

**Possible statuses:** `waiting` · `active` · `completed` · `failed`

On `completed`, the `result` field contains the Application (or SavedJob) object.
On `failed`, the `failedReason` field contains the error message.

---

## 🔁 Async Queue Architecture

The Redis-backed queue replaces Bull with the same semantics:

| Concept | Bull (Node.js) | Spring (Java) |
|---------|---------------|---------------|
| Queue storage | Redis LIST via Bull | Redis LIST (`db-write-queue`) |
| Job metadata | Bull job hash | Redis hash (`queue-meta:{jobId}`) |
| Concurrency | `queue.process(N, handler)` | N `@Async` BRPOP threads on `queueWorkerExecutor` |
| Retry | `attempts: 3, backoff: { type: "exponential", delay: 500 }` | 3 attempts, 500ms → 1s → 2s exponential backoff |
| Status polling | `GET /api/queue/job/:id` (Bull job status) | `GET /api/queue/job/:id` (same response shape) |

The `DbWriteWorker` starts N concurrent threads (configured by `QUEUE_CONCURRENCY`, default 5) each running a `BRPOP` loop with a 2-second timeout — non-CPU-spinning and production-safe.

---

## 🛡 Global Error Handling

`GlobalExceptionHandler` (`@ControllerAdvice`) is the single point of truth for all error responses:

| Condition | HTTP Status | Response |
|-----------|-------------|----------|
| `AppException` subclasses | `err.status` | `{ "error": err.message }` |
| `@Valid` failure | `400` | `{ "error": "Validation failed", "fields": { ... } }` |
| `AccessDeniedException` | `403` | `{ "error": "Forbidden" }` |
| `AuthenticationException` | `401` | `{ "error": "Unauthorized" }` |
| All other exceptions | `500` | `{ "error": "Internal server error" }` (detail logged) |

Throw a typed exception from any service:
```java
throw new ResourceNotFoundException("Job not found");   // 404
throw new ConflictException("Email already in use");    // 409
throw new ForbiddenException("Not your job");           // 403
throw new UnauthorizedException("Token expired");       // 401
```

---

## 🔒 Security & Performance

### Security filter chain

| Component | Purpose |
|-----------|---------|
| `CookieAuthFilter` | Reads `accessToken` cookie per request, verifies JWT, populates `SecurityContext` |
| `SecurityConfig` | Stateless JWT (no session), CSRF disabled, CORS configured, public routes declared |
| `@PreAuthorize` | Fine-grained role guards on controller methods (`hasRole('RECRUITER')`, etc.) |

### Cookie security

Both `accessToken` and `refreshToken` are set as `HttpOnly; SameSite=Lax` cookies.

| Attribute | Value | Why |
|-----------|-------|-----|
| `HttpOnly` | `true` | Cookie invisible to JavaScript — XSS cannot steal the token |
| `Path` | `/` | Cookie sent on all API requests |
| `MaxAge` | 900s (access) / 604800s (refresh) | Matches 15m / 7d JWT expiry |

**Why `Lax` and not `Strict`?**

`SameSite=Strict` blocks the cookie on every cross-site navigation, including top-level `GET` requests (e.g. clicking a link from email). `SameSite=Lax` allows cookies on top-level `GET` navigations while **blocking them on cross-site state-mutating requests** (`POST`, `PUT`, `PATCH`, `DELETE`) — which are the only requests that matter for CSRF.

> **In short:** `Lax` = "let me in through the front door, but never submit a form on my behalf from another site."

### Rate limiting (Bucket4j — in-memory, per client IP)

| Limiter | Routes | Window | Max requests | Purpose |
|---------|--------|--------|-------------|---------|
| `loginBucket` | `POST /login`, `POST /refresh` | 15 min | 10 | Prevents brute-force & token-refresh flooding |
| `signupBucket` | `POST /signup/jobseeker`, `POST /signup/recruiter` | 1 hour | 5 | Prevents automated account creation spam |

On limit breach, the API returns `429 Too Many Requests` with `{ "error": "Too many requests. Try again later." }`.

### sameSite decision matrix for production deployments

| Deployment topology | `sameSite` needed | `secure` required | Notes |
|--------------------|-------------------|-------------------|-------|
| Same domain, reverse-proxy path routing (`/api/*` → backend) | `Lax` ✅ | Yes (HTTPS) | Current setting works as-is |
| Different subdomains (`app.example.com` vs `api.example.com`) | `None` ⚠️ | **Must be `true`** | Browsers treat subdomains as cross-site |
| Completely different domains | `None` ⚠️ | **Must be `true`** | Same as above |

If frontend and backend are ever split onto separate subdomains, update `JwtService.buildCookie()`:
```java
cookie.setSecure(true);
// Add SameSite=None via response header — Servlet API doesn't expose it natively:
// response.addHeader("Set-Cookie", "accessToken=...; SameSite=None; Secure; HttpOnly; Path=/");
```

---

## 🗄 Database

### Flyway migrations (auto-run on startup)

| File | Contents |
|------|---------|
| `V1__initial_schema.sql` | Full DDL: enums, all 9 tables, indexes, foreign key constraints |
| `V2__seed_data.sql` | Demo users with pre-hashed BCrypt passwords, recruiter profile, job, application, saved job |

Flyway runs migrations automatically. To reset:
```bash
docker-compose down -v        # drops the pgdata volume
docker-compose up --build     # Flyway re-runs V1 + V2 from scratch
```

### Useful psql commands

```bash
# Connect from PowerShell
docker exec -it job-finder-db psql -U job_finder_user -d job-finder -W
# Password: secure_password_123

# Inside the container
psql -U job_finder_user -d job-finder
```

### pgAdmin4

- URL: `http://localhost:5050`
- Login: `admin@example.com` / `admin`

### Check if port 5432 is in use (Windows)

```bash
netstat -ano | findstr :5432

Get-Process -Id (Get-NetTCPConnection -LocalPort 5432).OwningProcess | Stop-Process -Force
```

---

## 🔗 Frontend

The React frontend lives at [`../job-finder-react-customized`](../job-finder-react-customized) and proxies all `/api` calls to this service on port **5002**. No frontend changes are needed — the API shape and cookie behaviour are identical to the Node.js version.

---

## 🔧 Upgrading Services

### PostgreSQL — Major Version Upgrade

> ⚠️ **PostgreSQL major versions (e.g. 15 → 16) are NOT backward compatible.**
> The on-disk data format changes between major releases.

#### Symptoms
```
FATAL:  database files are incompatible with server
DETAIL: The data directory was initialized by PostgreSQL version 15, which is
        not compatible with this version 16.x
```

#### Upgrade procedure (development / disposable data)

1. **Edit `docker-compose.yml`** — bump the image tag:
   ```yaml
   image: postgres:17   # was postgres:16
   ```

2. **Stop containers AND delete the named volume:**
   ```bash
   docker-compose down -v
   ```
   > The `-v` flag removes `pgdata`. **All existing data will be lost.**

3. **Bring everything back up:**
   ```bash
   docker-compose up --build
   ```
   Flyway re-runs `V1` + `V2` automatically on the fresh volume.

#### Upgrade procedure (production / data must be preserved)

```bash
# 1. Dump from the running PG container
docker exec job-finder-db pg_dump -U $DB_USER -d $DB_NAME -F c -f /tmp/backup.dump

# 2. Copy to host
docker cp job-finder-db:/tmp/backup.dump ./backup.dump

# 3. Stop and drop the old volume
docker-compose down -v

# 4. Bump image tag in docker-compose.yml, then start only postgres
docker-compose up -d postgres

# 5. Restore once healthy
docker cp ./backup.dump job-finder-db:/tmp/
docker exec job-finder-db pg_restore -U $DB_USER -d $DB_NAME /tmp/backup.dump

# 6. Start remaining services
docker-compose up -d
```

#### Healthcheck dependency (already configured)

The backend waits until PostgreSQL is fully ready before starting:

```yaml
backend:
  depends_on:
    postgres:
      condition: service_healthy   # waits for pg_isready to pass
```

---

### Redis — Minor / Patch Version Upgrade

Redis minor/patch upgrades are data-compatible; no volume deletion needed.

1. Edit `docker-compose.yml`:
   ```yaml
   image: redis:8-alpine   # was redis:7-alpine
   ```
2. Pull and restart:
   ```bash
   docker-compose pull redis
   docker-compose up -d redis
   ```

> For Redis **major** version upgrades, check release notes for breaking changes. Queue jobs stored as Redis hashes are generally forwards-compatible.

---

### Java Backend — Runtime Version Upgrade

The runtime is pinned in `Dockerfile` and `pom.xml`:

**`Dockerfile`:**
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder   # bump: 21 → 25
FROM eclipse-temurin:21-jre-alpine                    # bump: 21 → 25
```

**`pom.xml`:**
```xml
<java.version>21</java.version>   <!-- bump to 25 -->
```

Steps:
1. Update both files above.
2. Rebuild:
   ```bash
   docker-compose up --build backend
   ```
3. Verify:
   ```bash
   docker exec job-finder-backend java -version
   ```

---

### pgAdmin4 — Version Upgrade

```bash
# Edit docker-compose.yml
image: dpage/pgadmin4:9   # pin to a specific version

# Restart the service
docker-compose up -d pgadmin
```

---

### General Upgrade Checklist

- [ ] Read the release notes / CHANGELOG for **breaking changes**
- [ ] Determine if the data volume is **version-compatible**
- [ ] Back up data if the volume will be deleted
- [ ] Update the image tag in `docker-compose.yml` (and `pom.xml` / `Dockerfile` for Java)
- [ ] Run `docker-compose pull <service>` to fetch the new image
- [ ] Run `docker-compose up -d <service>` (or `--build` for the Java backend)
- [ ] Check container logs: `docker-compose logs -f <service>`
- [ ] Run integration tests / smoke-test the API
- [ ] Commit the changes with the version bump noted in the commit message
