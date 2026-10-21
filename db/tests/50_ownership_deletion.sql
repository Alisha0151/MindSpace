-- Suite: ownership  (identity/session constraints, user-scoped access pattern, account deletion)

CREATE FUNCTION test.owned_rows(p_user UUID) RETURNS TEXT LANGUAGE sql AS
$$
    SELECT concat_ws('/',
        (SELECT count(*) FROM user_preferences WHERE user_id = p_user),
        (SELECT count(*) FROM consent_records  WHERE user_id = p_user),
        (SELECT count(*) FROM refresh_sessions WHERE user_id = p_user),
        (SELECT count(*) FROM mood_check_ins   WHERE user_id = p_user),
        (SELECT count(*) FROM check_in_tags t JOIN mood_check_ins c ON c.id = t.check_in_id WHERE c.user_id = p_user),
        (SELECT count(*) FROM journal_entries  WHERE user_id = p_user),
        (SELECT count(*) FROM journal_entry_tags t JOIN journal_entries j ON j.id = t.entry_id WHERE j.user_id = p_user),
        (SELECT count(*) FROM habits           WHERE user_id = p_user),
        (SELECT count(*) FROM habit_pauses     WHERE user_id = p_user),
        (SELECT count(*) FROM habit_completions WHERE user_id = p_user),
        (SELECT count(*) FROM coping_strategies WHERE user_id = p_user));
$$;

-- Complete synthetic data set for two users (suffix a / b in every id).
INSERT INTO users (id, email, display_name, password_hash, timezone) VALUES
    ('50000000-0000-0000-0000-00000000000a', 'own.a@example.test', 'Synthetic A', 'synthetic-not-a-real-hash', 'UTC'),
    ('50000000-0000-0000-0000-00000000000b', 'own.b@example.test', 'Synthetic B', 'synthetic-not-a-real-hash', 'UTC');

INSERT INTO user_preferences (user_id, theme) VALUES
    ('50000000-0000-0000-0000-00000000000a', 'dark'), ('50000000-0000-0000-0000-00000000000b', 'light');
INSERT INTO consent_records (user_id, consent_type, granted, policy_version) VALUES
    ('50000000-0000-0000-0000-00000000000a', 'PRIVACY_NOTICE_ACK', TRUE, 'v1'),
    ('50000000-0000-0000-0000-00000000000b', 'PRIVACY_NOTICE_ACK', TRUE, 'v1');

-- s2 is the newer token; s1 was rotated into s2.
INSERT INTO refresh_sessions (id, user_id, family_id, token_hash, expires_at) VALUES
    ('5a000000-0000-0000-0000-0000000000a2', '50000000-0000-0000-0000-00000000000a', gen_random_uuid(), md5('A2') || md5('A2b'), now() + interval '7 days'),
    ('5b000000-0000-0000-0000-0000000000b2', '50000000-0000-0000-0000-00000000000b', gen_random_uuid(), md5('B2') || md5('B2b'), now() + interval '7 days');
INSERT INTO refresh_sessions (id, user_id, family_id, token_hash, expires_at, revoked_at, revoked_reason, replaced_by) VALUES
    ('5a000000-0000-0000-0000-0000000000a1', '50000000-0000-0000-0000-00000000000a', gen_random_uuid(), md5('A1') || md5('A1b'), now() + interval '7 days', now(), 'ROTATED', '5a000000-0000-0000-0000-0000000000a2'),
    ('5b000000-0000-0000-0000-0000000000b1', '50000000-0000-0000-0000-00000000000b', gen_random_uuid(), md5('B1') || md5('B1b'), now() + interval '7 days', now(), 'ROTATED', '5b000000-0000-0000-0000-0000000000b2');

INSERT INTO mood_check_ins (id, user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes) VALUES
    ('51000000-0000-0000-0000-00000000000a', '50000000-0000-0000-0000-00000000000a', '2026-10-01', 'UTC', 3, 3, 3, 400),
    ('51000000-0000-0000-0000-00000000000b', '50000000-0000-0000-0000-00000000000b', '2026-10-01', 'UTC', 3, 3, 3, 400);
