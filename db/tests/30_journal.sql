-- Suite: journal  (constraints plus representative queries on known synthetic data)

INSERT INTO users (id, email, display_name, password_hash, timezone) VALUES
    ('30000000-0000-0000-0000-00000000000a', 'journal.a@example.test', 'Synthetic A', 'synthetic-not-a-real-hash', 'UTC'),
    ('30000000-0000-0000-0000-00000000000b', 'journal.b@example.test', 'Synthetic B', 'synthetic-not-a-real-hash', 'UTC');

-- User A: five entries, ids end in 01..05, created one per odd October day.
INSERT INTO journal_entries (id, user_id, entry_date, title, body, prompt_text, created_at) VALUES
 ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-00000000000a', '2026-10-01', 'Quiet morning',
  'Walked by the river and felt calm.', NULL, '2026-10-01 10:00:00+00'),
 ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-00000000000a', '2026-10-03', 'Work pressure',
  'Deadline stress at work; hard to focus.', NULL, '2026-10-03 10:00:00+00'),
 ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-00000000000a', '2026-10-05', NULL,
  'Grateful for a long call with a friend.', NULL, '2026-10-05 10:00:00+00'),
 ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-00000000000a', '2026-10-07', 'Reflection',
  'Noticed that short walks help me think clearly.', 'What helped today?', '2026-10-07 10:00:00+00'),
 ('31000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-00000000000a', '2026-10-09', NULL,
  'Slept badly, felt tired but finished the report.', NULL, '2026-10-09 10:00:00+00');

INSERT INTO journal_entry_tags (entry_id, tag) VALUES
 ('31000000-0000-0000-0000-000000000001', 'nature'),
 ('31000000-0000-0000-0000-000000000001', 'calm'),
 ('31000000-0000-0000-0000-000000000002', 'work'),
 ('31000000-0000-0000-0000-000000000003', 'gratitude'),
 ('31000000-0000-0000-0000-000000000003', 'friends'),
 ('31000000-0000-0000-0000-000000000004', 'nature'),
 ('31000000-0000-0000-0000-000000000005', 'work');

-- User B: one entry sharing words with A's ("river") plus a word only B uses ("zebra"), and a tag A also uses.
INSERT INTO journal_entries (id, user_id, entry_date, title, body, created_at) VALUES
 ('32000000-0000-0000-0000-0000000000b1', '30000000-0000-0000-0000-00000000000b', '2026-10-02', 'Private',
  'A private note about the river that only B wrote. Also a zebra.', '2026-10-02 10:00:00+00');
INSERT INTO journal_entry_tags (entry_id, tag) VALUES ('32000000-0000-0000-0000-0000000000b1', 'nature');

-- ---- Pagination (newest first, keyset) ----
SELECT test.assert_eq('journal', 'page 1 (limit 2) returns the two newest entries in order',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                  ORDER BY created_at DESC, id DESC LIMIT 2 $q$),
    '(05) | (04)');

SELECT test.assert_eq('journal', 'page 2 via keyset cursor continues without overlap',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND (created_at, id) < ('2026-10-07 10:00:00+00'::timestamptz, '31000000-0000-0000-0000-000000000004'::uuid)
                  ORDER BY created_at DESC, id DESC LIMIT 2 $q$),
    '(03) | (02)');

SELECT test.assert_eq('journal', 'page 3 returns the single remaining entry',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND (created_at, id) < ('2026-10-03 10:00:00+00'::timestamptz, '31000000-0000-0000-0000-000000000002'::uuid)
                  ORDER BY created_at DESC, id DESC LIMIT 2 $q$),
    '(01)');

-- ---- Date-range filter ----
SELECT test.assert_eq('journal', 'date range 2026-10-03..2026-10-07 (inclusive) returns 3 entries',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND entry_date BETWEEN '2026-10-03' AND '2026-10-07' ORDER BY entry_date $q$),
    '(02) | (03) | (04)');

-- ---- Full-text search (always scoped by user_id) ----
SELECT test.assert_eq('journal', 'search "river" as A finds only A''s entry',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'river') $q$),
    '(01)');
SELECT test.assert_eq('journal', 'search "river" as B finds only B''s entry',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000b'
                    AND search_vector @@ websearch_to_tsquery('simple', 'river') $q$),
    '(b1)');
SELECT test.assert_eq('journal', 'unscoped "river" search WOULD match both users (proves the scoping test is meaningful)',
    test.scalar($q$ SELECT count(*) FROM journal_entries WHERE search_vector @@ websearch_to_tsquery('simple', 'river') $q$),
    '2');
SELECT test.assert_eq('journal', 'search for a word only B used ("zebra") returns nothing for A',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'zebra') $q$),
    '<no rows>');
SELECT test.assert_eq('journal', 'search matches the title ("quiet")',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'quiet') $q$),
    '(01)');
SELECT test.assert_eq('journal', 'search "work" finds only the entry containing the word',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'work') $q$),
    '(02)');
