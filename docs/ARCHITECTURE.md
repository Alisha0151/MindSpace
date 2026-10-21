# Architecture

See the diagrams in the README (`docs/diagrams/`).

## Shape
A **modular monolith**: one Spring Boot deployable, packages by feature under `com.mindspace`. Each feature has `Controller → Service → Repository` and its own DTO records. Features talk through services (e.g. dashboard uses `HabitService`), never through each other's repositories except for read-only aggregate views (analytics, calendar, privacy export).

## Why this split
- **Framework-free domain** (`common`, `*.domain`, `PasswordPolicy`, `TokenHasher`): streak maths, week boundaries (ISO, Monday start), HALF_UP rounding, check-in validation, mood analytics. These compile and test without Spring, which is also what allowed them to be verified in an environment with no dependency access.
- **Database as the final authority**: unique and CHECK constraints (one check-in per user per local day, composite ownership FKs so a completion can never reference another user's habit, cascading deletes). Services pre-check only for friendlier messages; constraint names map to 409s in `ApiExceptionHandler`.
- **Local dates, not instants**: a day belongs to the user's IANA time zone at the time of recording. The frontend computes "today" with the same zone (`lib/dates.ts`).
- **Analytics in Java over bounded data** (≤ 366 days) so rules are unit-testable and identical to the hand-computed SQL fixture.

## Request pipeline
`RequestIdFilter` → `RequestSizeLimitFilter` → `RateLimitFilter` → Spring Security (`JwtAuthenticationFilter` validates the access cookie and that its session family is still active, then CSRF) → controller. `JwtAuthenticationFilter` is deliberately not a bean to avoid double registration.

## Frontend
React Router (guards: `ProtectedRoute`, `PublicOnlyRoute`), TanStack Query for server state (cache cleared on sign-out), a thin `fetch` wrapper that sends cookies, echoes the CSRF cookie, and performs one single-flight refresh on 401. No tokens are readable by JavaScript. Charts (Recharts, lazy-loaded) always have a table alternative; colour is never the only signal.

## Data model
See [DATABASE.md](DATABASE.md). Migrations V1–V6 are Flyway files in `backend/src/main/resources/db/migration`; Hibernate runs with `ddl-auto=validate`.
