#!/usr/bin/env bash
# Runs the MindSpace database test-suite against a TEMPORARY PostgreSQL cluster.
#
#   * Creates its own cluster in a fresh temp directory (never touches an existing database).
#   * Applies backend/src/main/resources/db/migration/V*.sql in version order, each inside one
#     transaction (psql --single-transaction). NOTE: this is plain psql, NOT Flyway.
#   * Runs db/tests/*.sql suites, then multi-session concurrency tests, then prints every result.
#   * Exits non-zero if any assertion fails or any suite aborts.
#
# Environment overrides:
#   PG_BIN                    directory containing initdb/pg_ctl/psql (default: newest /usr/lib/postgresql/*/bin)
#   MINDSPACE_TEST_PG_PORT    TCP port for the temporary server (default 55433)
#   MINDSPACE_TEST_PG_TMP     parent directory for the temporary cluster (default $TMPDIR or /tmp)
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MIG_DIR="$ROOT/backend/src/main/resources/db/migration"
TEST_DIR="$ROOT/db/tests"
PG_BIN="${PG_BIN:-$(ls -d /usr/lib/postgresql/*/bin 2>/dev/null | sort -V | tail -1)}"
PORT="${MINDSPACE_TEST_PG_PORT:-55433}"
BASE="${MINDSPACE_TEST_PG_TMP:-${TMPDIR:-/tmp}}"
DB="mindspace_test"

[ -n "${PG_BIN:-}" ] && [ -x "$PG_BIN/initdb" ] || { echo "PostgreSQL server binaries not found (set PG_BIN)"; exit 2; }

# Guard 1: never run against something that is already listening on the port.
if (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then
  echo "Port $PORT is already in use; refusing to run (set MINDSPACE_TEST_PG_PORT)."; exit 2
fi

AS_PG=()
if [ "$(id -u)" -eq 0 ]; then AS_PG=(runuser -u postgres --); fi   # postgres refuses to run as root

WORK="$(mktemp -d "$BASE/mindspace-dbtest.XXXXXX")"
[ "$(id -u)" -eq 0 ] && chown postgres:postgres "$WORK"
chmod 700 "$WORK"
cd "$WORK" || exit 2

cleanup() {
  "${AS_PG[@]}" "$PG_BIN/pg_ctl" -D "$WORK/data" -m immediate stop >/dev/null 2>&1 || true
  case "$WORK" in */mindspace-dbtest.*) rm -rf "$WORK" ;; esac   # only ever removes our own temp dir
}
trap cleanup EXIT

echo "== MindSpace database tests =="
echo "date:        $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "temp dir:    $WORK (removed on exit)"

"${AS_PG[@]}" "$PG_BIN/initdb" -D "$WORK/data" -U postgres --auth=trust -E UTF8 >"$WORK/initdb.log" 2>&1 \
  || { echo "initdb failed:"; cat "$WORK/initdb.log"; exit 2; }
"${AS_PG[@]}" "$PG_BIN/pg_ctl" -D "$WORK/data" -w -l "$WORK/pg.log" \
  -o "-p $PORT -c listen_addresses=127.0.0.1 -c unix_socket_directories=$WORK -c fsync=off -c synchronous_commit=off -c full_page_writes=off" \
  start >"$WORK/start.log" 2>&1 || { echo "pg_ctl start failed:"; cat "$WORK/start.log" "$WORK/pg.log"; exit 2; }

PSQL=("$PG_BIN/psql" -h 127.0.0.1 -p "$PORT" -U postgres -X -q -v ON_ERROR_STOP=1)

# Guard 2: confirm the server we reached really is our temporary cluster.
DATADIR="$("${PSQL[@]}" -d postgres -Atc 'SHOW data_directory')"
if [ "$DATADIR" != "$WORK/data" ]; then echo "Connected to an unexpected server ($DATADIR); aborting."; exit 2; fi

echo "server:      $("${PSQL[@]}" -d postgres -Atc 'SELECT version()')"
"${PSQL[@]}" -d postgres -c "CREATE DATABASE $DB" || exit 2

