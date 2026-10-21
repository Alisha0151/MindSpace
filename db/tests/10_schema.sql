-- Suite: schema  (structural assertions over the migrated database)

SELECT test.assert_eq('schema', 'exactly 12 application tables exist',
    test.scalar($q$ SELECT count(*) FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_type = 'BASE TABLE' $q$),
    '12');

SELECT test.assert_eq('schema', 'application table names are exactly the expected set',
    test.scalar($q$ SELECT string_agg(table_name, ',' ORDER BY table_name COLLATE "C")
                    FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_type = 'BASE TABLE' $q$),
    'check_in_tags,consent_records,coping_strategies,habit_completions,habit_pauses,habits,journal_entries,journal_entry_tags,mood_check_ins,refresh_sessions,user_preferences,users');

SELECT test.assert_eq('schema', 'every table has a primary key',
    test.scalar($q$ SELECT count(*) FROM information_schema.tables t
                    WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                      AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints c
                                      WHERE c.table_schema = 'public' AND c.table_name = t.table_name
                                        AND c.constraint_type = 'PRIMARY KEY') $q$),
    '0');

SELECT test.assert_eq('schema', 'every "id" column is uuid',
    test.scalar($q$ SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND column_name = 'id' AND data_type <> 'uuid' $q$),
    '0');

SELECT test.assert_eq('schema', 'all *_at instants are timestamptz (none are timestamp without time zone)',
    test.scalar($q$ SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND column_name LIKE '%\_at'
                      AND data_type <> 'timestamp with time zone' $q$),
    '0');

SELECT test.assert_eq('schema', 'timestamptz columns actually exist (19 *_at columns)',
    test.scalar($q$ SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND column_name LIKE '%\_at'
                      AND data_type = 'timestamp with time zone' $q$),
    '19');

SELECT test.assert_eq('schema', 'tables without a direct user_id are only users + the two tag tables',
    test.scalar($q$ SELECT string_agg(t.table_name, ',' ORDER BY t.table_name COLLATE "C")
                    FROM information_schema.tables t
                    WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                      AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
                                      WHERE c.table_schema = 'public' AND c.table_name = t.table_name
                                        AND c.column_name = 'user_id') $q$),
    'check_in_tags,journal_entry_tags,users');

SELECT test.assert_eq('schema', 'every user_id column is NOT NULL',
    test.scalar($q$ SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND column_name = 'user_id' AND is_nullable = 'YES' $q$),
    '0');

SELECT test.assert_eq('schema', 'every user_id column has a foreign key to users(id)',
    test.scalar($q$ SELECT count(*) FROM information_schema.columns c
                    WHERE c.table_schema = 'public' AND c.column_name = 'user_id'
                      AND NOT EXISTS (
                        SELECT 1 FROM pg_constraint k
                        WHERE k.contype = 'f'
                          AND k.conrelid = format('public.%I', c.table_name)::regclass
                          AND k.confrelid = 'public.users'::regclass
                          AND k.conkey = ARRAY[(SELECT a.attnum FROM pg_attribute a
                                                WHERE a.attrelid = k.conrelid AND a.attname = 'user_id')]) $q$),
    '0');

SELECT test.assert_eq('schema', '9 foreign keys reference users(id)',
    test.scalar($q$ SELECT count(*) FROM pg_constraint
                    WHERE contype = 'f' AND confrelid = 'public.users'::regclass $q$),
    '9');

SELECT test.assert_eq('schema', 'all foreign keys to users are ON DELETE CASCADE',
    test.scalar($q$ SELECT count(*) FROM pg_constraint
                    WHERE contype = 'f' AND confrelid = 'public.users'::regclass AND confdeltype <> 'c' $q$),
    '0');

SELECT test.assert_eq('schema', 'no PostgreSQL ENUM types (JPA-friendly VARCHAR + CHECK instead)',
    test.scalar($q$ SELECT count(*) FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                    WHERE n.nspname = 'public' AND t.typtype = 'e' $q$),
    '0');

SELECT test.assert_eq('schema', 'all 19 expected indexes exist (none missing)',
    test.scalar($q$ SELECT string_agg(e, ',') FROM unnest(ARRAY[
        'users_email_key', 'refresh_sessions_token_hash_key', 'refresh_sessions_user_idx',
        'refresh_sessions_active_idx', 'refresh_sessions_expires_idx', 'consent_records_user_type_idx',
        'mood_check_ins_user_date_key', 'check_in_tags_pkey',
        'journal_entries_user_created_idx', 'journal_entries_user_date_idx', 'journal_entries_search_idx',
        'journal_entry_tags_tag_idx', 'habits_id_user_key', 'habits_user_name_uq',
        'habit_completions_habit_date_key', 'habit_completions_user_date_idx',
        'habit_pauses_habit_idx', 'habit_pauses_one_open_idx', 'coping_strategies_user_order_idx']) e
        WHERE NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = e) $q$),
    NULL);

SELECT test.assert_eq('schema', 'every table with updated_at has the set_updated_at trigger',
    test.scalar($q$ SELECT string_agg(c.table_name, ',') FROM information_schema.columns c
                    WHERE c.table_schema = 'public' AND c.column_name = 'updated_at'
                      AND NOT EXISTS (SELECT 1 FROM pg_trigger t
                                      WHERE t.tgrelid = format('public.%I', c.table_name)::regclass
                                        AND t.tgfoid = 'public.set_updated_at'::regproc
                                        AND NOT t.tgisinternal) $q$),
    NULL);

SELECT test.assert_eq('schema', 'habit pause/completion ownership FKs are composite (habit_id, user_id)',
    test.scalar($q$ SELECT string_agg(conname, ',' ORDER BY conname) FROM pg_constraint
                    WHERE contype = 'f' AND confrelid = 'public.habits'::regclass
                      AND array_length(conkey, 1) = 2 $q$),
    'habit_completions_habit_owner_fk,habit_pauses_habit_owner_fk');
