-- =============================================================================
-- MindSpace V3: private journal
--
-- Journal text is sensitive. It is stored as PLAIN TEXT only; the frontend must
-- render it as text, never as HTML. entry_date is the author's local calendar
-- date at creation (derived from users.timezone by the application) and is what
-- date-range filters use.
-- =============================================================================

CREATE TABLE journal_entries (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    entry_date  DATE         NOT NULL,
    title       VARCHAR(200),
    body        TEXT         NOT NULL,
    -- Optional reflection prompt the entry responded to.
    prompt_text VARCHAR(300),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    -- Generated and maintained by PostgreSQL; do NOT map it as a writable JPA
    -- field (use a native/JPQL-function query for search, or map read-only).
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || body)) STORED,
    CONSTRAINT journal_entries_user_fk      FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT journal_entries_title_chk    CHECK (title IS NULL OR char_length(title) BETWEEN 1 AND 200),
    CONSTRAINT journal_entries_body_chk     CHECK (char_length(btrim(body, E' \t\r\n')) >= 1 AND char_length(body) <= 10000),
    CONSTRAINT journal_entries_prompt_chk   CHECK (prompt_text IS NULL OR char_length(prompt_text) BETWEEN 1 AND 300)
);

-- Paginated history, newest first, keyset-friendly: WHERE user_id = ? ORDER BY created_at DESC, id DESC.
CREATE INDEX journal_entries_user_created_idx ON journal_entries (user_id, created_at DESC, id DESC);
-- Date-range filtering.
CREATE INDEX journal_entries_user_date_idx    ON journal_entries (user_id, entry_date);
-- Full-text search. Queries MUST also filter on user_id.
CREATE INDEX journal_entries_search_idx       ON journal_entries USING GIN (search_vector);

CREATE TRIGGER journal_entries_set_updated_at
    BEFORE UPDATE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Ownership is inherited through entry_id (no user_id column here).
CREATE TABLE journal_entry_tags (
    entry_id UUID        NOT NULL,
    tag      VARCHAR(50) NOT NULL,
    CONSTRAINT journal_entry_tags_pkey     PRIMARY KEY (entry_id, tag),
    CONSTRAINT journal_entry_tags_entry_fk FOREIGN KEY (entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT journal_entry_tags_tag_chk  CHECK (tag = lower(btrim(tag)) AND char_length(tag) BETWEEN 1 AND 50)
);

CREATE INDEX journal_entry_tags_tag_idx ON journal_entry_tags (tag, entry_id);
