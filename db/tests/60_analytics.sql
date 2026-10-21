-- Suite: analytics  (representative analytics queries against a KNOWN fixture)
--
-- Expected values below were computed by hand from the fixture, independently of the queries:
--
--   date   mood stress energy sleep(min)   emotions
--   10-05   4     2      4      450        calm, hopeful
--   10-06   3     3      3      390        calm
--   10-07   5     1      5      480        joyful, calm
--   10-08   2     4      2      300        anxious
--   10-09   3     3      3      420        calm, tired
--   10-12   4     2      4      460        hopeful, calm
--   10-13   4     2      4      440        calm
--   10-14   1     5      1      240        anxious, sad          (+ ACTIVITY tag "calm" to prove tag_type filtering)
--   10-15   2     4      2      420        tired
--   10-18   5     1      5      500        joyful, hopeful
--   (no check-in on 10-10, 10-11, 10-16, 10-17)
--
--   Habit "Evening walk" (DAILY) completed: 10-05 10-07 10-09 10-10 10-11 10-12 10-13 10-18
--   Habit "Long run"  (WEEKLY, target 2) completed: 10-06 10-08 10-14

INSERT INTO users (id, email, display_name, password_hash, timezone) VALUES
    ('60000000-0000-0000-0000-00000000000a', 'analytics.a@example.test', 'Synthetic A', 'synthetic-not-a-real-hash', 'Asia/Kolkata'),
    ('60000000-0000-0000-0000-00000000000b', 'analytics.b@example.test', 'Synthetic Noise B', 'synthetic-not-a-real-hash', 'UTC'),
    ('60000000-0000-0000-0000-00000000000c', 'analytics.c@example.test', 'Synthetic Empty C', 'synthetic-not-a-real-hash', 'UTC');

-- created_at is noon local time, except 10-18 which is 00:30 local (= 19:00 UTC on 10-17).
INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes, created_at)
SELECT '60000000-0000-0000-0000-00000000000a', d, 'Asia/Kolkata', mood, stress, energy, sleep,
       (CASE WHEN d = DATE '2026-10-18' THEN d::timestamp + interval '30 minutes'
             ELSE d::timestamp + interval '12 hours' END) AT TIME ZONE 'Asia/Kolkata'
FROM (VALUES
    (DATE '2026-10-05', 4, 2, 4, 450), (DATE '2026-10-06', 3, 3, 3, 390), (DATE '2026-10-07', 5, 1, 5, 480),
    (DATE '2026-10-08', 2, 4, 2, 300), (DATE '2026-10-09', 3, 3, 3, 420), (DATE '2026-10-12', 4, 2, 4, 460),
    (DATE '2026-10-13', 4, 2, 4, 440), (DATE '2026-10-14', 1, 5, 1, 240), (DATE '2026-10-15', 2, 4, 2, 420),
    (DATE '2026-10-18', 5, 1, 5, 500)) AS f(d, mood, stress, energy, sleep);

INSERT INTO check_in_tags (check_in_id, tag_type, label)
SELECT c.id, t.tag_type, t.label
FROM (VALUES
    (DATE '2026-10-05', 'EMOTION', 'calm'),    (DATE '2026-10-05', 'EMOTION', 'hopeful'),
    (DATE '2026-10-06', 'EMOTION', 'calm'),
    (DATE '2026-10-07', 'EMOTION', 'joyful'),  (DATE '2026-10-07', 'EMOTION', 'calm'),
    (DATE '2026-10-08', 'EMOTION', 'anxious'),
    (DATE '2026-10-09', 'EMOTION', 'calm'),    (DATE '2026-10-09', 'EMOTION', 'tired'),
    (DATE '2026-10-12', 'EMOTION', 'hopeful'), (DATE '2026-10-12', 'EMOTION', 'calm'),
    (DATE '2026-10-13', 'EMOTION', 'calm'),
    (DATE '2026-10-14', 'EMOTION', 'anxious'), (DATE '2026-10-14', 'EMOTION', 'sad'), (DATE '2026-10-14', 'ACTIVITY', 'calm'),
    (DATE '2026-10-15', 'EMOTION', 'tired'),
    (DATE '2026-10-18', 'EMOTION', 'joyful'),  (DATE '2026-10-18', 'EMOTION', 'hopeful')) AS t(d, tag_type, label)
