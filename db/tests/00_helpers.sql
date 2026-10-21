-- Test harness helpers. Loaded into the TEMPORARY test database only (schema "test"),
-- never part of the Flyway migrations.

CREATE SCHEMA test;

CREATE TABLE test.results (
    id          SERIAL      PRIMARY KEY,
    suite       TEXT        NOT NULL,
    label       TEXT        NOT NULL,
    status      TEXT        NOT NULL CHECK (status IN ('PASS', 'FAIL')),
    detail      TEXT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE FUNCTION test.record(p_suite TEXT, p_label TEXT, p_ok BOOLEAN, p_detail TEXT DEFAULT NULL)
    RETURNS VOID LANGUAGE plpgsql AS
$$
BEGIN
    INSERT INTO test.results (suite, label, status, detail)
    VALUES (p_suite, p_label, CASE WHEN p_ok THEN 'PASS' ELSE 'FAIL' END, p_detail);
END;
$$;

-- Runs p_sql and expects it to fail with SQLSTATE p_sqlstate (and, if given, the
-- named constraint). If the statement unexpectedly succeeds its effects are rolled
-- back and the test is recorded as FAIL.
CREATE FUNCTION test.expect_error(p_suite TEXT, p_label TEXT, p_sql TEXT,
                                  p_sqlstate TEXT, p_constraint TEXT DEFAULT NULL)
    RETURNS VOID LANGUAGE plpgsql AS
$$
DECLARE
    v_state      TEXT;
    v_constraint TEXT;
    v_msg        TEXT;
BEGIN
    BEGIN
        EXECUTE p_sql;
        RAISE EXCEPTION 'statement succeeded' USING ERRCODE = 'XX999';
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS v_state = RETURNED_SQLSTATE,
                                v_constraint = CONSTRAINT_NAME,
                                v_msg = MESSAGE_TEXT;
    END;

    IF v_state = 'XX999' THEN
        PERFORM test.record(p_suite, p_label, FALSE,
            'expected SQLSTATE ' || p_sqlstate || ' but the statement succeeded (rolled back)');
    ELSIF v_state <> p_sqlstate THEN
        PERFORM test.record(p_suite, p_label, FALSE,
            'expected SQLSTATE ' || p_sqlstate || ', got ' || v_state || ': ' || v_msg);
    ELSIF p_constraint IS NOT NULL AND v_constraint IS DISTINCT FROM p_constraint THEN
        PERFORM test.record(p_suite, p_label, FALSE,
            'expected constraint ' || p_constraint || ', got ' || coalesce(v_constraint, '<none>'));
    ELSE
        PERFORM test.record(p_suite, p_label, TRUE,
            'rejected with SQLSTATE ' || v_state || coalesce(' / ' || v_constraint, ''));
    END IF;
END;
$$;

-- Runs p_sql and expects it to succeed (effects are kept).
CREATE FUNCTION test.expect_ok(p_suite TEXT, p_label TEXT, p_sql TEXT)
    RETURNS VOID LANGUAGE plpgsql AS
$$
DECLARE
    v_state TEXT;
    v_msg   TEXT;
BEGIN
    BEGIN
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS v_state = RETURNED_SQLSTATE, v_msg = MESSAGE_TEXT;
        PERFORM test.record(p_suite, p_label, FALSE, 'unexpected error ' || v_state || ': ' || v_msg);
        RETURN;
    END;
    PERFORM test.record(p_suite, p_label, TRUE, 'accepted');
END;
$$;

-- First column of the first row, as text (NULL stays NULL).
CREATE FUNCTION test.scalar(p_sql TEXT) RETURNS TEXT LANGUAGE plpgsql AS
$$
DECLARE
    v TEXT;
BEGIN
    EXECUTE p_sql INTO v;
    RETURN v;
END;
$$;

-- All rows rendered as "(a,b) | (c,d)" in query order, for exact comparisons.
CREATE FUNCTION test.rows(p_sql TEXT) RETURNS TEXT LANGUAGE plpgsql AS
$$
DECLARE
    v TEXT;
BEGIN
    EXECUTE 'SELECT string_agg(t.r, '' | '' ORDER BY t.n) FROM ('
         || 'SELECT x::text AS r, row_number() OVER () AS n FROM (' || p_sql || ') x) t'
    INTO v;
    RETURN coalesce(v, '<no rows>');
END;
$$;

CREATE FUNCTION test.assert_eq(p_suite TEXT, p_label TEXT, p_actual TEXT, p_expected TEXT)
    RETURNS VOID LANGUAGE plpgsql AS
$$
BEGIN
    PERFORM test.record(p_suite, p_label, p_actual IS NOT DISTINCT FROM p_expected,
        'expected=' || coalesce(p_expected, 'NULL') || ' actual=' || coalesce(p_actual, 'NULL'));
END;
$$;

-- Passes when the EXPLAIN output of p_sql mentions p_index_name.
CREATE FUNCTION test.assert_plan_uses(p_suite TEXT, p_label TEXT, p_sql TEXT, p_index_name TEXT)
    RETURNS VOID LANGUAGE plpgsql AS
$$
DECLARE
    r      RECORD;
    v_plan TEXT := '';
BEGIN
    FOR r IN EXECUTE 'EXPLAIN ' || p_sql LOOP
        v_plan := v_plan || r."QUERY PLAN" || E'\n';
    END LOOP;
    PERFORM test.record(p_suite, p_label, position(p_index_name IN v_plan) > 0,
        CASE WHEN position(p_index_name IN v_plan) > 0
             THEN 'plan uses ' || p_index_name
             ELSE 'index ' || p_index_name || ' NOT in plan: ' || replace(v_plan, E'\n', ' // ') END);
END;
$$;
