-- =============================================================================
-- MindSpace V2: daily mood check-ins and their tags
--
-- Policy: at most ONE check-in per user per LOCAL calendar day.
--   local_date is derived by the application from the user's timezone at the
--   time of the check-in; `timezone` records which zone was used so historical
--   rows stay interpretable if the user later changes their profile timezone.
--   The UNIQUE (user_id, local_date) constraint is the authoritative guard and
--   makes concurrent duplicate submissions safe: exactly one insert wins.
--
-- Score meanings (self-reported observations, NOT clinical measures):
--   mood_score   1 = very low ... 5 = very good
--   stress_score 1 = very calm ... 5 = very stressed
--   energy_score 1 = very low ... 5 = very high
--   sleep_quality (optional) 1 = very poor ... 5 = very good
-- =============================================================================

CREATE TABLE mood_check_ins (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    local_date    DATE        NOT NULL,
    timezone      VARCHAR(64) NOT NULL,
    mood_score    INTEGER     NOT NULL,
    stress_score  INTEGER     NOT NULL,
    energy_score  INTEGER     NOT NULL,
    sleep_minutes INTEGER     NOT NULL,
    sleep_quality INTEGER,
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT mood_check_ins_user_fk          FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Also the index that serves "this user's check-ins in a date range".
    CONSTRAINT mood_check_ins_user_date_key    UNIQUE (user_id, local_date),
    CONSTRAINT mood_check_ins_timezone_chk     CHECK (char_length(timezone) BETWEEN 1 AND 64),
    CONSTRAINT mood_check_ins_mood_chk         CHECK (mood_score BETWEEN 1 AND 5),
    CONSTRAINT mood_check_ins_stress_chk       CHECK (stress_score BETWEEN 1 AND 5),
    CONSTRAINT mood_check_ins_energy_chk       CHECK (energy_score BETWEEN 1 AND 5),
    CONSTRAINT mood_check_ins_sleep_minutes_chk CHECK (sleep_minutes BETWEEN 0 AND 1440),
    CONSTRAINT mood_check_ins_sleep_quality_chk CHECK (sleep_quality IS NULL OR sleep_quality BETWEEN 1 AND 5),
    CONSTRAINT mood_check_ins_notes_chk        CHECK (notes IS NULL OR char_length(notes) <= 2000)
);

CREATE TRIGGER mood_check_ins_set_updated_at
    BEFORE UPDATE ON mood_check_ins
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Emotion tags, activities and context tags share one table.
-- Ownership is inherited through check_in_id; this table has no user_id, so
-- every access path must join through mood_check_ins (which is user-scoped).
CREATE TABLE check_in_tags (
    check_in_id UUID        NOT NULL,
    tag_type    VARCHAR(10) NOT NULL,
    label       VARCHAR(50) NOT NULL,
    CONSTRAINT check_in_tags_pkey     PRIMARY KEY (check_in_id, tag_type, label),
    CONSTRAINT check_in_tags_check_in_fk FOREIGN KEY (check_in_id) REFERENCES mood_check_ins (id) ON DELETE CASCADE,
    CONSTRAINT check_in_tags_type_chk  CHECK (tag_type IN ('EMOTION', 'ACTIVITY', 'CONTEXT')),
    CONSTRAINT check_in_tags_label_chk CHECK (label = lower(btrim(label)) AND char_length(label) BETWEEN 1 AND 50)
);
