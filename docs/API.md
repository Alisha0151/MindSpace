# API reference

Base path `/api/v1`. JSON in, JSON out. All endpoints except those marked **public** need the `ms_access` cookie. Every write (POST/PUT/PATCH/DELETE) needs the CSRF header `X-XSRF-TOKEN` equal to the `XSRF-TOKEN` cookie (call `GET /auth/csrf` first). Authoritative, generated documentation: Swagger UI at `/swagger-ui/index.html` and the spec at `/v3/api-docs` (not yet generated: the backend has not been run).

**Errors** are RFC 9457 `application/problem+json`: `type` (`urn:mindspace:problem:*`), `title`, `status`, `detail`, `requestId`, and for validation `errors: [{field, message}]`. Other users' resource ids return **404**, never 403.

| Status | Meaning |
|---|---|
| 400 | Validation failed / malformed request (`errors[]`) |
| 401 | Not signed in, session revoked/expired, or bad credentials (generic message) |
| 403 | Missing/invalid CSRF token |
| 404 | Not found (or belongs to someone else) |
| 409 | Conflict: duplicate check-in date, habit name, e-mail, or concurrent edit |
| 413 | Body larger than 64 KiB |
| 429 | Rate limited (`Retry-After`) |

## Auth (`/auth`)
| Method & path | Purpose |
|---|---|
| `GET /auth/csrf` (public) | Issue the `XSRF-TOKEN` cookie |
| `POST /auth/register` (public) | Create account + sign in. Body: `email, password, confirmPassword, displayName, timezone, acceptTerms` |
| `POST /auth/login` (public) | `email, password`. Same error for unknown e-mail and wrong password |
| `POST /auth/refresh` (public, needs refresh cookie) | Rotate refresh token, issue new access token |
| `POST /auth/logout` (public) | Revoke this session, clear cookies |
| `POST /auth/logout-all` | Revoke every session |
| `GET /auth/sessions` | Active sessions; `current` marks this one |
| `DELETE /auth/sessions/{id}` | Revoke one session |

## User (`/users/me`)
`GET` profile · `PATCH` `{displayName?, timezone?, theme?}` · `PUT /password` `{currentPassword,newPassword,confirmPassword}` (signs out other devices) · `GET|PUT /consents` (`{optionalUsageAnalytics}`)

## Dashboard
`GET /dashboard` – today's check-in, check-in streak, 7-day mood, habits, recent journal summaries.

## Check-ins (`/checkins`) – scores 1–5, at most one per local day
`POST` · `GET /today` (204 if none) · `GET ?from&to` (default last 30 days, max 366) · `GET|PUT|DELETE /{id}`

## Journal (`/journal`)
`POST` `{title?, body, promptText?, tags[]}` · `GET ?q&tag&from&to&page&size` (full-text, size ≤ 50) · `GET|PUT|DELETE /{id}`

## Habits (`/habits`)
`GET` · `POST` `{name, description?, cadence: DAILY|WEEKLY, targetCount?, startDate?, endDate?}` (max 50) · `GET|PUT|DELETE /{id}` · `POST /{id}/pause` · `POST /{id}/resume` · `PUT|DELETE /{id}/completions/{yyyy-MM-dd}` (idempotent) · `GET /{id}/progress?from&to` (default 28 days, max 366)

## Calendar
`GET /calendar?month=YYYY-MM&minMood&maxMood&emotion&activity` – no notes or journal text.

## Analytics
`GET /analytics/summary?from&to&compareFrom&compareTo&habitId` – default last 30 days, max 366.

## Wellness (`/wellness`)
`GET /toolkit` (static content) · `GET|POST /coping-strategies` · `PUT|DELETE /coping-strategies/{id}` · `PUT /coping-strategies/order` `{ids[]}` (must list every id once)

## Privacy
`GET /privacy/export` – JSON, `schemaVersion: 1`, `Cache-Control: no-store`. `DELETE /account` `{password}` – permanent; clears cookies.
