# Learning Progress Tracker

A small full-stack app for a training programme. A **coordinator** creates learning activities, adds
candidates (who receive one-time credentials), assigns training and watches cohort progress. A
**candidate** signs in, sets their own password on first login, and sees and updates only their own training.

It implements the eight functional requirements (FR-1 to FR-8) of the capstone brief, track A (Java).
All names and data are fictional.

| Layer | Technology |
| --- | --- |
| Frontend | React 18 (plain JavaScript), Vite, React Router, Axios, plain CSS |
| Backend | Java 17+, Spring Boot 3.3, Spring Web, Spring Security, Spring Data JPA, Bean Validation |
| Database | MySQL 8 (schema and fictional seed data in `schema.sql` / `data.sql`) |
| Tests | JUnit 5, Mockito, MockMvc (H2 in-memory) · Vitest and React Testing Library |

## Prerequisites

- **JDK 17 or newer** (21 LTS recommended), check with `java -version`
- **Maven 3.9+**, check with `mvn -v`
- **Node.js 18+** and npm, check with `node -v`
- **MySQL 8.0.16+** (8.0.16 is the first version that enforces `CHECK` constraints)

## Project layout

```
learning-progress-tracker/
├── backend/                 Spring Boot API
│   ├── pom.xml
│   ├── .env.example         copy to .env and fill in
│   └── src/
│       ├── main/java/com/example/lpt/
│       │   ├── controller/  HTTP endpoints (thin)
│       │   ├── service/     business rules
│       │   ├── repository/  Spring Data JPA
│       │   ├── entity/      Account, Learner, Activity, Assignment
│       │   ├── dto/         request/response shapes (no password hashes)
│       │   ├── security/    session handling, rate limiter, CSRF cookie, first-login guard
│       │   ├── config/      SecurityConfig, AppConfig, CoordinatorBootstrap
│       │   └── exception/   ApiException + one GlobalExceptionHandler
│       ├── main/resources/  application.properties, schema.sql, data.sql
│       └── test/            MockMvc, Mockito and unit tests
├── frontend/                React app (Vite)
│   └── src/ api/ auth/ components/ pages/ styles/ __tests__/
└── docs/                    DEMO.md, CHECKLIST.md
```

## Configuration

The backend reads its settings from environment variables. For local use, put them in **`backend/.env`**
(plain `KEY=value` lines, no quotes, no trailing spaces). Spring Boot imports that file automatically.
`.env` is git-ignored; never commit it. `backend/.env.example` is the template.

| Variable | Required | Meaning |
| --- | --- | --- |
| `DB_URL` | no | JDBC URL. Default `jdbc:mysql://localhost:3306/learning_tracker?serverTimezone=UTC` |
| `DB_USERNAME` | no | Default `lpt_user` |
| `DB_PASSWORD` | **yes** | Password of the MySQL user (the app refuses to start without it) |
| `APP_COORDINATOR_EMAIL` | first run | Email of the initial coordinator |
| `APP_COORDINATOR_PASSWORD` | first run | Password of the initial coordinator, **at least 12 characters** |
| `SESSION_COOKIE_SECURE` | no | `false` on localhost; set `true` when served over HTTPS |

Other settings live in `backend/src/main/resources/application.properties`:
session timeout (30 minutes), login limit (5 failed attempts per email in 15 minutes), temporary
password lifetime (24 hours).

The frontend needs no configuration. In development, Vite proxies `/api` to `http://localhost:8080`
(change it with the `VITE_API_TARGET` environment variable if your backend runs elsewhere).

## MySQL setup

Run once as the MySQL `root` user (MySQL Workbench or the `mysql` command line):

```sql
CREATE DATABASE IF NOT EXISTS learning_tracker CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'lpt_user'@'localhost' IDENTIFIED BY 'LptDev2026x';
GRANT ALL PRIVILEGES ON learning_tracker.* TO 'lpt_user'@'localhost';
FLUSH PRIVILEGES;
```

Use your own password and put the same one in `DB_PASSWORD`. **You do not create any tables:** the backend
runs `schema.sql` (tables and constraints) and `data.sql` (fictional seed data) on every start. Both are
idempotent, so restarts never duplicate anything.

## Initial coordinator

There is no public registration and no hard-coded account. On the first start, if no coordinator exists,
the backend creates one from `APP_COORDINATOR_EMAIL` and `APP_COORDINATOR_PASSWORD`
(the log shows `Created initial coordinator account for ...`; the password is never logged).
After that, the coordinator adds candidates from the **Learners** page.

