# Blog API

A Java + Spring Boot mini-project, built step-by-step to learn the framework from the ground up.

## Tech Stack
- Java 17
- Spring Boot 3.3.4 (Web, Data JPA, Security — added incrementally)
- Gradle
- PostgreSQL (via Docker)
- Lombok

## Local Setup

**1. Start PostgreSQL:**
```bash
docker run --name blog-api-postgres \
  -e POSTGRES_DB=blogdb \
  -e POSTGRES_USER=bloguser \
  -e POSTGRES_PASSWORD=blogpassword \
  -p 5432:5432 \
  -d postgres:16-alpine
```
(If the container already exists from before: `docker start blog-api-postgres`)

**2. Run the app** in IntelliJ: open `BlogApiApplication.java` and click Run.

The app starts on `http://localhost:8080`.

## Progress Log

- [x] **Step 1** — Bootstrapped an empty Spring Boot app (Gradle, `spring-boot-starter-web`, runs cleanly on port 8080)
- [x] **Step 2** — `Post` entity + `PostStatus` enum + PostgreSQL wiring via Docker. Fields: `text`, `attachments` (list), `status` (DRAFT/PUBLISHED), `createdAt`, `updatedAt`, `createdBy`, `remarks`. Hibernate auto-creates `posts` + `post_attachments` tables (`ddl-auto: update` — fine for learning, would use Flyway/Liquibase in production).
- [x] **Step 3a** — `PostRepository` (Spring Data JPA interface, no hand-written SQL)
- [ ] **Step 3b** — Service layer (business logic)
- [ ] **Step 3c** — Controller (REST endpoints)
- [ ] **Step 4** — Validation + centralized exception handling
- [ ] **Step 5** — Spring Security (Basic Auth)
- [ ] **Step 6** — Postman testing + Phase 1 reflection

## API Endpoints
_(filled in as they're built)_

| Method | Path | Description |
|--------|------|-------------|
| — | — | — |

## Phase 1 Reflection
_(to fill in once Phase 1 — full CRUD — is complete)_
