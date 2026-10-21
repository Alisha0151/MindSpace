# Testing

## What exists and what has actually been run
| Layer | Files | Status |
|---|---|---|
| SQL (migrations, constraints, ownership, deletion, queries, plans) | `db/tests/*.sql`, `db/scripts/run-db-tests.sh` | **Run**: 173/173 on PostgreSQL 16 via plain `psql` (logs in `docs/db-verification/`). Run 1 had 3 real failures that were fixed; see the logs. Not run through Flyway. |
| Pure Java unit tests | `backend/src/test/java/**` (excluding `it/`, `support/`) | **Run**: 63/63 with `scripts/run-domain-tests.sh` against a JUnit-API stand-in (not real JUnit). A mutation spot-check (4 planted bugs, 3 effective) made 10 tests fail. |
| Backend integration tests | `backend/src/test/java/com/mindspace/it/*IT.java` | **Run in CI** (24 tests, all pass): real PostgreSQL 16 via Testcontainers, Flyway + Hibernate validation. Need Docker and Maven locally. Cover: cookie auth, CSRF, logout revocation, refresh rotation/forged token, cross-user 404 on every verb, duplicate check-in 409, journal search, idempotent habit completion, export contents, account deletion cascade, Flyway applied + Hibernate validate. |
| Frontend unit tests | `frontend/src/lib/*.test.ts` | **Run**: 17/17 with a vitest-globals stand-in. React component tests (RTL) not written yet. |
| End-to-end | `frontend/e2e/smoke.spec.ts` | **Run in CI** (3 smoke scenarios pass against the Docker Compose stack). |
| Type checking | `tsc` | Pure modules pass `--strict`; `npm run typecheck` (app + Node configs) passes in CI. |

## Commands
```bash
bash db/scripts/run-db-tests.sh
JUNIT_SHIM_DIR=/path/to/compiled-junit-api-standin ./scripts/run-domain-tests.sh
./scripts/javac-triage.sh
cd backend && mvn verify                 # surefire (*Test) + failsafe (*IT)
cd frontend && npm test && npm run typecheck && npx playwright test
```

## Stand-ins: what they do and do not prove
The JUnit and vitest stand-ins implement only the handful of APIs these tests use. They prove the assertions hold for the logic; they do not prove the tests run under the real frameworks. The javac triage only proves there are no syntax or project-internal symbol errors, not that the Spring code compiles with its dependencies or behaves correctly.

## Gaps to close first
1. `mvn verify` (expect compile fixes), then the ITs.
2. `npm install && npm run typecheck`, then component tests for forms and the breathing exercise.
3. Playwright run and accessibility scan (axe) per page.
4. Load test of journal search; verify the GIN index is used on real data.
