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