JOIN mood_check_ins c ON c.user_id = '60000000-0000-0000-0000-00000000000a' AND c.local_date = t.d;

INSERT INTO habits (id, user_id, name, cadence, target_count, start_date) VALUES
    ('63000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-00000000000a', 'Evening walk', 'DAILY', 1, '2026-10-05'),
    ('63000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-00000000000a', 'Long run', 'WEEKLY', 2, '2026-10-05');
INSERT INTO habit_completions (habit_id, user_id, completed_on)
SELECT h, '60000000-0000-0000-0000-00000000000a', d FROM (VALUES
    ('63000000-0000-0000-0000-000000000001'::uuid, DATE '2026-10-05'), ('63000000-0000-0000-0000-000000000001', DATE '2026-10-07'),
    ('63000000-0000-0000-0000-000000000001', DATE '2026-10-09'), ('63000000-0000-0000-0000-000000000001', DATE '2026-10-10'),
    ('63000000-0000-0000-0000-000000000001', DATE '2026-10-11'), ('63000000-0000-0000-0000-000000000001', DATE '2026-10-12'),
    ('63000000-0000-0000-0000-000000000001', DATE '2026-10-13'), ('63000000-0000-0000-0000-000000000001', DATE '2026-10-18'),
    ('63000000-0000-0000-0000-000000000002', DATE '2026-10-06'), ('63000000-0000-0000-0000-000000000002', DATE '2026-10-08'),
    ('63000000-0000-0000-0000-000000000002', DATE '2026-10-14')) AS x(h, d);

-- Noise: user B logs terrible moods on the same dates and completes a same-named habit. None of it may leak into A's results.
INSERT INTO mood_check_ins (user_id, local_date, timezone, mood_score, stress_score, energy_score, sleep_minutes) VALUES
    ('60000000-0000-0000-0000-00000000000b', '2026-10-05', 'UTC', 1, 5, 1, 100),
    ('60000000-0000-0000-0000-00000000000b', '2026-10-06', 'UTC', 1, 5, 1, 100),
    ('60000000-0000-0000-0000-00000000000b', '2026-10-07', 'UTC', 1, 5, 1, 100);
INSERT INTO check_in_tags (check_in_id, tag_type, label)
SELECT id, 'EMOTION', 'calm' FROM mood_check_ins WHERE user_id = '60000000-0000-0000-0000-00000000000b';
INSERT INTO habits (id, user_id, name, cadence, target_count, start_date) VALUES
    ('63000000-0000-0000-0000-0000000000b1', '60000000-0000-0000-0000-00000000000b', 'Evening walk', 'DAILY', 1, '2026-10-05');
INSERT INTO habit_completions (habit_id, user_id, completed_on) VALUES
    ('63000000-0000-0000-0000-0000000000b1', '60000000-0000-0000-0000-00000000000b', '2026-10-06'),
    ('63000000-0000-0000-0000-0000000000b1', '60000000-0000-0000-0000-00000000000b', '2026-10-08'),
    ('63000000-0000-0000-0000-0000000000b1', '60000000-0000-0000-0000-00000000000b', '2026-10-14');

-- ---- 0. Fixture sanity ----
SELECT test.assert_eq('analytics', 'fixture: A has 10 check-ins, 17 tags, 11 completions',
    test.scalar($q$ SELECT (SELECT count(*) FROM mood_check_ins WHERE user_id = '60000000-0000-0000-0000-00000000000a')
                       || '/' || (SELECT count(*) FROM check_in_tags t JOIN mood_check_ins c ON c.id = t.check_in_id
                                  WHERE c.user_id = '60000000-0000-0000-0000-00000000000a')
                       || '/' || (SELECT count(*) FROM habit_completions WHERE user_id = '60000000-0000-0000-0000-00000000000a') $q$),
    '10/17/11');