SELECT test.assert_eq('journal', 'multi-word search is AND ("work report" matches no single entry)',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'work report') $q$),
    '<no rows>');

-- ---- Tag filter ----
SELECT test.assert_eq('journal', 'tag filter "nature" for A returns A''s two tagged entries only',
    test.rows($q$ SELECT right(e.id::text, 2) FROM journal_entries e
                  JOIN journal_entry_tags t ON t.entry_id = e.id
                  WHERE e.user_id = '30000000-0000-0000-0000-00000000000a' AND t.tag = 'nature'
                  ORDER BY e.created_at $q$),
    '(01) | (04)');
SELECT test.assert_eq('journal', 'unscoped tag "nature" would return 3 rows (B also uses it)',
    test.scalar($q$ SELECT count(*) FROM journal_entry_tags WHERE tag = 'nature' $q$), '3');

-- ---- Generated search_vector ----
UPDATE journal_entries SET body = body || ' Tried a short meditation.' WHERE id = '31000000-0000-0000-0000-000000000005';
SELECT test.assert_eq('journal', 'search_vector is regenerated after the body is edited',
    test.rows($q$ SELECT right(id::text, 2) FROM journal_entries
                  WHERE user_id = '30000000-0000-0000-0000-00000000000a'
                    AND search_vector @@ websearch_to_tsquery('simple', 'meditation') $q$),
    '(05)');
SELECT test.expect_error('journal', 'search_vector cannot be written directly (generated column)', $q$
    UPDATE journal_entries SET search_vector = to_tsvector('simple', 'tamper')
    WHERE id = '31000000-0000-0000-0000-000000000001' $q$, '428C9');

-- ---- Constraints ----
SELECT test.expect_error('journal', 'empty body rejected', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', '') $q$, '23514', 'journal_entries_body_chk');
SELECT test.expect_error('journal', 'whitespace-only body rejected', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', E'  \n  ') $q$, '23514', 'journal_entries_body_chk');
SELECT test.expect_error('journal', 'body of 10001 characters rejected', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', repeat('x', 10001)) $q$, '23514', 'journal_entries_body_chk');
SELECT test.expect_ok('journal', 'body of exactly 10000 characters accepted', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-00000000000b', '2026-10-10', repeat('x', 10000)) $q$);
SELECT test.expect_error('journal', 'NULL body rejected (NOT NULL)', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', NULL) $q$, '23502');
SELECT test.expect_error('journal', 'title of 201 characters rejected (VARCHAR(200) length limit)', $q$
    INSERT INTO journal_entries (user_id, entry_date, title, body)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', repeat('t', 201), 'x') $q$, '22001');
SELECT test.expect_error('journal', 'prompt_text of 301 characters rejected (VARCHAR(300) length limit)', $q$
    INSERT INTO journal_entries (user_id, entry_date, body, prompt_text)
    VALUES ('30000000-0000-0000-0000-00000000000a', '2026-10-10', 'x', repeat('p', 301)) $q$, '22001');
SELECT test.expect_error('journal', 'entry for a nonexistent user rejected (FK)', $q$
    INSERT INTO journal_entries (user_id, entry_date, body)
    VALUES ('30000000-0000-0000-0000-0000000000ff', '2026-10-10', 'x') $q$, '23503', 'journal_entries_user_fk');
SELECT test.expect_error('journal', 'upper-case tag rejected', $q$
    INSERT INTO journal_entry_tags (entry_id, tag) VALUES ('31000000-0000-0000-0000-000000000001', 'Nature') $q$,
    '23514', 'journal_entry_tags_tag_chk');
SELECT test.expect_error('journal', 'duplicate tag on one entry rejected', $q$
    INSERT INTO journal_entry_tags (entry_id, tag) VALUES ('31000000-0000-0000-0000-000000000001', 'nature') $q$,
    '23505', 'journal_entry_tags_pkey');
SELECT test.expect_error('journal', 'tag on a nonexistent entry rejected (FK)', $q$
    INSERT INTO journal_entry_tags (entry_id, tag) VALUES ('31000000-0000-0000-0000-0000000000ff', 'x') $q$,
    '23503', 'journal_entry_tags_entry_fk');

-- ---- Deletion ----
DELETE FROM journal_entries WHERE id = '31000000-0000-0000-0000-000000000003';
SELECT test.assert_eq('journal', 'deleting an entry cascades to its tags',
    test.scalar($q$ SELECT count(*) FROM journal_entry_tags WHERE entry_id = '31000000-0000-0000-0000-000000000003' $q$), '0');
SELECT test.assert_eq('journal', 'deleting one entry leaves the user''s other entries and tags intact',
    test.scalar($q$ SELECT (SELECT count(*) FROM journal_entries WHERE user_id = '30000000-0000-0000-0000-00000000000a')
                        || '/' || (SELECT count(*) FROM journal_entry_tags t JOIN journal_entries e ON e.id = t.entry_id
                                   WHERE e.user_id = '30000000-0000-0000-0000-00000000000a') $q$),
    '4/5');
