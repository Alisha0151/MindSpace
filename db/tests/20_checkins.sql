-- Suite: checkins  (constraints and behaviour of mood_check_ins / check_in_tags)

INSERT INTO users (id, email, display_name, password_hash, timezone) VALUES
    ('20000000-0000-0000-0000-00000000000a', 'checkin.a@example.test', 'Synthetic A', 'synthetic-not-a-real-hash', 'Asia/Kolkata'),
    ('20000000-0000-0000-0000-00000000000b', 'checkin.b@example.test', 'Synthetic B', 'synthetic-not-a-real-hash', 'UTC');

SELECT test.expect_ok('checkins', 'valid check-in is accepted', $q$
    INSERT INTO mood_check_ins (id, user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, sleep_quality, notes)
    VALUES ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-00000000000a',
            '2026-10-01', 'Asia/Kolkata', 4, 2, 3, 450, 4, 'synthetic note') $q$);

SELECT test.expect_error('checkins', 'duplicate check-in for same user + local date is rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-10-01', 'Asia/Kolkata', 3, 3, 3, 400) $q$,
    '23505', 'mood_check_ins_user_date_key');

SELECT test.expect_ok('checkins', 'same local date for a DIFFERENT user is allowed', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000b', '2026-10-01', 'UTC', 3, 3, 3, 400) $q$);

SELECT test.expect_ok('checkins', 'different local date for the same user is allowed', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-10-02', 'Asia/Kolkata', 3, 3, 3, 400) $q$);

SELECT test.expect_error('checkins', 'mood_score 0 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 0, 3, 3, 400) $q$, '23514', 'mood_check_ins_mood_chk');
SELECT test.expect_error('checkins', 'mood_score 6 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 6, 3, 3, 400) $q$, '23514', 'mood_check_ins_mood_chk');
SELECT test.expect_error('checkins', 'stress_score 0 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 3, 0, 3, 400) $q$, '23514', 'mood_check_ins_stress_chk');
SELECT test.expect_error('checkins', 'energy_score 6 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 3, 3, 6, 400) $q$, '23514', 'mood_check_ins_energy_chk');
SELECT test.expect_error('checkins', 'sleep_minutes -1 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 3, 3, 3, -1) $q$, '23514', 'mood_check_ins_sleep_minutes_chk');
SELECT test.expect_error('checkins', 'sleep_minutes 1441 (more than 24h) rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 3, 3, 3, 1441) $q$, '23514', 'mood_check_ins_sleep_minutes_chk');
SELECT test.expect_error('checkins', 'sleep_quality 6 rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, sleep_quality)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-01', 'UTC', 3, 3, 3, 400, 6) $q$, '23514', 'mood_check_ins_sleep_quality_chk');
SELECT test.expect_ok('checkins', 'sleep_quality may be NULL (optional field)', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, sleep_quality)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-02', 'UTC', 3, 3, 3, 400, NULL) $q$);
SELECT test.expect_ok('checkins', 'notes of exactly 2000 characters accepted', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, notes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-03', 'UTC', 3, 3, 3, 400, repeat('x', 2000)) $q$);
SELECT test.expect_error('checkins', 'notes of 2001 characters rejected', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, notes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-04', 'UTC', 3, 3, 3, 400, repeat('x', 2001)) $q$, '23514', 'mood_check_ins_notes_chk');
SELECT test.expect_error('checkins', 'NULL mood_score rejected (NOT NULL)', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-05', 'UTC', NULL, 3, 3, 400) $q$, '23502');
SELECT test.expect_error('checkins', 'check-in for a nonexistent user rejected (FK)', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-0000000000ff', '2026-11-06', 'UTC', 3, 3, 3, 400) $q$, '23503', 'mood_check_ins_user_fk');

-- Idempotent upsert: the pattern the application uses for "create or update today's check-in".
SELECT test.expect_ok('checkins', 'upsert (first call) inserts', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-10', 'Asia/Kolkata', 2, 3, 3, 400)
    ON CONFLICT (user_id, local_date) DO UPDATE SET mood_score = EXCLUDED.mood_score $q$);
SELECT test.expect_ok('checkins', 'upsert (second call, same date) updates instead of failing', $q$
    INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes)
    VALUES ('20000000-0000-0000-0000-00000000000a', '2026-11-10', 'Asia/Kolkata', 5, 3, 3, 400)
    ON CONFLICT (user_id, local_date) DO UPDATE SET mood_score = EXCLUDED.mood_score $q$);
SELECT test.assert_eq('checkins', 'after two upserts exactly one row exists with the latest value',
    test.scalar($q$ SELECT count(*) || ' row(s), mood=' || max(mood_score) FROM mood_check_ins
                    WHERE user_id = '20000000-0000-0000-0000-00000000000a' AND local_date = '2026-11-10' $q$),
    '1 row(s), mood=5');

-- Trigger behaviour.
DO $t$
DECLARE
    c1 TIMESTAMPTZ; c2 TIMESTAMPTZ; u2 TIMESTAMPTZ;
BEGIN
    SELECT created_at INTO c1 FROM mood_check_ins WHERE id = '21000000-0000-0000-0000-000000000001';
    PERFORM pg_sleep(0.05);
    UPDATE mood_check_ins SET mood_score = 2 WHERE id = '21000000-0000-0000-0000-000000000001';
    SELECT created_at, updated_at INTO c2, u2 FROM mood_check_ins WHERE id = '21000000-0000-0000-0000-000000000001';
    PERFORM test.record('checkins', 'updated_at advances on UPDATE while created_at is unchanged',
        u2 > c2 AND c2 = c1, 'created_at=' || c2 || ' updated_at=' || u2);
END
$t$;

-- Tags.
SELECT test.expect_ok('checkins', 'emotion tags accepted', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label) VALUES
      ('21000000-0000-0000-0000-000000000001', 'EMOTION', 'calm'),
      ('21000000-0000-0000-0000-000000000001', 'EMOTION', 'hopeful'),
      ('21000000-0000-0000-0000-000000000001', 'ACTIVITY', 'rest') $q$);
SELECT test.expect_ok('checkins', 'same label under a different tag_type is allowed', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label) VALUES
      ('21000000-0000-0000-0000-000000000001', 'CONTEXT', 'rest') $q$);
SELECT test.expect_error('checkins', 'duplicate tag on the same check-in rejected', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label)
    VALUES ('21000000-0000-0000-0000-000000000001', 'EMOTION', 'calm') $q$, '23505', 'check_in_tags_pkey');
SELECT test.expect_error('checkins', 'upper-case tag label rejected', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label)
    VALUES ('21000000-0000-0000-0000-000000000001', 'EMOTION', 'Calm') $q$, '23514', 'check_in_tags_label_chk');
SELECT test.expect_error('checkins', 'empty tag label rejected', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label)
    VALUES ('21000000-0000-0000-0000-000000000001', 'EMOTION', '') $q$, '23514', 'check_in_tags_label_chk');
SELECT test.expect_error('checkins', 'unknown tag_type rejected', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label)
    VALUES ('21000000-0000-0000-0000-000000000001', 'DIAGNOSIS', 'x') $q$, '23514', 'check_in_tags_type_chk');
SELECT test.expect_error('checkins', 'tag for a nonexistent check-in rejected (FK)', $q$
    INSERT INTO check_in_tags (check_in_id, tag_type, label)
    VALUES ('21000000-0000-0000-0000-0000000000ff', 'EMOTION', 'calm') $q$, '23503', 'check_in_tags_check_in_fk');

-- Deleting a check-in removes its tags and nothing else.
DELETE FROM mood_check_ins WHERE id = '21000000-0000-0000-0000-000000000001';
SELECT test.assert_eq('checkins', 'deleting a check-in cascades to its tags',
    test.scalar($q$ SELECT count(*) FROM check_in_tags WHERE check_in_id = '21000000-0000-0000-0000-000000000001' $q$), '0');
SELECT test.assert_eq('checkins', 'deleting a check-in leaves the same user''s other check-ins intact',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins WHERE user_id = '20000000-0000-0000-0000-00000000000a'
                    AND local_date = '2026-10-02' $q$), '1');
