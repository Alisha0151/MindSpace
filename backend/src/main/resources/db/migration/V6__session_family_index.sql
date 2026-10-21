-- =============================================================================
-- MindSpace V6: index for per-request session validation
--
-- Every authenticated request checks that the refresh-token family named in the
-- access token (claim "sid") still has a non-revoked row, so logout takes effect
-- immediately instead of when the short-lived access token expires:
--   SELECT 1 FROM refresh_sessions WHERE family_id = ? AND revoked_at IS NULL
-- =============================================================================

CREATE INDEX refresh_sessions_family_active_idx
    ON refresh_sessions (family_id)
    WHERE revoked_at IS NULL;
