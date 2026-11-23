# Plan: Step 1 — Backend

## Context

The repository holds only documentation. Step 1 delivers the Spring Boot REST API for users on an
H2 in-memory database, its tests and the CI workflow. Step 2 builds the Angular screen on the API
contract fixed here.

## Core contracts (locked up front)

1. **Endpoints.** One controller, `/api/users`:

   | Method | Path | Success | Errors |
   |---|---|---|---|
   | GET | `/api/users` | 200, array of users | - |
   | GET | `/api/users/{id}` | 200, user | 404 unknown id; 400 non-numeric id |
   | POST | `/api/users` | 201, user, `Location` of the new user | 400 invalid input; 409 duplicate email |
   | PUT | `/api/users/{id}` | 200, updated user | 400 invalid input; 404 unknown id; 409 stale version |
   | DELETE | `/api/users/{id}` | 204 | 404 unknown id |

2. **JSON.** User: `{"id": 1, "name": "Ivy Smith", "email": "ivy.smith@example.com", "version": 0}`.
   Create request: `name`, `email`. Update request: `name`, `version`. Any other property in a
   request body is rejected with 400 (D1); that includes `email` on update and `id` on create.

3. **Validation.** `name`: not blank, at most 255 characters. `email`: not blank, valid format
   (`@Email`), at most 255 characters. `version`: required on update. The 255 limit matches the
   column length, so a long value is a 400 and never a database error.

4. **Errors.** One `@RestControllerAdvice` returns problem details (`application/problem+json`).
   `detail` is an English sentence the UI shows as it is. No client mistake returns 500.

   | Case | Status | `detail` |
   |---|---|---|
   | Validation | 400 | the field errors, e.g. `email: must be a well-formed email address` |
   | Unknown property | 400 | names the property |
   | Malformed JSON, non-numeric id | 400 | Spring's default |
   | Unknown id | 404 | `User 999 not found` |
   | Duplicate email | 409 | `A user with this email already exists` |
   | Stale version | 409 | `The user was changed by someone else. Reload and try again.` |

5. **Email rule.** The email is set on create only. The update request has no `email`, an update
   that sends one is rejected (D1), and the entity has no email setter.

6. **Unique email.** A unique constraint on the column; the handler maps the constraint violation to
   409. No lookup before the insert: one code path, which also covers two concurrent creates.

7. **Optimistic locking.** A `@Version` column. An update whose `version` differs from the stored one
   returns 409 and changes nothing. A concurrent update that slips in between read and write fails at
   flush and returns the same 409. The update response carries the new version.

8. **Locale.** Case conversion uses `Locale.ROOT`; response messages are English on any machine (D4).

## Decisions (proposed)

### D1 — An update that contains `email`: reject with 400

A silently ignored field returns 200 and hides the client's mistake; rejecting it makes the rule
visible in the API. Done by configuration: Jackson fails on unknown properties, which also rejects
an `id` sent on create. Alternative: ignore the field (no code), rejected for the reason above.

### D2 — How the client sends the version: a `version` field in the PUT body

The client already holds the user JSON, which includes `version`; sending it back needs no header
handling on either side. Alternative: ETag and `If-Match`, more idiomatic HTTP but more code.

### D3 — Email case: stored in lower case, so uniqueness ignores case

`Ivy@Example.com` and `ivy@example.com` reach the same mailbox in practice, so they must not be two
users. The service converts on create with `toLowerCase(Locale.ROOT)`. Alternative: store as entered
with a case-sensitive constraint, which allows such duplicates.

### D4 — Response messages: English, whatever the machine's locale or the request's `Accept-Language`

Hibernate Validator ships Turkish messages, so on the development machine a request without
`Accept-Language` would get Turkish validation text, and section 2 of the agent instructions forbids
locale-dependent behaviour. Done by configuration: a fixed `en` locale.

## PRs

| PR | Scope | Acceptance criteria |
|---|---|---|
| **1/1** `[Step 1/1] Backend: user CRUD API and CI` | Maven project in `backend/` from start.spring.io (Spring Boot 4.2.0, Java 21, the starters of section 2, springdoc); generated extras removed (`spring-boot-h2console`, `HELP.md`, the context-loads test, unused test starters); the classes below; `application.properties` for D1 and D4; the tests below; `.github/workflows/ci.yml` running `./mvnw -B verify` on pull requests and pushes to `main`, skipped for doc-only changes | `./mvnw verify` passes on the development machine (Turkish locale) and in CI; every contract above has a test, seen failing once; E1–E16 pass when tried by hand |
| **1/2** `[Step 1/2] Backend: end-to-end run and docs` | Runs E1–E16 and records the result in this file; README: prerequisites, build, run, test, key decisions; ROADMAP status; `docs/ai-collaboration.md` | Every E step passes. A failure is fixed in a code PR before 1/2 |

Production code, in `backend/src/main/java/io/github/bdogru/usermanagement/`:

- `UserManagementApplication`
- `User`: entity, table `users` (`user` is reserved in H2); `id` generated by the database, `name`,
  `email` (unique), `version`
