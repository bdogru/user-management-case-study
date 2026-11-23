# User Management

A small full-stack application for managing users: a Spring Boot REST API and a single-screen Angular
client. A user has a name, which can be changed, and an email address, which is fixed at creation.

## Status

The backend (step 1) is done; the frontend (step 2) is next. See [ROADMAP.md](ROADMAP.md) and
[plan/](plan/).

## Prerequisites

- JDK 21. Maven is not needed: the wrapper downloads it.

## Backend

```bash
cd backend
./mvnw verify            # build and run the tests
./mvnw spring-boot:run   # start the API on http://localhost:8080
```

On Windows outside Git Bash, use `mvnw.cmd` instead of `./mvnw`.

- API: `http://localhost:8080/api/users`
- OpenAPI document: `http://localhost:8080/v3/api-docs`, Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Database

H2 in memory. Nothing to install, and Hibernate creates the schema at startup. The data is lost when
the application stops.

## Key decisions

- **The email cannot change.** The update request has no `email`, and unknown JSON properties are
  rejected, so an update that sends one gets 400.
- **Concurrent edits.** Every user has a `version`. An update sends the version it read; a stale one
  gets 409 and changes nothing.
- **Unique email.** A unique constraint catches duplicates (409). Emails are stored in lower case, so
  the check ignores case.
- **Errors** are problem details (`application/problem+json`) with an English `detail`, whatever the
  server's locale.
