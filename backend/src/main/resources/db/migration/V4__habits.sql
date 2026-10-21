-- =============================================================================
-- MindSpace V4: habits, pauses, completions
--
-- Definitions vs history are separate tables so history stays meaningful:
--   habits            what the habit is (cadence, target, start/end date)
--   habit_pauses      when it was paused/resumed (half-open: [paused_on, resumed_on))
--   habit_completions one row per habit per local date completed
--
-- Cadence rules:
--   DAILY  -> target_count = 1   (once per day)
--   WEEKLY -> target_count 1..7  (times per ISO week, Monday-Sunday)
--
-- Ownership: child tables carry user_id AND a composite FK (habit_id, user_id)
-- -> habits (id, user_id), so a pause or completion can never point at a habit
-- owned by a different user, even if application code has a bug.
--
-- Rules enforced by the application (cannot be expressed as a single-table
-- CHECK): completed_on >= habit.start_date, completed_on <= habit.end_date,
-- completed_on not in a pause window, completed_on not in the future.
-- =============================================================================

CREATE TABLE habits (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    cadence      VARCHAR(10)  NOT NULL,
    target_count INTEGER      NOT NULL DEFAULT 1,
    start_date   DATE         NOT NULL,
    end_date     DATE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT habits_user_fk           FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Target of the composite ownership FKs below.
    CONSTRAINT habits_id_user_key       UNIQUE (id, user_id),
    CONSTRAINT habits_name_chk          CHECK (char_length(btrim(name, E' \t\r\n')) BETWEEN 1 AND 100),
    CONSTRAINT habits_description_chk   CHECK (description IS NULL OR char_length(description) <= 500),
    CONSTRAINT habits_cadence_chk       CHECK (cadence IN ('DAILY', 'WEEKLY')),
    CONSTRAINT habits_cadence_target_chk CHECK (
        (cadence = 'DAILY'  AND target_count = 1) OR
        (cadence = 'WEEKLY' AND target_count BETWEEN 1 AND 7)),
    CONSTRAINT habits_dates_chk         CHECK (end_date IS NULL OR end_date >= start_date)
);

-- No two habits with the same name (case-insensitive) per user. Also serves
-- "list this user's habits" through its user_id prefix.
CREATE UNIQUE INDEX habits_user_name_uq ON habits (user_id, lower(btrim(name)));

CREATE TRIGGER habits_set_updated_at
    BEFORE UPDATE ON habits
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE habit_pauses (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id   UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    paused_on  DATE        NOT NULL,
    resumed_on DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT habit_pauses_user_fk        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT habit_pauses_habit_owner_fk FOREIGN KEY (habit_id, user_id) REFERENCES habits (id, user_id) ON DELETE CASCADE,
    CONSTRAINT habit_pauses_range_chk      CHECK (resumed_on IS NULL OR resumed_on > paused_on)
);

CREATE INDEX habit_pauses_habit_idx ON habit_pauses (habit_id);
-- At most one currently-open pause per habit.
CREATE UNIQUE INDEX habit_pauses_one_open_idx ON habit_pauses (habit_id) WHERE resumed_on IS NULL;

CREATE TABLE habit_completions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id     UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    completed_on DATE        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT habit_completions_user_fk        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT habit_completions_habit_owner_fk FOREIGN KEY (habit_id, user_id) REFERENCES habits (id, user_id) ON DELETE CASCADE,
    -- Idempotent completion: repeated or concurrent requests cannot double-count.
    CONSTRAINT habit_completions_habit_date_key UNIQUE (habit_id, completed_on)
);

-- Cross-habit, per-user date queries (analytics: habit completion vs mood).
CREATE INDEX habit_completions_user_date_idx ON habit_completions (user_id, completed_on);
