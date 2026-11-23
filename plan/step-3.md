# Plan: Step 3 — One-command start with Docker Compose

## Context

Steps 1 and 2 are merged. The brief lists "Docker Compose to start everything with one command" as an
optional extension, and the agent instructions need an owner decision before Docker Compose is added.
After the roadmap was done, the owner asked for it and made the decisions below.

## Core contracts (locked up front)

1. **One command.** `docker compose up --build` in the repository root builds and starts everything.
   Docker is the only prerequisite: both images build from source.
2. **Backend container.** Built with the Maven wrapper on `eclipse-temurin:21-jdk`, run on
   `eclipse-temurin:21-jre`. Port 8080: the API, the OpenAPI document and Swagger UI.
3. **Frontend container.** Built with Node 24, served by nginx. Port 4200. nginx forwards `/api` to the
   backend, as `proxy.conf.json` does in development, so the backend needs no change and no CORS.
4. **Development is unchanged.** `./mvnw spring-boot:run` and `npm start` work as before.

## Decisions (owner, 2026-11-23)

### D6 — Two containers, not Spring Boot serving the screen

Keeps the backend build independent of the frontend build and mirrors the development proxy.

### D7 — One PR

The Docker files, the end-to-end run and the documentation go into a single PR, 3/1.

## PR

| PR | Scope | Acceptance criteria |
|---|---|---|
| **3/1** `[Step 3/1] Docker Compose: one command to start` | `backend/Dockerfile` and `.dockerignore`; `frontend/Dockerfile`, `.dockerignore` and `nginx.conf`; `compose.yaml`; README, roadmap, this plan, `docs/ai-collaboration.md` | The run below passes from a clean state |

## End-to-end test plan

Docker running, nothing listening on ports 4200 and 8080, no images of this project built yet. From the
repository root:

| # | Action | Expected |
|---|---|---|
| D1 | `docker compose up --build` | Both images build; the backend logs `Started UserManagementApplication`; nginx starts |
| D2 | Open `http://localhost:4200` | The screen with an empty table |
| D3 | B2, B4, B6, B7, B9 and B10 of `plan/step-2.md`, with B9's `curl` against `localhost:8080` | As in step 2 |
| D4 | `curl -i -X POST localhost:4200/api/users -H 'Content-Type: application/json' -d '{"name":"Dee","email":"dee@example.com"}'` | 201, `Location: http://localhost:4200/api/users/…` (through nginx) |
| D5 | `curl -I localhost:8080/swagger-ui/index.html` | 200 |
| D6 | `docker compose down` | Both containers stop and are removed |

## End-to-end result

Run on 2026-11-23 on Windows 10 with Docker 29.7 and Compose 5.4, starting with no images of this project:
**D1–D6 pass**.

- D1: both images build from the clean checkout; the backend starts in 7.3 s.
- D3: in the desktop app's browser pane through nginx. The PUT body of B7 is
  `{"name":"Ivy Jones","version":0}`, without `email`.
- D4: `Location: http://localhost:4200/api/users/4`. Without `proxy_set_header Host` in `nginx.conf`,
  it would point at the internal `backend:8080`.

## Risks

- A Windows checkout could give `mvnw` CRLF line endings or no execute bit. `.gitattributes` stores it
  with LF, and the image runs it with `sh`, so neither matters.
- Local build output (`target/`, `node_modules/`, `dist/`) could leak stale files into the images;
  each `.dockerignore` excludes it.
- The image build skips tests; CI and `./mvnw verify` run them.