INSERT INTO check_in_tags (check_in_id, tag_type, label) VALUES
    ('51000000-0000-0000-0000-00000000000a', 'EMOTION', 'calm'), ('51000000-0000-0000-0000-00000000000a', 'ACTIVITY', 'rest'),
    ('51000000-0000-0000-0000-00000000000b', 'EMOTION', 'calm'), ('51000000-0000-0000-0000-00000000000b', 'ACTIVITY', 'rest');

INSERT INTO journal_entries (id, user_id, entry_date, body) VALUES
    ('52000000-0000-0000-0000-00000000000a', '50000000-0000-0000-0000-00000000000a', '2026-10-01', 'synthetic entry A'),
    ('52000000-0000-0000-0000-00000000000b', '50000000-0000-0000-0000-00000000000b', '2026-10-01', 'synthetic entry B');
INSERT INTO journal_entry_tags (entry_id, tag) VALUES
    ('52000000-0000-0000-0000-00000000000a', 'misc'), ('52000000-0000-0000-0000-00000000000b', 'misc');

INSERT INTO habits (id, user_id, name, cadence, target_count, start_date) VALUES
    ('53000000-0000-0000-0000-00000000000a', '50000000-0000-0000-0000-00000000000a', 'Habit A', 'DAILY', 1, '2026-10-01'),
    ('53000000-0000-0000-0000-00000000000b', '50000000-0000-0000-0000-00000000000b', 'Habit B', 'DAILY', 1, '2026-10-01');
INSERT INTO habit_completions (habit_id, user_id, completed_on) VALUES
    ('53000000-0000-0000-0000-00000000000a', '50000000-0000-0000-0000-00000000000a', '2026-10-02'),
    ('53000000-0000-0000-0000-00000000000b', '50000000-0000-0000-0000-00000000000b', '2026-10-02');
INSERT INTO habit_pauses (habit_id, user_id, paused_on) VALUES
    ('53000000-0000-0000-0000-00000000000a', '50000000-0000-0000-0000-00000000000a', '2026-10-05'),
    ('53000000-0000-0000-0000-00000000000b', '50000000-0000-0000-0000-00000000000b', '2026-10-05');

INSERT INTO coping_strategies (user_id, title, details, sort_order) VALUES
    ('50000000-0000-0000-0000-00000000000a', 'Slow breathing', 'synthetic', 0),
    ('50000000-0000-0000-0000-00000000000b', 'Short walk', 'synthetic', 0);

-- ---- Baseline ----
-- Order: preferences/consent/sessions/check-ins/check-in tags/journal/journal tags/habits/pauses/completions/coping
SELECT test.assert_eq('ownership', 'baseline row counts for user A across all 11 owned tables',
    test.owned_rows('50000000-0000-0000-0000-00000000000a'), '1/1/2/1/2/1/1/1/1/1/1');
SELECT test.assert_eq('ownership', 'baseline row counts for user B across all 11 owned tables',
    test.owned_rows('50000000-0000-0000-0000-00000000000b'), '1/1/2/1/2/1/1/1/1/1/1');

-- ---- Identity constraints ----
SELECT test.expect_error('ownership', 'duplicate email rejected', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('own.a@example.test', 'Dup', 'x') $q$, '23505', 'users_email_key');
SELECT test.expect_error('ownership', 'upper-case email rejected (must be normalised by the app)', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('Mixed.Case@Example.Test', 'Case', 'x') $q$, '23514', 'users_email_normalized_chk');
SELECT test.expect_error('ownership', 'email without "@" rejected', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('not-an-email', 'NoAt', 'x') $q$, '23514', 'users_email_normalized_chk');
SELECT test.expect_error('ownership', 'blank display_name rejected', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('blank.name@example.test', '   ', 'x') $q$, '23514', 'users_display_name_chk');
SELECT test.expect_error('ownership', 'NULL password_hash rejected', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('nohash@example.test', 'NoHash', NULL) $q$, '23502');

-- ---- Preferences / consent ----
SELECT test.expect_error('ownership', 'second preferences row for the same user rejected (1:1)', $q$
    INSERT INTO user_preferences (user_id, theme) VALUES ('50000000-0000-0000-0000-00000000000a', 'light') $q$, '23505', 'user_preferences_pkey');
SELECT test.expect_error('ownership', 'invalid theme rejected', $q$
    UPDATE user_preferences SET theme = 'neon' WHERE user_id = '50000000-0000-0000-0000-00000000000a' $q$, '23514', 'user_preferences_theme_chk');