## Run

Backend (terminal 1):

```bash
cd backend
cp .env.example .env        # Windows PowerShell: copy .env.example .env   then edit .env
mvn spring-boot:run         # http://localhost:8080
```

Frontend (terminal 2):

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173  (open this one)
```

Sign in as the coordinator with the email and password from your `.env`.

**Seed data** (loaded automatically by `data.sql` on every start, without ever duplicating anything):
8 fictional learners in two cohorts (Jordan Lee, Priya Nair, Sam Okafor, Aisha Khan, Mateo Silva, Hana Sato,
Liam Murphy, Zara Ahmed), 10 activities and 20 assignments with mixed statuses, so the dashboard has something to show.
The seeded learners have an unusable placeholder password, so nobody can sign in as them until the coordinator
opens the learner and clicks **Reset password** (this is also part of the demo).

## Test

```bash
# backend: unit tests (Mockito) + API tests (MockMvc, real security chain, in-memory H2). No MySQL needed.
cd backend
mvn test

# one class or one method
mvn -Dtest=WorkflowTest test
mvn -Dtest=AuthApiTest#logoutEndsTheSession test

# frontend
cd frontend
npm test
```

What the backend tests cover: required fields, hours greater than zero, duplicate assignment (409),
duplicate email ignoring case (409), invalid and expired credentials, first-login enforcement, logout and
session expiry, coordinator-only endpoints, cross-candidate access denial, password reset ending sessions,
login rate limiting, CSRF, and one full workflow (create candidate, first login, change password, assign,
update status, summary). Authentication dependencies (password encoder, repositories, session manager)
are mocked in the service unit tests.

## API

All routes are under `/api/v1`. JSON errors look like `{"message": "Assignment already exists."}`
(validation errors also carry `"errors": {"field": "message"}` so the UI can show them inline).

| Method and route | Who | Success |
| --- | --- | --- |
| `POST /auth/login` `{email,password}` | public, rate-limited | 200 `{role, mustChangePassword}`, sets session cookie |
| `POST /auth/change-password` `{currentPassword,newPassword}` | signed in | 204 |
| `POST /auth/logout` | signed in | 204 |
| `GET /auth/me` | signed in | 200 `{email, role, mustChangePassword}` |
| `GET /activities` | coordinator | 200 |
| `POST /activities` `{title,topic,plannedHours}` | coordinator | 201 |
| `PUT /activities/{id}` | coordinator | 200 |
| `DELETE /activities/{id}` | coordinator | 204, also removes its assignments (extra, not in the brief) |
| `GET /learners` | coordinator | 200 (with `assignedCount`, `completedCount`) |
| `POST /learners` `{name,email,cohort}` | coordinator | 201 candidate + one-time credentials, `Cache-Control: no-store` |
| `DELETE /learners/{id}` | coordinator | 204, also removes their account, assignments and open sessions (extra, not in the brief) |
| `POST /learners/{id}/reset-password` | coordinator | 200 new one-time credentials; ends the candidate's sessions |
| `GET /learners/{id}/assignments` | coordinator | 200 with activity details |
| `POST /assignments` `{learnerId,activityId}` | coordinator | 201 |
| `PATCH /assignments/{id}/status` `{status}` | the owning candidate | 200 |
| `GET /cohort-summary` | coordinator | 200 `{learners, assignments, completed, inProgress, notStarted}` |
| `GET /me/assignments` | candidate | 200, identity from the session only |

| Status | Meaning |
| --- | --- |
| 400 | missing fields, invalid status, hours ≤ 0, malformed body |
| 401 | not logged in, expired session, invalid or expired credentials (always the same generic login message) |
| 403 | wrong role, another candidate's assignment, missing CSRF token, or the first-login password change not done yet |
| 404 | learner, activity or assignment not found |
| 409 | duplicate email (case-insensitive) or duplicate assignment |
| 429 | too many failed logins (generic message) |

## Data model

```
accounts     id, email (unique, lower-cased), password_hash (BCrypt), role (COORDINATOR|CANDIDATE),
             must_change_password, temp_password_expires_at
learners     id, display_name, cohort, account_id (unique, FK -> accounts)
activities   id, title, topic, planned_hours (CHECK > 0)
assignments  id, learner_id (FK), activity_id (FK), status (Not Started|In Progress|Completed),
             UNIQUE (learner_id, activity_id)
