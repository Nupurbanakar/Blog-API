# Blog API

A Java + Spring Boot mini-project: building service for blog posts CRUD.

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

## API Endpoints

| Method | Path | Auth Required | Description |
|--------|------|----------------|-------------|
| POST | /api/auth/login | No | Exchange username/password for access + refresh tokens |
| POST | /api/auth/refresh | No | Exchange a valid refresh token for a new access token |
| POST | /api/posts | Bearer token | Create a post |
| GET | /api/posts | Bearer token | List visible posts |
| GET | /api/posts/{id} | Bearer token | Get one post |
| PUT | /api/posts/{id} | Bearer token | Update a post (own drafts only, unless admin) |
| DELETE | /api/posts/{id} | Bearer token | Delete a post (own drafts only, unless admin) |
| PATCH | /api/posts/{id}/publish | Bearer token, ADMIN | Publish a draft |
| PATCH | /api/posts/{id}/reject | Bearer token, ADMIN | Send a post back to draft with remarks |
