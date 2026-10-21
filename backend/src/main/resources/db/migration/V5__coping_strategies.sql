-- =============================================================================
-- MindSpace V5: personal coping strategies
--
-- User-authored, user-ordered notes. sort_order is NOT unique so that reordering
-- does not need deferrable constraints; ties are broken by created_at.
-- (WellnessGoal is intentionally not part of this migration set yet.)
-- =============================================================================

CREATE TABLE coping_strategies (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    title      VARCHAR(120) NOT NULL,
    details    TEXT,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT coping_strategies_user_fk    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT coping_strategies_title_chk  CHECK (char_length(btrim(title, E' \t\r\n')) BETWEEN 1 AND 120),
    CONSTRAINT coping_strategies_details_chk CHECK (details IS NULL OR char_length(details) <= 2000),
    CONSTRAINT coping_strategies_sort_chk   CHECK (sort_order >= 0)
);

CREATE INDEX coping_strategies_user_order_idx ON coping_strategies (user_id, sort_order, created_at);

CREATE TRIGGER coping_strategies_set_updated_at
    BEFORE UPDATE ON coping_strategies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