```

A candidate is a `learner` row linked to one `account`. The coordinator only has an account. Password
hashes never leave the server (DTOs do not contain them, and the test suite checks responses for them).

## Security summary

- Spring Security, BCrypt password hashes, server-side session (opaque id in an `HttpOnly`, `SameSite=Lax`
  cookie), 30-minute expiry, `Secure` flag controlled by `SESSION_COOKIE_SECURE`.
- CSRF protection on: Spring sets an `XSRF-TOKEN` cookie and the React client echoes it in `X-XSRF-TOKEN`.
- Same origin through the Vite proxy, so no CORS configuration is needed.
- Roles and ownership are enforced on the server (`SecurityConfig` for roles, `AssignmentService` for ownership);
  the React route guards only mirror them.
- Temporary passwords are random (14 characters), shown once, valid for 24 hours. A session that still has a
  temporary password can only call change-password, logout and the identity check. Changing the password
  starts a fresh session and ends the others. A coordinator reset ends the candidate's sessions at once.
- Login rate limit (in memory, per email and per IP), same error for unknown email, wrong password and expired
  credentials, and a dummy hash check for unknown emails so response time does not reveal accounts.
- No secrets in the repository: `.env` is git-ignored and `.env.example` has placeholders only. Passwords are never logged.
- **HTTPS:** the brief requires HTTPS outside localhost. The app does not terminate TLS itself; put it behind an
  HTTPS reverse proxy and set `SESSION_COOKIE_SECURE=true`. The rate limiter uses the connection's address, so
  behind a proxy configure it to forward the real client address before relying on the per-IP limit.

## Decisions and small additions to the brief

- `GET /auth/me` was added so the React route guards can restore state after a page refresh.
- Status code `429` is used for rate limiting (the brief lists no code for it).
- A wrong current password on change-password returns `400` (not `401`) so the UI does not mistake it for an ended session.
- `/activities` is coordinator-only; candidates see activity details through `/me/assignments`.
- Only the candidate who owns an assignment can change its status; a coordinator gets `403`.
- `DELETE /activities/{id}` and `DELETE /learners/{id}` are not in the brief; they were added on request. Deleting an
  activity also removes its assignments; deleting a learner also removes their account, assignments and sessions (so the
  email can be reused). The UI always asks for confirmation first.
- The Activities and Learners pages have a search box (name, email, cohort / title, topic). It filters the list already
  loaded in the page, ignoring case and accents, and needs no extra API. For very large data it would move to a
  server-side query with pagination.
- Public self-registration is intentionally absent (candidate onboarding is FR-7, done by the coordinator).
- The landing page, charts and layout are presentation only and add no business behaviour.

## Why this stack

- **Spring Boot with layered controller / service / repository code** keeps rules in one place (services),
  makes them easy to unit-test with mocks, and gives production-grade authentication, sessions and CSRF
  protection through Spring Security instead of hand-written code.
- **JPA with MySQL** matches the data: a small relational model whose key rules (unique email, one assignment per
  learner and activity, hours greater than zero) are also enforced by database constraints, as a safety net.
- **React with Vite** gives a fast development loop and a clear separation from the API. The Vite proxy makes the
  browser talk to a single origin, which is what makes secure session cookies and CSRF simple.
- **Plain CSS and no UI library** keeps the bundle small and the accessibility work (focus, labels, contrast) explicit.
- **H2 for tests only** lets the whole API test suite run anywhere with no database setup.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Access denied for user 'lpt_user'` | `DB_PASSWORD` in `backend/.env` differs from the MySQL user's password. Re-run the SQL above and make them match. |
| `Could not resolve placeholder 'DB_PASSWORD'` | `backend/.env` is missing or misnamed (check it is not `.env.txt`) or you ran `mvn` outside `backend/`. |
| Backend refuses to start: password must be at least 12 characters | Use a longer `APP_COORDINATOR_PASSWORD`. |
| Browser shows "Cannot reach the server" | The backend is not running on port 8080. |
| 403 when calling the API by hand | Unsafe requests need the `X-XSRF-TOKEN` header; see the PowerShell helper in `docs/DEMO.md`. |
| Mockito tests fail with `Java 25 (69) is not supported by the current version of Byte Buddy` | Already handled in `pom.xml` (`-Dnet.bytebuddy.experimental=true`). If you run tests from an IDE, add that as a VM option, or use JDK 21. |
| Changed `.env` but nothing changed | Restart the backend; settings are read at startup. |
