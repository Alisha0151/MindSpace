# Security

Status: **implemented and partly tested.** The `*IT` tests exercise cookie auth, CSRF, logout revocation, refresh rotation, forged tokens and cross-user isolation against a real server in CI. This is not a penetration test or an independent review; do that before any real use.

## Assets and threats
Most sensitive: journal text and check-in notes. Threats considered: credential stuffing/brute force, session theft, CSRF, XSS, IDOR (reading another user's rows), account enumeration, log leakage, over-large payloads, abusive export/delete.

## Controls
| Concern | Control |
|---|---|
| Password storage | BCrypt (cost 12 by default). Policy: ≥ 12 chars, ≤ 72 UTF-8 bytes (BCrypt truncation), small blocklist, must not contain the e-mail local part or display name. |
| Login timing / enumeration | A dummy BCrypt hash is checked for unknown e-mails; identical 401 for unknown user and wrong password. |
| Brute force | Per-IP sliding window on login/register/refresh; per-e-mail limit on failed logins. Password change, export and deletion have their own per-IP limit. |
| Sessions | Access JWT (HS256, 10 min) in an HttpOnly cookie; opaque 256-bit refresh token (14 days, `Path=/api/v1/auth`) whose **SHA-256 only** is stored. Rotation on each refresh; replay of an already-rotated token outside a 10 s grace window revokes the whole family. Every request checks the family is still active, so logout is immediate. |
| Cookies | HttpOnly, SameSite (configurable), Secure in production. No token in JS, URL or storage. |
| CSRF | Double-submit cookie (`XSRF-TOKEN` / `X-XSRF-TOKEN`) on all state-changing requests. |
| XSS | React escapes output; no `dangerouslySetInnerHTML`; API CSP `default-src 'self'`; nginx CSP for the SPA; no third-party scripts. |
| IDOR | Every repository finder takes the user id; cross-user ids give 404. Composite FKs in the database. |
| Input | Bean Validation + domain rules + DB CHECKs; 64 KiB body cap; page sizes capped; date ranges capped at 366 days. |
| Errors/logging | RFC 9457 responses without stack traces, SQL or submitted text. Request log has method, path, status, duration only: never query strings (search terms), bodies, cookies or e-mails. |
| Transport | HSTS, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`; TLS terminates in front of nginx. |
| Data control | Full JSON export; account deletion needs the password and cascades through every table. |
| Secrets | None committed. `.env.example` has placeholders; the app refuses to start without a JWT secret of ≥ 32 characters (compose requires it). |

## Known trade-offs
- Register returns 409 for an existing e-mail (usability over enumeration resistance).
- Per-e-mail failed-login limiting lets an attacker lock a victim's login for the window.
- Rate limiters are in memory and per instance; behind a proxy, configure trusted forwarded headers so the client IP is real.
- No MFA, e-mail verification or password reset yet.
- Data is not encrypted at the application level; rely on disk/database encryption and backups policy.