echo
echo "== Applying migrations (plain psql, one transaction per file; NOT Flyway) =="
for f in $(ls "$MIG_DIR"/V*__*.sql | sort -V); do
  if "${PSQL[@]}" -d "$DB" --single-transaction -f "$f" >"$WORK/mig.out" 2>&1; then
    echo "  applied  $(basename "$f")"
  else
    echo "  FAILED   $(basename "$f")"; cat "$WORK/mig.out"; exit 1
  fi
done

echo
echo "== Running test suites =="
SUITE_ERRORS=0
"${PSQL[@]}" -d "$DB" -o /dev/null -f "$TEST_DIR/00_helpers.sql" || { echo "helpers failed to load"; exit 1; }
for f in $(ls "$TEST_DIR"/[1-7]0_*.sql | sort); do
  name="$(basename "$f")"
  if "${PSQL[@]}" -d "$DB" -o /dev/null -f "$f" 2>"$WORK/suite.err"; then
    echo "  ran      $name"
  else
    echo "  ABORTED  $name"; sed 's/^/           /' "$WORK/suite.err"; SUITE_ERRORS=$((SUITE_ERRORS + 1))
  fi
done

# ---------------------------------------------------------------------------------------------
# Concurrency: N real, simultaneous psql sessions. Each session holds its transaction open for a
# moment (pg_sleep) so the sessions genuinely overlap and contend on the same unique key.
# ---------------------------------------------------------------------------------------------
echo
echo "== Concurrency tests (10 parallel sessions each) =="
"${PSQL[@]}" -d "$DB" -o /dev/null <<'SQL' || { echo "concurrency fixture failed"; exit 1; }
INSERT INTO users (id, email, display_name, password_hash) VALUES
  ('80000000-0000-0000-0000-00000000000a', 'conc.a@example.test', 'Synthetic', 'synthetic-not-a-real-hash'),
  ('80000000-0000-0000-0000-00000000000b', 'conc.b@example.test', 'Synthetic', 'synthetic-not-a-real-hash');
INSERT INTO habits (id, user_id, name, cadence, target_count, start_date) VALUES
  ('83000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-00000000000b', 'Concurrent habit', 'DAILY', 1, '2026-10-01');
SQL

N=10
run_parallel() {   # run_parallel <dir> <sql-with-@I@-placeholder>
  local dir="$1" sql="$2" i
  mkdir -p "$dir"
  for i in $(seq 1 "$N"); do
    ( "${PSQL[@]}" -d "$DB" -c "${sql//@I@/$i}" >"$dir/out.$i" 2>"$dir/err.$i"; echo $? >"$dir/rc.$i" ) &
  done
  wait
}
n_ok()  { grep -lx 0 "$1"/rc.* 2>/dev/null | wc -l | tr -d ' '; }
n_err() { grep -l "$2" "$1"/err.* 2>/dev/null | wc -l | tr -d ' '; }
record() {         # record <label> <true|false> <detail>
  "${PSQL[@]}" -d "$DB" -o /dev/null -c "SELECT test.record('concurrency', \$\$$1\$\$, $2, \$\$$3\$\$)"
}
scalar() { "${PSQL[@]}" -d "$DB" -Atc "$1"; }

# C1: duplicate daily check-in, plain INSERT -> exactly one winner, nine unique violations.
run_parallel "$WORK/c1" "BEGIN; INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes) VALUES ('80000000-0000-0000-0000-00000000000a', '2026-10-17', 'Asia/Kolkata', 3, 3, 3, 400); SELECT pg_sleep(0.7); COMMIT;"
ok=$(n_ok "$WORK/c1"); viol=$(n_err "$WORK/c1" 'mood_check_ins_user_date_key'); rows=$(scalar "SELECT count(*) FROM mood_check_ins WHERE user_id='80000000-0000-0000-0000-00000000000a' AND local_date='2026-10-17'")
[ "$ok" -eq 1 ] && [ "$viol" -eq $((N-1)) ] && [ "$rows" -eq 1 ] && r=true || r=false
record "10 parallel plain INSERTs of the same (user, local_date) check-in: exactly one succeeds, the rest get a unique violation" "$r" "succeeded=$ok unique_violations=$viol rows_in_table=$rows"

