# Demo script (about 5 minutes)
Use synthetic data only. Prerequisite: the stack is running (`docker compose up --build`, http://localhost:8081). *This flow has not been run yet; adjust to what you see.*

1. **Landing → Register** with a fake e-mail, a 12+ character passphrase, your time zone. Mention: password policy, cookies are HttpOnly (show DevTools → Application: no tokens in storage).
2. **Check-in**: set mood/stress/energy, add `calm, grateful`, save. Show the 30-day table. Try to check in again: it edits today's entry (one per local day).
3. **Habits**: add "Stretch" (daily) and "Walk" (3×/week). Mark done, then undo. Pause and resume. Open Progress.
4. **Journal**: write two entries with tags; search a word; filter by tag; edit; delete (confirm dialog).
5. **Calendar** and **Insights**: show shading with printed numbers, the "not enough data" message under 5 check-ins, and a chart's "View as a table".
6. **Toolkit**: run a short breathing session; use a reflection prompt → opens the journal editor with the prompt.
7. **Security story**: Settings → devices list; sign out everywhere; show 401 on a replayed request in DevTools.
8. **Privacy**: download the JSON export; delete the account (needs the password); show the login now fails.
9. **Engineering**: open `/swagger-ui/index.html`; run `bash db/scripts/run-db-tests.sh` to show the 173 SQL assertions.