-- ---- 1. Weekly trend (ISO weeks, Monday start): n, avg mood, avg stress, avg energy, avg sleep minutes ----
SELECT test.assert_eq('analytics', 'weekly trend: two ISO weeks with exact averages',
    test.rows($q$
        SELECT date_trunc('week', local_date::timestamp)::date AS week_start, count(*) AS n,
               round(avg(mood_score), 2) AS mood, round(avg(stress_score), 2) AS stress,
               round(avg(energy_score), 2) AS energy, round(avg(sleep_minutes), 0) AS sleep_min
        FROM mood_check_ins
        WHERE user_id = '60000000-0000-0000-0000-00000000000a' AND local_date BETWEEN '2026-10-05' AND '2026-10-18'
        GROUP BY 1 ORDER BY 1 $q$),
    '(2026-10-05,5,3.40,2.60,3.40,408) | (2026-10-12,5,3.20,2.80,3.20,412)');

-- ---- 2. Monthly trend ----
SELECT test.assert_eq('analytics', 'monthly trend: October n/mood/stress/energy/sleep',
    test.rows($q$
        SELECT to_char(date_trunc('month', local_date::timestamp), 'YYYY-MM') AS month, count(*) AS n,
               round(avg(mood_score), 2), round(avg(stress_score), 2), round(avg(energy_score), 2), round(avg(sleep_minutes), 0)
        FROM mood_check_ins WHERE user_id = '60000000-0000-0000-0000-00000000000a'
        GROUP BY 1 ORDER BY 1 $q$),
    '(2026-10,10,3.30,2.70,3.30,410)');

-- ---- 3. Mood distribution (zero-filled 1..5) ----
SELECT test.assert_eq('analytics', 'mood distribution is zero-filled and sums to 10',
    test.rows($q$
        SELECT s.score, count(c.id) AS n
        FROM generate_series(1, 5) AS s(score)
        LEFT JOIN mood_check_ins c ON c.mood_score = s.score
             AND c.user_id = '60000000-0000-0000-0000-00000000000a' AND c.local_date BETWEEN '2026-10-05' AND '2026-10-18'
        GROUP BY s.score ORDER BY s.score $q$),
    '(1,1) | (2,2) | (3,2) | (4,3) | (5,2)');

-- ---- 4. Most frequent emotions (EMOTION tags only, ties broken alphabetically) ----
SELECT test.assert_eq('analytics', 'top 3 emotions; ACTIVITY tag "calm" is not counted and user B''s tags do not leak',
    test.rows($q$
        SELECT t.label, count(*) AS n
        FROM check_in_tags t JOIN mood_check_ins c ON c.id = t.check_in_id
        WHERE c.user_id = '60000000-0000-0000-0000-00000000000a' AND t.tag_type = 'EMOTION'
        GROUP BY t.label ORDER BY n DESC, t.label ASC LIMIT 3 $q$),
    '(calm,6) | (hopeful,3) | (anxious,2)');

-- ---- 5. Sleep duration vs mood (buckets: <6h, 6-<8h, >=8h) ----
SELECT test.assert_eq('analytics', 'sleep buckets: observation counts and average mood (observation, not causation)',
    test.rows($q$
        SELECT CASE WHEN sleep_minutes < 360 THEN '<6h' WHEN sleep_minutes < 480 THEN '6-8h' ELSE '8h+' END AS bucket,
               count(*) AS n, round(avg(mood_score), 2) AS avg_mood
        FROM mood_check_ins WHERE user_id = '60000000-0000-0000-0000-00000000000a'
        GROUP BY 1 ORDER BY min(sleep_minutes) $q$),
    '(<6h,2,1.50) | (6-8h,6,3.33) | (8h+,2,5.00)');

-- ---- 6. Habit completion vs mood (days with a check-in, split by whether "Evening walk" was completed) ----
SELECT test.assert_eq('analytics', 'habit vs mood: 6 completed days avg 4.17, 4 non-completed days avg 2.00; completions on days without a check-in are ignored',
    test.rows($q$
        SELECT (h.completed_on IS NOT NULL) AS habit_done, count(*) AS n, round(avg(c.mood_score), 2) AS avg_mood
        FROM mood_check_ins c
        LEFT JOIN habit_completions h ON h.user_id = c.user_id AND h.completed_on = c.local_date
             AND h.habit_id = '63000000-0000-0000-0000-000000000001'
        WHERE c.user_id = '60000000-0000-0000-0000-00000000000a' AND c.local_date BETWEEN '2026-10-05' AND '2026-10-18'
        GROUP BY 1 ORDER BY 1 DESC $q$),
    '(t,6,4.17) | (f,4,2.00)');