- `UserRepository`: `JpaRepository<User, Long>`
- `UserService`: transactions, email lower-casing, version check
- `UserController`: the five endpoints
- `CreateUserRequest`, `UpdateUserRequest`, `UserResponse`: records
- `ApiExceptionHandler`: extends `ResponseEntityExceptionHandler`
- `UserNotFoundException`

A stale version throws Spring's `ObjectOptimisticLockingFailureException`, the same exception a
flush-time conflict raises, so one handler method covers both and no second exception class is needed.

## Testing strategy (1/1)

One class, `UserControllerTest`: `@SpringBootTest` with MockMvc, through the controller, against the
real H2 database and its constraints.

| # | Test | Proves |
|---|---|---|
| T1 | With the JVM default locale set to `tr_TR` (restored afterwards), create `Ivy.Smith@Example.COM` → 201, `Location`, body with `ivy.smith@example.com` and version 0 | create; D3 with `Locale.ROOT` (a default-locale conversion gives `ıvy` under `tr_TR`, so the test fails on any machine, CI included) |
| T2 | List after creating two users → both returned | list |
| T3 | Create with blank name and invalid email, `Accept-Language: tr-TR` → 400, English detail naming both fields | validation; D4 |
| T4 | Create with an email differing only in case from an existing one → 409 | contract 6, caught by the constraint |
| T5 | Get, update and delete an unknown id → 404 | 404 handling |
| T6 | Update the name with the current version → 200, new name, version 1, email unchanged | update |
| T7 | Update with an `email` property → 400; a following GET returns the old email | **the email rule** |
| T8 | Update with a stale version → 409; a following GET returns the old name | contract 7 |
| T9 | Delete → 204; a following GET → 404 | delete |

Not tested: getters, framework defaults (malformed JSON, non-numeric id); E14 and E8 cover them by hand.

## End-to-end test plan (run in 1/2)

In Git Bash, on a fresh start: `cd backend && ./mvnw spring-boot:run`. In a second terminal:

```bash
B=http://localhost:8080/api/users
J='Content-Type: application/json'
```

| # | Command | Expected |
|---|---|---|
| E1 | `curl -i $B` | 200, `[]` |
| E2 | `curl -i -X POST $B -H "$J" -d '{"name":"Ivy Smith","email":"Ivy.Smith@Example.COM"}'` | 201, `Location` ending in `/api/users/1`, body `{"id":1,"name":"Ivy Smith","email":"ivy.smith@example.com","version":0}` |
| E3 | `curl -i -X POST $B -H "$J" -d '{"name":"","email":"not-an-email"}'` | 400, problem details, English detail naming `name` and `email` |
| E4 | E3 with `-H 'Accept-Language: tr-TR'` | the same English detail |
| E5 | `curl -i -X POST $B -H "$J" -d '{"name":"Bob","email":"IVY.SMITH@example.com"}'` | 409, `A user with this email already exists` |
| E6 | `curl -i -X POST $B -H "$J" -d '{"id":5,"name":"Bob","email":"bob@example.com"}'` | 400 naming `id`; `curl $B` still lists one user |
| E7 | `curl -i -X POST $B -H "$J" -d '{"name":"Bob","email":"bob@example.com"}'` | 201, id 3 (the insert rejected in E5 used up id 2) |
| E8 | `curl -i $B/1`, `curl -i $B/999`, `curl -i $B/abc` | 200 with Ivy; 404 `User 999 not found`; 400 |
| E9 | `curl -i -X PUT $B/1 -H "$J" -d '{"name":"Ivy Jones","version":0}'` | 200, name `Ivy Jones`, version 1, email unchanged |
| E10 | E9 again (still version 0) | 409 stale version; `curl $B/1` shows `Ivy Jones`, version 1 |
| E11 | `curl -i -X PUT $B/1 -H "$J" -d '{"name":"Ivy Brown","email":"new@example.com","version":1}'` | 400 naming `email`; `curl $B/1` shows `ivy.smith@example.com` and `Ivy Jones` |
| E12 | PUT `$B/1` with `{"name":"","version":1}`, then with `{"name":"Ivy Brown"}` | 400 each |
| E13 | `curl -i -X PUT $B/999 -H "$J" -d '{"name":"X","version":0}'` | 404 |
| E14 | `curl -i -X POST $B -H "$J" -d '{"name":'` | 400 |
| E15 | `curl -i -X DELETE $B/3`, the same again, then `curl $B` | 204; 404; only Ivy listed |
| E16 | `curl -s localhost:8080/v3/api-docs`, `curl -I localhost:8080/swagger-ui/index.html` | JSON with the five operations; 200 |

No step returns 500.

## Risks

- `mvnw` loses its executable bit when committed from Windows and CI fails; set it in the index.
- Spring Boot 4 renamed modules and properties. Every name is checked against the 4.2 reference.
- The development machine's locale is Turkish and CI's is not: the tests must pass in both.
