# Blog API

A Java + Spring Boot mini-project: building service for blog posts CRUD.

## Tech Stack
- Java 17
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Security)
- Gradle
- PostgreSQL (via Docker, for local dev)
- H2 (in-memory, test-only)
- Lombok
- JWT (`jjwt`) — access + refresh tokens
- Cloudinary — attachment storage
- JUnit 5 + Mockito + AssertJ + MockMvc — testing
- GitHub Actions — CI

## Local Setup

## Prerequisites

- IntelliJ IDEA
- JDK 17
- Docker Desktop (for local Postgres)
- Postman
- A free [Cloudinary](https://cloudinary.com/users/register/free) account

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

## Environment Variables (required)

The app will not start without these set. In IntelliJ: **Run → Edit Configurations → Environment Variables**.

JWT_SECRET= <random-string>
ADMIN_USERNAME= admin
ADMIN_PASSWORD= <your-choice>
USER_USERNAME= user
USER_PASSWORD= <your-choice>
CLOUDINARY_CLOUD_NAME= <from-your-Cloudinary-dashboard>
CLOUDINARY_API_KEY= <from-your-Cloudinary-dashboard>
CLOUDINARY_API_SECRET= <from-your-Cloudinary-dashboard>

**2. Set the environment variables** above in your Run Configuration.

**3. Run the app** in IntelliJ: open `BlogApiApplication.java` and click Run. Starts on `http://localhost:8080`.

## Authentication & Authorization

Uses **JWT**, not Basic Auth. Log in via `/api/auth/login` to get an access + refresh token, then send `Authorization: Bearer <accessToken>` on subsequent requests. Access tokens expire in 1 hour; use `/api/auth/refresh` to get a new one without re-entering a password.

Two roles:
- **USER** : can create posts (always start as `DRAFT`), and edit/delete their *own* drafts only. Sees all `PUBLISHED` posts plus their own drafts (other users' drafts are invisible, returned as 404).
- **ADMIN** : can view everything, publish/reject any draft, and edit/delete any post.

## API Endpoints

| Method | Path | Auth Required | Description |
|--------|------|----------------|-------------|
| POST | /api/auth/login | No | Exchange username/password for access + refresh tokens |
| POST | /api/auth/refresh | No | Exchange a valid refresh token for a new access token |
| POST | /api/posts | Bearer token | Create a post. **`multipart/form-data`**: fields `text` (required), `remarks` (optional), `files` (optional, one or more) |
| GET | /api/posts | Bearer token | List visible posts |
| GET | /api/posts/{id} | Bearer token | Get one post |
| PUT | /api/posts/{id} | Bearer token | Update a post (own drafts only, unless admin) — JSON body |
| DELETE | /api/posts/{id} | Bearer token | Delete a post (own drafts only, unless admin) |
| PATCH | /api/posts/{id}/publish | Bearer token, ADMIN | Publish a draft |
| PATCH | /api/posts/{id}/reject | Bearer token, ADMIN | Send a post back to draft with remarks |

## Attachments

Create Post accepts file uploads directly (`multipart/form-data`), uploaded to Cloudinary. If an attachment fails to upload, the post is still created — check the `attachmentUploadErrors` field in the response for details on any that failed.

## CI

Every push to `main` triggers GitHub Actions (`.github/workflows/ci.yml`) to build and run the full test suite on a clean machine. See the **Actions** tab on GitHub.