-- ---- 7. Check-in consistency / data coverage ----
SELECT test.assert_eq('analytics', 'coverage: 10 of 14 days logged = 71.4%',
    test.rows($q$
        SELECT count(*) AS days_logged, (DATE '2026-10-18' - DATE '2026-10-05' + 1) AS days_in_range,
               round(100.0 * count(*) / (DATE '2026-10-18' - DATE '2026-10-05' + 1), 1) AS pct
        FROM mood_check_ins
        WHERE user_id = '60000000-0000-0000-0000-00000000000a' AND local_date BETWEEN '2026-10-05' AND '2026-10-18' $q$),
    '(10,14,71.4)');

-- ---- 8. User-selected date-range comparison ----
SELECT test.assert_eq('analytics', 'range comparison: week of 10-05 vs week of 10-12 average mood',
    test.rows($q$
        SELECT round(avg(mood_score) FILTER (WHERE local_date BETWEEN '2026-10-05' AND '2026-10-11'), 2) AS range_1,
               round(avg(mood_score) FILTER (WHERE local_date BETWEEN '2026-10-12' AND '2026-10-18'), 2) AS range_2
        FROM mood_check_ins WHERE user_id = '60000000-0000-0000-0000-00000000000a' $q$),
    '(3.40,3.20)');

-- ---- 9. Habit streaks and weekly progress ----
SELECT test.assert_eq('analytics', 'longest daily streak for "Evening walk" is 5 (10-09 through 10-13)',
    test.scalar($q$
        SELECT max(len) FROM (
            SELECT count(*) AS len FROM (
                SELECT completed_on, completed_on - (row_number() OVER (ORDER BY completed_on))::int AS grp
                FROM habit_completions
                WHERE habit_id = '63000000-0000-0000-0000-000000000001' AND user_id = '60000000-0000-0000-0000-00000000000a') t
            GROUP BY grp) s $q$),
    '5');
SELECT test.assert_eq('analytics', 'streak ending on 2026-10-13 is 5',
    test.scalar($q$
        SELECT coalesce(max(len), 0) FROM (
            SELECT count(*) AS len, max(completed_on) AS last_day FROM (
                SELECT completed_on, completed_on - (row_number() OVER (ORDER BY completed_on))::int AS grp
                FROM habit_completions
                WHERE habit_id = '63000000-0000-0000-0000-000000000001' AND user_id = '60000000-0000-0000-0000-00000000000a'
                  AND completed_on <= DATE '2026-10-13') t
            GROUP BY grp) s WHERE last_day = DATE '2026-10-13' $q$),
    '5');
SELECT test.assert_eq('analytics', 'streak ending on 2026-10-18 is 1 (10-17 was missed)',
    test.scalar($q$
        SELECT coalesce(max(len), 0) FROM (
            SELECT count(*) AS len, max(completed_on) AS last_day FROM (
                SELECT completed_on, completed_on - (row_number() OVER (ORDER BY completed_on))::int AS grp
                FROM habit_completions
                WHERE habit_id = '63000000-0000-0000-0000-000000000001' AND user_id = '60000000-0000-0000-0000-00000000000a'
                  AND completed_on <= DATE '2026-10-18') t
            GROUP BY grp) s WHERE last_day = DATE '2026-10-18' $q$),
    '1');
SELECT test.assert_eq('analytics', 'streak ending on a day with no completion (2026-10-17) is 0, not an error',
    test.scalar($q$
        SELECT coalesce(max(len), 0) FROM (
            SELECT count(*) AS len, max(completed_on) AS last_day FROM (
                SELECT completed_on, completed_on - (row_number() OVER (ORDER BY completed_on))::int AS grp
                FROM habit_completions
                WHERE habit_id = '63000000-0000-0000-0000-000000000001' AND user_id = '60000000-0000-0000-0000-00000000000a'
                  AND completed_on <= DATE '2026-10-17') t
            GROUP BY grp) s WHERE last_day = DATE '2026-10-17' $q$),
    '0');