SELECT test.expect_error('ownership', 'unknown consent type rejected', $q$
    INSERT INTO consent_records (user_id, consent_type, granted, policy_version)
    VALUES ('50000000-0000-0000-0000-00000000000a', 'SELL_MY_DATA', TRUE, 'v1') $q$, '23514', 'consent_records_type_chk');

-- ---- Refresh sessions ----
SELECT test.expect_error('ownership', 'duplicate refresh token hash rejected', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at)
    VALUES ('50000000-0000-0000-0000-00000000000b', gen_random_uuid(), md5('A2') || md5('A2b'), now() + interval '1 day') $q$,
    '23505', 'refresh_sessions_token_hash_key');
SELECT test.expect_error('ownership', 'token hash that is not 64 lower-case hex characters rejected (no raw tokens stored)', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at)
    VALUES ('50000000-0000-0000-0000-00000000000a', gen_random_uuid(), 'raw-token-value', now() + interval '1 day') $q$,
    '23514', 'refresh_sessions_token_hash_chk');
SELECT test.expect_error('ownership', 'session expiring before it was issued rejected', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, issued_at, expires_at)
    VALUES ('50000000-0000-0000-0000-00000000000a', gen_random_uuid(), md5('X1') || md5('X1b'), now(), now() - interval '1 hour') $q$,
    '23514', 'refresh_sessions_expiry_chk');
SELECT test.expect_error('ownership', 'revoked_reason without revoked_at rejected', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at, revoked_reason)
    VALUES ('50000000-0000-0000-0000-00000000000a', gen_random_uuid(), md5('X2') || md5('X2b'), now() + interval '1 day', 'LOGOUT') $q$,
    '23514', 'refresh_sessions_revocation_chk');
SELECT test.expect_error('ownership', 'unknown revocation reason rejected', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at, revoked_at, revoked_reason)
    VALUES ('50000000-0000-0000-0000-00000000000a', gen_random_uuid(), md5('X3') || md5('X3b'), now() + interval '1 day', now(), 'BECAUSE') $q$,
    '23514', 'refresh_sessions_reason_chk');
SELECT test.expect_error('ownership', 'session for a nonexistent user rejected (FK)', $q$
    INSERT INTO refresh_sessions (user_id, family_id, token_hash, expires_at)
    VALUES ('50000000-0000-0000-0000-0000000000ff', gen_random_uuid(), md5('X4') || md5('X4b'), now() + interval '1 day') $q$,
    '23503', 'refresh_sessions_user_fk');

-- ---- Coping strategies ----
SELECT test.expect_error('ownership', 'blank coping strategy title rejected', $q$
    INSERT INTO coping_strategies (user_id, title) VALUES ('50000000-0000-0000-0000-00000000000a', '  ') $q$,
    '23514', 'coping_strategies_title_chk');
SELECT test.expect_error('ownership', 'negative sort_order rejected', $q$
    INSERT INTO coping_strategies (user_id, title, sort_order) VALUES ('50000000-0000-0000-0000-00000000000a', 'x', -1) $q$,
    '23514', 'coping_strategies_sort_chk');

-- ---- IDOR pattern: every repository query must filter by (id AND user_id) ----
SELECT test.assert_eq('ownership', 'IDOR: A cannot read B''s check-in by id when the query is user-scoped',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins WHERE id = '51000000-0000-0000-0000-00000000000b'
                    AND user_id = '50000000-0000-0000-0000-00000000000a' $q$), '0');
SELECT test.assert_eq('ownership', 'IDOR control: B can read B''s own check-in with the same scoped query',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins WHERE id = '51000000-0000-0000-0000-00000000000b'
                    AND user_id = '50000000-0000-0000-0000-00000000000b' $q$), '1');
SELECT test.assert_eq('ownership', 'IDOR: A cannot read B''s journal entry by id when user-scoped',
    test.scalar($q$ SELECT count(*) FROM journal_entries WHERE id = '52000000-0000-0000-0000-00000000000b'
                    AND user_id = '50000000-0000-0000-0000-00000000000a' $q$), '0');
SELECT test.assert_eq('ownership', 'IDOR: A cannot read B''s habit by id when user-scoped',
    test.scalar($q$ SELECT count(*) FROM habits WHERE id = '53000000-0000-0000-0000-00000000000b'
                    AND user_id = '50000000-0000-0000-0000-00000000000a' $q$), '0');
