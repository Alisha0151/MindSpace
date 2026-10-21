# MindSpace database layer

Status: **schema written and verified against a real PostgreSQL 16.15 with plain `psql`.**
It has **not** been run through Flyway, Hibernate (`ddl-auto=validate`) or any Java code, because
Maven Central and the npm registry are unreachable from the build environment. See
[Verification status](#verification-status).

ER diagram: [`diagrams/er-diagram.mmd`](diagrams/er-diagram.mmd).

## Migrations

Location: `backend/src/main/resources/db/migration/` (Flyway's default).

| File | Contents |
|---|---|
| `V1__users_sessions_preferences.sql` | `set_updated_at()` trigger function, `users`, `refresh_sessions`, `user_preferences`, `consent_records` |
| `V2__mood_check_ins.sql` | `mood_check_ins`, `check_in_tags` |
| `V3__journal.sql` | `journal_entries` (with generated `search_vector`), `journal_entry_tags` |
| `V4__habits.sql` | `habits`, `habit_pauses`, `habit_completions` |
| `V5__coping_strategies.sql` | `coping_strategies` |

Strategy: forward-only, versioned, never edited once applied to a shared database (V1-V5 were edited
during development only, before any real database existed). V6 adds the partial index `refresh_sessions_family_active_idx` used by the per-request session check. Later changes go in `V7+`. Hibernate must
run with `spring.jpa.hibernate.ddl-auto=validate` (never `create`/`update`).

`WellnessGoal` from the brief is deliberately not in the schema yet.

## Conventions (chosen for Spring Boot 3 / JPA compatibility)

- UUID primary keys via `gen_random_uuid()` (built in since PostgreSQL 13).
- Instants are `TIMESTAMPTZ`; local calendar days are `DATE`, derived by the application from the user's
  IANA timezone, which is stored next to the value (`users.timezone`, `mood_check_ins.timezone`).
- Enumerations are `VARCHAR` + `CHECK`, not PostgreSQL `ENUM`, so `@Enumerated(EnumType.STRING)` works.
- Scores and minutes are `INTEGER` so Java `int` fields pass Hibernate schema validation.
- `version BIGINT` on mutable aggregates for `@Version` optimistic locking.
- `updated_at` is maintained by a trigger, so it is correct even for writes outside JPA.
- `journal_entries.search_vector` is a generated column: map it read-only or query it natively.

## Score meanings (self-reported observations, not clinical measures)

| Field | 1 | 5 |
|---|---|---|
| `mood_score` | very low | very good |
| `stress_score` | very calm | very stressed |
| `energy_score` | very low | very high |
| `sleep_quality` (optional) | very poor | very good |

## Key rules enforced by the database

- **One check-in per user per local day:** `UNIQUE (user_id, local_date)`. Concurrent duplicates: exactly one
  insert wins. The application can use `INSERT ... ON CONFLICT (user_id, local_date) DO UPDATE` for upsert.
- **One completion per habit per day:** `UNIQUE (habit_id, completed_on)`; repeats use `ON CONFLICT DO NOTHING`.
- **Ownership of habit children:** `habit_pauses` and `habit_completions` carry `user_id` and a composite FK
  `(habit_id, user_id) -> habits (id, user_id)`, so a row can never point at another user's habit even if
  application code is wrong.
- **Unique account e-mail:** stored lower-cased and trimmed; a CHECK rejects anything else.
- **Habit cadence:** `DAILY` requires `target_count = 1`; `WEEKLY` requires 1..7. Names are unique per user,
  case- and whitespace-insensitively.
- **At most one open pause per habit** (partial unique index).
- **Refresh tokens:** only a 64-char SHA-256 hex digest may be stored (CHECK), never a raw token.
- **Blank text:** name/title/body checks treat space, tab, CR and LF as blank.

Rules that need data from more than one table are **not** in the database and must be enforced (and tested)
in the Java service layer: completion date within the habit's start/end range, not inside a pause window,
not in the future; that a user's timezone is a valid `ZoneId`.

## Deletion behaviour

Every user-owned table has `user_id ... ON DELETE CASCADE`, and tag tables cascade from their parent, so
`DELETE FROM users WHERE id = ?` removes the account and everything it owns in one statement. There is no
soft delete and no anonymised residue in these tables. `refresh_sessions.replaced_by` is `ON DELETE SET NULL`
so deleting a successor token does not delete its predecessor. Not covered here: database backups, WAL and
infrastructure logs are outside the schema and are not erased by this cascade.

## Indexes (all 19 verified to exist; 7 verified to be chosen by the planner)

| Access pattern | Index |
|---|---|
| Login by e-mail | `users_email_key` |
| Refresh token lookup | `refresh_sessions_token_hash_key` |
| A user's active sessions / revoke all | `refresh_sessions_active_idx` (partial) |
| Expired-session cleanup | `refresh_sessions_expires_idx` |
| Check-ins in a date range | `mood_check_ins_user_date_key` |
| Journal history, newest first, keyset paging | `journal_entries_user_created_idx (user_id, created_at DESC, id DESC)` |
| Journal date-range filter | `journal_entries_user_date_idx` |
| Journal full-text search | `journal_entries_search_idx` (GIN; always also filter on `user_id`) |
| Journal tag filter | `journal_entry_tags_tag_idx` |
| Completions across habits by date | `habit_completions_user_date_idx` |
| Duplicate-completion probe | `habit_completions_habit_date_key` |

## Verification status

Run with: `bash db/scripts/run-db-tests.sh`. It starts a **temporary** PostgreSQL cluster in a fresh temp
directory on port 55433, refuses to run if that port is in use, checks it is connected to its own data
directory, applies V1-V5 with `psql --single-transaction`, runs the suites, and deletes the cluster.

Latest result (`docs/db-verification/run-2.log`, PostgreSQL 16.15): **172 assertions, 172 passed, 0 failed.**

| Suite | Assertions | What it covers |
|---|---|---|
| schema | 15 | 12 tables, PKs, UUID/timestamptz types, every `user_id` NOT NULL with a cascading FK, indexes, triggers |
| checkins | 29 | score and sleep bounds, duplicate day, per-user dates, upsert, trigger, tags, cascade |
| journal | 28 | pagination (keyset), date range, full-text search scoped per user, tags, limits, cascade |
| habits | 31 | cadence rules, name uniqueness, duplicate completion, composite ownership FK, pauses, cascade |
| ownership | 34 | identity/session/consent constraints, scoped-query IDOR pattern, account deletion across 11 tables |
| analytics | 23 | hand-computed expectations: weekly/monthly trends, distribution, emotions, sleep buckets, habit vs mood, coverage, streaks, timezone, empty user, no cross-user leakage |
| plans | 8 | planner uses the intended index on 30,000 check-ins / 9,000 journal entries / 18,000 completions |
| concurrency | 4 | 10 parallel real sessions: duplicate check-in, idempotent completion, upsert, duplicate registration |

The first run found 3 failures: a real bug (the blank-body check ignored newlines, now fixed in the migrations
with regression tests) and two wrong test expectations (over-length titles raise SQLSTATE 22001 from `VARCHAR`,
not a CHECK violation). Log: `docs/db-verification/run-1.log`.

### Not verified (do not assume)

- **Flyway and Hibernate `validate`:** later exercised in CI (the Testcontainers integration tests start the app on a fresh PostgreSQL 16, apply V1–V6 with Flyway and validate every entity). The `psql` suite above remains the authoritative constraint test.
- **Performance.** Plan tests show index *choice* on synthetic data only; no latency was measured.
- **Dev/prod parity.** Only PostgreSQL 16.15 was used. Test cluster settings turn fsync off for speed.
- **Analytics SQL is reference SQL.** The Java analytics service will need its own tests against these expectations.
