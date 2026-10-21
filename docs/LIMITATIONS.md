# Limitations

**Build/verification**
- Build, unit/integration tests and three E2E smoke scenarios pass in GitHub Actions (see README status). Beyond those, most feature paths have no automated browser test yet.
- The Maven wrapper (`mvnw`) is not generated; use a local Maven (3.9+) or run `mvn -N wrapper:wrapper`.

**Product**
- Not a medical device and gives no clinical advice; scores are self-ratings. Crisis numbers cover India, US and UK/Ireland only and must be re-verified before each release (last checked 2026-10-20).
- No password reset, e-mail verification or MFA.
- Rate limiting is in-memory and per instance.
- Export is assembled in memory (fine for one person's data, not for huge accounts); deletion is immediate and irreversible.
- Single user per account, no sharing, no offline mode, English only, no push reminders.
- Analytics are descriptive; they require ≥ 5 observations before comparisons are shown and never imply causation.
- Journal search uses the `simple` Postgres text configuration (no stemming), chosen for language neutrality.
- No screenshots, because the UI has never been rendered.
