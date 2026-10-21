-- Suite: habits  (constraints for habits / habit_pauses / habit_completions)

INSERT INTO users (id, email, display_name, password_hash, timezone) VALUES
    ('40000000-0000-0000-0000-00000000000a', 'habit.a@example.test', 'Synthetic A', 'synthetic-not-a-real-hash', 'UTC'),
    ('40000000-0000-0000-0000-00000000000b', 'habit.b@example.test', 'Synthetic B', 'synthetic-not-a-real-hash', 'UTC');

-- ---- Habit definitions ----
SELECT test.expect_ok('habits', 'DAILY habit with target_count 1 accepted', $q$
    INSERT INTO habits (id, user_id, name, cadence, target_count, start_date)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', 'Evening walk', 'DAILY', 1, '2026-10-01') $q$);
SELECT test.expect_ok('habits', 'WEEKLY habit with target_count 3 accepted', $q$
    INSERT INTO habits (id, user_id, name, cadence, target_count, start_date, end_date)
    VALUES ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-00000000000a', 'Long run', 'WEEKLY', 3, '2026-10-01', '2026-12-31') $q$);
SELECT test.expect_error('habits', 'DAILY habit with target_count 2 rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'Bad daily', 'DAILY', 2, '2026-10-01') $q$, '23514', 'habits_cadence_target_chk');
SELECT test.expect_error('habits', 'WEEKLY habit with target_count 0 rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'Bad weekly 0', 'WEEKLY', 0, '2026-10-01') $q$, '23514', 'habits_cadence_target_chk');
SELECT test.expect_error('habits', 'WEEKLY habit with target_count 8 rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'Bad weekly 8', 'WEEKLY', 8, '2026-10-01') $q$, '23514', 'habits_cadence_target_chk');
SELECT test.expect_error('habits', 'unknown cadence rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'Bad cadence', 'MONTHLY', 1, '2026-10-01') $q$, '23514', 'habits_cadence_chk');
SELECT test.expect_error('habits', 'end_date before start_date rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date, end_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'Bad dates', 'DAILY', 1, '2026-10-10', '2026-10-09') $q$, '23514', 'habits_dates_chk');
SELECT test.expect_ok('habits', 'end_date equal to start_date accepted (one-day habit)', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date, end_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', 'One day only', 'DAILY', 1, '2026-10-10', '2026-10-10') $q$);
SELECT test.expect_error('habits', 'blank habit name rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', '   ', 'DAILY', 1, '2026-10-01') $q$, '23514', 'habits_name_chk');
SELECT test.expect_error('habits', 'duplicate habit name for the same user rejected (case/whitespace-insensitive)', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', '  EVENING WALK ', 'DAILY', 1, '2026-10-01') $q$, '23505', 'habits_user_name_uq');
SELECT test.expect_ok('habits', 'the same habit name for a DIFFERENT user is allowed', $q$
    INSERT INTO habits (id, user_id, name, cadence, target_count, start_date)
    VALUES ('41000000-0000-0000-0000-00000000000b', '40000000-0000-0000-0000-00000000000b', 'Evening walk', 'DAILY', 1, '2026-10-01') $q$);
SELECT test.expect_error('habits', 'habit for a nonexistent user rejected (FK)', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-0000000000ff', 'Ghost', 'DAILY', 1, '2026-10-01') $q$, '23503', 'habits_user_fk');

-- ---- Completions ----
SELECT test.expect_ok('habits', 'first completion accepted', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-05') $q$);
SELECT test.expect_error('habits', 'duplicate completion (same habit + date) rejected', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-05') $q$,
    '23505', 'habit_completions_habit_date_key');
SELECT test.expect_ok('habits', 'completion on a different date accepted', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-06') $q$);
SELECT test.expect_ok('habits', 'idempotent completion (ON CONFLICT DO NOTHING) does not error on a repeat', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-05')
    ON CONFLICT (habit_id, completed_on) DO NOTHING $q$);
SELECT test.assert_eq('habits', 'after the repeat there are still exactly 2 completions for the habit',
    test.scalar($q$ SELECT count(*) FROM habit_completions WHERE habit_id = '41000000-0000-0000-0000-000000000001' $q$), '2');
SELECT test.expect_ok('habits', 'the other user''s habit can be completed on the same date', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-00000000000b', '40000000-0000-0000-0000-00000000000b', '2026-10-05') $q$);

-- ---- Ownership: a completion/pause must belong to the habit's owner ----
SELECT test.expect_error('habits', 'OWNERSHIP: completion naming user B for user A''s habit rejected (composite FK)', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000b', '2026-10-07') $q$,
    '23503', 'habit_completions_habit_owner_fk');
SELECT test.expect_error('habits', 'completion for a nonexistent habit rejected', $q$
    INSERT INTO habit_completions (habit_id, user_id, completed_on)
    VALUES ('41000000-0000-0000-0000-0000000000ff', '40000000-0000-0000-0000-00000000000a', '2026-10-07') $q$,
    '23503', 'habit_completions_habit_owner_fk');
SELECT test.expect_error('habits', 'OWNERSHIP: pause naming user B for user A''s habit rejected (composite FK)', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000b', '2026-10-07') $q$,
    '23503', 'habit_pauses_habit_owner_fk');

-- ---- Pauses ----
SELECT test.expect_ok('habits', 'open pause accepted', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-10') $q$);
SELECT test.expect_error('habits', 'a second OPEN pause for the same habit rejected', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-11') $q$,
    '23505', 'habit_pauses_one_open_idx');
SELECT test.expect_ok('habits', 'resuming (setting resumed_on) accepted', $q$
    UPDATE habit_pauses SET resumed_on = '2026-10-12'
    WHERE habit_id = '41000000-0000-0000-0000-000000000001' AND resumed_on IS NULL $q$);
SELECT test.expect_ok('habits', 'a new open pause after a resume is accepted', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on)
    VALUES ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-00000000000a', '2026-10-20') $q$);
SELECT test.expect_error('habits', 'pause with resumed_on equal to paused_on rejected', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on, resumed_on)
    VALUES ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-00000000000a', '2026-10-10', '2026-10-10') $q$,
    '23514', 'habit_pauses_range_chk');
SELECT test.expect_error('habits', 'pause with resumed_on before paused_on rejected', $q$
    INSERT INTO habit_pauses (habit_id, user_id, paused_on, resumed_on)
    VALUES ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-00000000000a', '2026-10-10', '2026-10-09') $q$,
    '23514', 'habit_pauses_range_chk');

-- ---- Deletion: removing a habit removes its history only ----
INSERT INTO habit_completions (habit_id, user_id, completed_on)
VALUES ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-00000000000a', '2026-10-06');
DELETE FROM habits WHERE id = '41000000-0000-0000-0000-000000000001';
SELECT test.assert_eq('habits', 'deleting a habit cascades to its completions and pauses',
    test.scalar($q$ SELECT (SELECT count(*) FROM habit_completions WHERE habit_id = '41000000-0000-0000-0000-000000000001')
                       || '/' || (SELECT count(*) FROM habit_pauses WHERE habit_id = '41000000-0000-0000-0000-000000000001') $q$),
    '0/0');
SELECT test.assert_eq('habits', 'deleting a habit leaves other habits'' completions untouched',
    test.scalar($q$ SELECT count(*) FROM habit_completions WHERE habit_id = '41000000-0000-0000-0000-000000000002' $q$), '1');
SELECT test.assert_eq('habits', 'deleting a habit leaves other users'' habits and completions untouched',
    test.scalar($q$ SELECT (SELECT count(*) FROM habits WHERE user_id = '40000000-0000-0000-0000-00000000000b')
                       || '/' || (SELECT count(*) FROM habit_completions WHERE user_id = '40000000-0000-0000-0000-00000000000b') $q$),
    '1/1');

-- ---- Regression: blank-text checks must treat tabs/newlines as whitespace, not only spaces ----
SELECT test.expect_error('habits', 'habit name of only tab/newline characters rejected', $q$
    INSERT INTO habits (user_id, name, cadence, target_count, start_date)
    VALUES ('40000000-0000-0000-0000-00000000000a', E'\t\n ', 'DAILY', 1, '2026-10-01') $q$, '23514', 'habits_name_chk');
