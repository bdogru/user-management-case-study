# User Management

A small full-stack application for managing users: a Spring Boot REST API and a single-screen Angular
client. A user has a name, which can be changed, and an email address, which is fixed at creation.

## Status

Done: the backend (step 1) and the frontend (step 2). See [ROADMAP.md](ROADMAP.md) and [plan/](plan/);
how the AI agents worked together is in [docs/ai-collaboration.md](docs/ai-collaboration.md).

## How it was built

Claude Code wrote the code, Codex reviewed every PR, and the owner decided disputes and merged. The
instructions both agents followed are in [docs/](docs/).

Time: about two hours of the owner's active time over about two hours of elapsed time for the five
PRs, including about 30 minutes at the start to fix Codex's access to GitHub; plus about 10 minutes
beforehand to write the agent instructions.

## Prerequisites

- JDK 21. Maven is not needed: the wrapper downloads it.
- Node.js 24 (Angular 22 needs `^22.22.3` or `^24.15.0`).

## Run

Start the backend first, then the frontend, and open `http://localhost:4200`.

```bash
cd backend
./mvnw verify            # build and run the tests
./mvnw spring-boot:run   # start the API on http://localhost:8080
```

```bash
cd frontend
npm ci                   # install
npm run build            # production build into dist/
npm start                # dev server on http://localhost:4200, proxies /api to port 8080
```

On Windows outside Git Bash, use `mvnw.cmd` instead of `./mvnw`.

- API: `http://localhost:8080/api/users`
- OpenAPI document: `http://localhost:8080/v3/api-docs`, Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Database

H2 in memory. Nothing to install, and Hibernate creates the schema at startup. The data is lost when
the application stops.

## Key decisions

- **The email cannot change.** The update request has no `email`, and unknown JSON properties are
  rejected, so an update that sends one gets 400. In edit mode the screen shows the email read-only.
- **Concurrent edits.** Every user has a `version`. An update sends the version it read; a stale one
  gets 409 and changes nothing.
- **Unique email.** A unique constraint catches duplicates (409). Emails are stored in lower case, so
  the check ignores case.
- **Errors** are problem details (`application/problem+json`) with an English `detail`, whatever the
  server's locale. The screen shows that `detail` under the form.
- **No CORS.** The Angular dev server proxies `/api` to the backend, so the browser sees one origin.

## Possible improvements

- **Concurrent edits.** Optimistic locking already stops one person from overwriting another's
  change. On a 409 the screen could reload that user and show the current name, instead of asking
  for a page reload. `ETag` and `If-Match` headers would carry the version in standard HTTP.
- **Further tests.**
  - A browser test (for example Playwright) that automates B1–B10 of `plan/step-2.md`.
  - Component tests for edit mode, the read-only email and the error line.
  - A backend test that fires two updates at the same time, to cover the flush-time conflict, not only
    the explicit version check.
- **API documentation.** The OpenAPI document lists no 400, 404 or 409 responses yet.
- **One command to start.** Docker Compose, or serving the Angular build from Spring Boot.
- **A persistent database** (PostgreSQL with migrations) instead of H2 in memory.