SELECT test.assert_eq('analytics', 'weekly habit "Long run" progress: week of 10-05 is 2/2 (met), week of 10-12 is 1/2',
    test.rows($q$
        SELECT date_trunc('week', c.completed_on::timestamp)::date AS week_start, count(*) AS done, h.target_count
        FROM habit_completions c JOIN habits h ON h.id = c.habit_id AND h.user_id = c.user_id
        WHERE c.habit_id = '63000000-0000-0000-0000-000000000002' AND c.user_id = '60000000-0000-0000-0000-00000000000a'
        GROUP BY 1, h.target_count ORDER BY 1 $q$),
    '(2026-10-05,2,2) | (2026-10-12,1,2)');

-- ---- 10. Timezone handling: local_date is the authoritative day, UTC date can differ ----
SELECT test.assert_eq('analytics', 'every check-in''s local_date equals its created_at converted to the stored timezone',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins
                    WHERE user_id = '60000000-0000-0000-0000-00000000000a'
                      AND (created_at AT TIME ZONE timezone)::date = local_date $q$), '10');
SELECT test.assert_eq('analytics', 'exactly one check-in (00:30 IST on 10-18) would land on a different day if bucketed by UTC',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins
                    WHERE user_id = '60000000-0000-0000-0000-00000000000a'
                      AND (created_at AT TIME ZONE 'UTC')::date <> local_date $q$), '1');

-- ---- 11. Scoping and empty/incomplete datasets ----
SELECT test.assert_eq('analytics', 'noise exists: an UNSCOPED count over the range sees 13 check-ins (10 for A + 3 for B)',
    test.scalar($q$ SELECT count(*) FROM mood_check_ins WHERE local_date BETWEEN '2026-10-05' AND '2026-10-18' $q$), '13');
SELECT test.assert_eq('analytics', 'scoped average mood for A ignores B (3.30, not dragged down by B''s 1s)',
    test.scalar($q$ SELECT round(avg(mood_score), 2) FROM mood_check_ins
                    WHERE user_id = '60000000-0000-0000-0000-00000000000a' AND local_date BETWEEN '2026-10-05' AND '2026-10-18' $q$),
    '3.30');
SELECT test.assert_eq('analytics', 'empty user: average mood is NULL (no division-by-zero error)',
    test.scalar($q$ SELECT coalesce(round(avg(mood_score), 2)::text, 'NULL') FROM mood_check_ins
                    WHERE user_id = '60000000-0000-0000-0000-00000000000c' $q$), 'NULL');
SELECT test.assert_eq('analytics', 'empty user: coverage is 0 of 14 days = 0.0%',
    test.rows($q$ SELECT count(*), round(100.0 * count(*) / 14, 1) FROM mood_check_ins
                  WHERE user_id = '60000000-0000-0000-0000-00000000000c' AND local_date BETWEEN '2026-10-05' AND '2026-10-18' $q$),
    '(0,0.0)');
SELECT test.assert_eq('analytics', 'empty user: weekly trend returns no rows (the API must render an honest empty state)',
    test.rows($q$ SELECT date_trunc('week', local_date::timestamp)::date, count(*) FROM mood_check_ins
                  WHERE user_id = '60000000-0000-0000-0000-00000000000c' GROUP BY 1 $q$),
    '<no rows>');
SELECT test.assert_eq('analytics', 'empty user: mood distribution is still zero-filled 1..5',
    test.rows($q$ SELECT s.score, count(c.id) FROM generate_series(1, 5) s(score)
                  LEFT JOIN mood_check_ins c ON c.mood_score = s.score AND c.user_id = '60000000-0000-0000-0000-00000000000c'
                  GROUP BY s.score ORDER BY s.score $q$),
    '(1,0) | (2,0) | (3,0) | (4,0) | (5,0)');
SELECT test.assert_eq('analytics', 'user B''s habit-vs-mood result is built only from B''s own data (1 completed day: 10-06, mood 1)',
    test.rows($q$
        SELECT (h.completed_on IS NOT NULL) AS habit_done, count(*), round(avg(c.mood_score), 2)
        FROM mood_check_ins c
        LEFT JOIN habit_completions h ON h.user_id = c.user_id AND h.completed_on = c.local_date
             AND h.habit_id = '63000000-0000-0000-0000-0000000000b1'
        WHERE c.user_id = '60000000-0000-0000-0000-00000000000b' GROUP BY 1 ORDER BY 1 DESC $q$),
    '(t,1,1.00) | (f,2,1.00)');
