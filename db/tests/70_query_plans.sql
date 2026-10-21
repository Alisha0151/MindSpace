-- Suite: plans  (do the intended indexes serve the main access patterns on a realistic row count?)
-- Bulk synthetic data: 300 users x 100 check-ins, 300 users x 30 journal entries, 300 users x 60 completions.
-- This suite asserts index USAGE only. It makes no claim about latency.

INSERT INTO users (id, email, display_name, password_hash, timezone)
SELECT gen_random_uuid(), 'bulk' || n || '@example.test', 'Bulk ' || n, 'synthetic-not-a-real-hash', 'UTC'
FROM generate_series(1, 300) AS n;

INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
SELECT u.id, d::date, 'UTC',
       1 + (abs(hashtext(u.email || d::text)) % 5),
       1 + (abs(hashtext(d::text || u.email)) % 5),
       1 + (abs(hashtext(u.email || 'e' || d::text)) % 5),
       240 + (abs(hashtext(u.email || 's' || d::text)) % 300)
FROM users u
CROSS JOIN generate_series(DATE '2026-01-01', DATE '2026-04-10', INTERVAL '1 day') AS d
WHERE u.email LIKE 'bulk%@example.test';

INSERT INTO journal_entries (user_id, entry_date, body, created_at)
SELECT u.id, (DATE '2026-01-01' + n), 'synthetic bulk entry number ' || n,
       (DATE '2026-01-01' + n)::timestamp + interval '9 hours' + (n || ' minutes')::interval
FROM users u CROSS JOIN generate_series(0, 29) AS n
WHERE u.email LIKE 'bulk%@example.test';

INSERT INTO habits (user_id, name, cadence, target_count, start_date)
SELECT id, 'Bulk habit', 'DAILY', 1, DATE '2026-01-01' FROM users WHERE email LIKE 'bulk%@example.test';

INSERT INTO habit_completions (habit_id, user_id, completed_on)
SELECT h.id, h.user_id, (DATE '2026-01-01' + n)
FROM habits h CROSS JOIN generate_series(0, 59) AS n
WHERE h.name = 'Bulk habit' AND h.user_id IN (SELECT id FROM users WHERE email LIKE 'bulk%@example.test');

INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at)
SELECT u.id, gen_random_uuid(), md5(u.email || n) || md5(n || u.email), now() + interval '7 days'
FROM users u CROSS JOIN generate_series(1, 10) AS n
WHERE u.email LIKE 'bulk%@example.test';

ANALYZE;

SELECT test.assert_eq('plans', 'bulk volume loaded: 30000 check-ins / 9000 journal entries / 18000 completions',
    test.scalar($q$ SELECT (SELECT count(*) FROM mood_check_ins c JOIN users u ON u.id = c.user_id WHERE u.email LIKE 'bulk%@example.test')
                       || '/' || (SELECT count(*) FROM journal_entries j JOIN users u ON u.id = j.user_id WHERE u.email LIKE 'bulk%@example.test')
                       || '/' || (SELECT count(*) FROM habit_completions h JOIN users u ON u.id = h.user_id WHERE u.email LIKE 'bulk%@example.test') $q$),
    '30000/9000/18000');

SELECT test.assert_plan_uses('plans', 'check-in date-range lookup for one user uses mood_check_ins_user_date_key',
    format($f$ SELECT * FROM mood_check_ins WHERE user_id = %L AND local_date BETWEEN '2026-02-01' AND '2026-02-14' $f$,
           (SELECT id FROM users WHERE email = 'bulk150@example.test')),
    'mood_check_ins_user_date_key');

SELECT test.assert_plan_uses('plans', 'journal history page (newest first, LIMIT 20) uses journal_entries_user_created_idx',
    format($f$ SELECT * FROM journal_entries WHERE user_id = %L ORDER BY created_at DESC, id DESC LIMIT 20 $f$,
           (SELECT id FROM users WHERE email = 'bulk150@example.test')),
    'journal_entries_user_created_idx');

SELECT test.assert_plan_uses('plans', 'journal date-range filter uses journal_entries_user_date_idx',
    format($f$ SELECT * FROM journal_entries WHERE user_id = %L AND entry_date BETWEEN '2026-01-05' AND '2026-01-09' $f$,
           (SELECT id FROM users WHERE email = 'bulk150@example.test')),
    'journal_entries_user_date_idx');

SELECT test.assert_plan_uses('plans', 'completions by user and date range use habit_completions_user_date_idx',
    format($f$ SELECT * FROM habit_completions WHERE user_id = %L AND completed_on BETWEEN '2026-01-10' AND '2026-01-20' $f$,
           (SELECT id FROM users WHERE email = 'bulk150@example.test')),
    'habit_completions_user_date_idx');

SELECT test.assert_plan_uses('plans', 'habit completion duplicate probe (habit_id, completed_on) uses habit_completions_habit_date_key',
    format($f$ SELECT 1 FROM habit_completions WHERE habit_id = %L AND completed_on = '2026-01-10' $f$,
           (SELECT id FROM habits WHERE user_id = (SELECT id FROM users WHERE email = 'bulk150@example.test') AND name = 'Bulk habit')),
    'habit_completions_habit_date_key');

SELECT test.assert_plan_uses('plans', 'login lookup by email uses users_email_key',
    $f$ SELECT * FROM users WHERE email = 'bulk150@example.test' $f$,
    'users_email_key');

SELECT test.assert_plan_uses('plans', 'refresh-token lookup by hash uses refresh_sessions_token_hash_key',
    format($f$ SELECT * FROM refresh_sessions WHERE token_hash = %L $f$, md5('x') || md5('y')),
    'refresh_sessions_token_hash_key');

SELECT test.assert_plan_uses('plans', 'per-request session check (family_id, not revoked) uses refresh_sessions_family_active_idx (V6)',
    format($f$ SELECT 1 FROM refresh_sessions WHERE family_id = %L AND revoked_at IS NULL $f$,
           (SELECT family_id FROM refresh_sessions LIMIT 1)),
    'refresh_sessions_family_active_idx');