SELECT test.assert_eq('ownership', 'IDOR: a user-scoped UPDATE of B''s journal entry by A changes 0 rows',
    test.scalar($q$ WITH u AS (UPDATE journal_entries SET body = 'tampered'
                               WHERE id = '52000000-0000-0000-0000-00000000000b'
                                 AND user_id = '50000000-0000-0000-0000-00000000000a' RETURNING 1)
                    SELECT count(*) FROM u $q$), '0');
SELECT test.assert_eq('ownership', 'IDOR: a user-scoped DELETE of B''s habit by A removes 0 rows',
    test.scalar($q$ WITH d AS (DELETE FROM habits WHERE id = '53000000-0000-0000-0000-00000000000b'
                               AND user_id = '50000000-0000-0000-0000-00000000000a' RETURNING 1)
                    SELECT count(*) FROM d $q$), '0');
SELECT test.assert_eq('ownership', 'B''s data is unchanged after the attempted cross-user write/delete',
    test.owned_rows('50000000-0000-0000-0000-00000000000b'), '1/1/2/1/2/1/1/1/1/1/1');
SELECT test.assert_eq('ownership', 'B''s journal body is still the original text',
    test.scalar($q$ SELECT body FROM journal_entries WHERE id = '52000000-0000-0000-0000-00000000000b' $q$),
    'synthetic entry B');

-- ---- Token rotation chain: deleting the successor must not delete the predecessor ----
DELETE FROM refresh_sessions WHERE id = '5a000000-0000-0000-0000-0000000000a2';
SELECT test.assert_eq('ownership', 'deleting a successor session nulls replaced_by on the predecessor (ON DELETE SET NULL)',
    test.scalar($q$ SELECT count(*) || ' row, replaced_by is null=' || bool_and(replaced_by IS NULL)
                    FROM refresh_sessions WHERE id = '5a000000-0000-0000-0000-0000000000a1' $q$),
    '1 row, replaced_by is null=true');

-- ---- Account deletion: removing the user removes everything they own, and nothing else ----
DELETE FROM users WHERE id = '50000000-0000-0000-0000-00000000000a';

SELECT test.assert_eq('ownership', 'DELETE USER: all 11 owned tables are empty for the deleted user',
    test.owned_rows('50000000-0000-0000-0000-00000000000a'), '0/0/0/0/0/0/0/0/0/0/0');
SELECT test.assert_eq('ownership', 'DELETE USER: the user row itself is gone',
    test.scalar($q$ SELECT count(*) FROM users WHERE id = '50000000-0000-0000-0000-00000000000a' $q$), '0');
SELECT test.assert_eq('ownership', 'DELETE USER: no tag rows survive for the deleted user''s check-in or journal entry',
    test.scalar($q$ SELECT (SELECT count(*) FROM check_in_tags WHERE check_in_id = '51000000-0000-0000-0000-00000000000a')
                       || '/' || (SELECT count(*) FROM journal_entry_tags WHERE entry_id = '52000000-0000-0000-0000-00000000000a') $q$),
    '0/0');
SELECT test.assert_eq('ownership', 'DELETE USER: the other user''s data is completely unchanged',
    test.owned_rows('50000000-0000-0000-0000-00000000000b'), '1/1/2/1/2/1/1/1/1/1/1');
SELECT test.assert_eq('ownership', 'DELETE USER: the other user''s active session still exists',
    test.scalar($q$ SELECT count(*) FROM refresh_sessions WHERE user_id = '50000000-0000-0000-0000-00000000000b'
                    AND revoked_at IS NULL $q$), '1');

-- ---- Regression: blank-text checks must treat tabs/newlines as whitespace, not only spaces ----
SELECT test.expect_error('ownership', 'display_name of only tab/newline characters rejected', $q$
    INSERT INTO users (email, display_name, password_hash) VALUES ('ws.name@example.test', E'\t\n ', 'x') $q$, '23514', 'users_display_name_chk');
SELECT test.expect_error('ownership', 'coping strategy title of only tab/newline characters rejected', $q$
    INSERT INTO coping_strategies (user_id, title) VALUES ('50000000-0000-0000-0000-00000000000b', E'\t\n ') $q$, '23514', 'coping_strategies_title_chk');