# C2: idempotent habit completion -> all ten requests succeed, one row.
run_parallel "$WORK/c2" "BEGIN; INSERT INTO habit_completions (habit_id, user_id, completed_on) VALUES ('83000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-00000000000b', '2026-10-17') ON CONFLICT (habit_id, completed_on) DO NOTHING; SELECT pg_sleep(0.7); COMMIT;"
ok=$(n_ok "$WORK/c2"); rows=$(scalar "SELECT count(*) FROM habit_completions WHERE habit_id='83000000-0000-0000-0000-000000000001' AND completed_on='2026-10-17'")
[ "$ok" -eq "$N" ] && [ "$rows" -eq 1 ] && r=true || r=false
record "10 parallel idempotent habit completions (ON CONFLICT DO NOTHING): all succeed, exactly one row stored" "$r" "succeeded=$ok rows_in_table=$rows"

# C3: concurrent upserts with different values -> all succeed, one row, value from one of the writers.
run_parallel "$WORK/c3" "BEGIN; INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes) VALUES ('80000000-0000-0000-0000-00000000000a', '2026-10-18', 'Asia/Kolkata', 1 + (@I@ % 5), 3, 3, 400) ON CONFLICT (user_id, local_date) DO UPDATE SET mood_score = EXCLUDED.mood_score; SELECT pg_sleep(0.3); COMMIT;"
ok=$(n_ok "$WORK/c3"); rows=$(scalar "SELECT count(*) FROM mood_check_ins WHERE user_id='80000000-0000-0000-0000-00000000000a' AND local_date='2026-10-18'"); mood=$(scalar "SELECT mood_score FROM mood_check_ins WHERE user_id='80000000-0000-0000-0000-00000000000a' AND local_date='2026-10-18'")
[ "$ok" -eq "$N" ] && [ "$rows" -eq 1 ] && [ "$mood" -ge 1 ] && [ "$mood" -le 5 ] && r=true || r=false
record "10 parallel check-in upserts with different mood values: all succeed, exactly one row, valid final value" "$r" "succeeded=$ok rows_in_table=$rows final_mood=$mood"

# C4: concurrent registration with the same e-mail -> exactly one account.
run_parallel "$WORK/c4" "BEGIN; INSERT INTO users (email, display_name, password_hash) VALUES ('race@example.test', 'Race', 'synthetic-not-a-real-hash'); SELECT pg_sleep(0.7); COMMIT;"
ok=$(n_ok "$WORK/c4"); viol=$(n_err "$WORK/c4" 'users_email_key'); rows=$(scalar "SELECT count(*) FROM users WHERE email='race@example.test'")
[ "$ok" -eq 1 ] && [ "$viol" -eq $((N-1)) ] && [ "$rows" -eq 1 ] && r=true || r=false
record "10 parallel registrations with the same e-mail: exactly one account is created" "$r" "succeeded=$ok unique_violations=$viol accounts=$rows"

# ---------------------------------------------------------------------------------------------
echo
echo "== Results =="
"${PSQL[@]}" -d "$DB" -At -c "SELECT format('%-4s [%s] %s  -> %s', status, suite, label, coalesce(detail, '')) FROM test.results ORDER BY id"

echo
echo "== Summary =="
"${PSQL[@]}" -d "$DB" -c "SELECT suite, count(*) FILTER (WHERE status = 'PASS') AS pass, count(*) FILTER (WHERE status = 'FAIL') AS fail, count(*) AS total FROM test.results GROUP BY suite ORDER BY min(id)"
read -r PASS FAIL TOTAL < <("${PSQL[@]}" -d "$DB" -At -F ' ' -c "SELECT count(*) FILTER (WHERE status = 'PASS'), count(*) FILTER (WHERE status = 'FAIL'), count(*) FROM test.results")
echo "TOTAL: $TOTAL assertions, $PASS passed, $FAIL failed; suites aborted: $SUITE_ERRORS"

if [ "$FAIL" -ne 0 ] || [ "$SUITE_ERRORS" -ne 0 ]; then echo "RESULT: FAILED"; exit 1; fi
echo "RESULT: ALL PASSED"
