package com.mindspace.auth;

import com.mindspace.auth.AuthDtos.LoginRequest;
import com.mindspace.auth.AuthDtos.RegisterRequest;
import com.mindspace.auth.AuthDtos.SessionResponse;
import com.mindspace.user.UserDtos.UserResponse;
import com.mindspace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Cookie-based sessions. State-changing calls need the X-XSRF-TOKEN header (value of the XSRF-TOKEN cookie, issued by GET /auth/csrf).")
public class AuthController {

    private final AuthService auth;
    private final SessionService sessions;
    private final CookieFactory cookies;
    private final UserService users;

    public AuthController(AuthService auth, SessionService sessions, CookieFactory cookies, UserService users) {
        this.auth = auth;
        this.sessions = sessions;
        this.cookies = cookies;
        this.users = users;
    }

    @Operation(summary = "Issue a CSRF cookie", description = "Call once before the first state-changing request; the response sets the XSRF-TOKEN cookie.")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create an account and sign in", description = "409 if the e-mail is already registered. Sets HttpOnly ms_access and ms_refresh cookies.")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        AuthService.AuthResult result = auth.register(request, http.getHeader(HttpHeaders.USER_AGENT));
        return withCookies(HttpStatus.CREATED, result, users.profile(result.userId()));
    }

    @Operation(summary = "Sign in", description = "401 with a generic problem for any bad credentials; 429 after repeated failures for the same e-mail.")
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthService.AuthResult result = auth.login(request, http.getHeader(HttpHeaders.USER_AGENT));
        return withCookies(HttpStatus.OK, result, users.profile(result.userId()));
    }

    @Operation(summary = "Rotate the refresh token and issue a new access token", description = "Reads the ms_refresh cookie. A reused refresh token revokes the whole session family.")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest http) {
        AuthService.AuthResult result = auth.refresh(CookieFactory.read(http, CookieFactory.REFRESH_COOKIE),
                http.getHeader(HttpHeaders.USER_AGENT));
        HttpHeaders headers = new HttpHeaders();
        cookies.addSessionCookies(headers, result.accessToken(), result.refreshToken());
        return ResponseEntity.noContent().headers(headers).build();
    }

    @Operation(summary = "Sign out this session", description = "Revokes the session server-side and clears the cookies. Always 204.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        sessions.logoutByRefreshToken(CookieFactory.read(http, CookieFactory.REFRESH_COOKIE));
        HttpHeaders headers = new HttpHeaders();
        cookies.addClearingCookies(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @Operation(summary = "Sign out of every device")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthUser user) {
        sessions.logoutAll(user.userId());
        HttpHeaders headers = new HttpHeaders();
        cookies.addClearingCookies(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @Operation(summary = "List active sessions of the signed-in user")
    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal AuthUser user) {
        return sessions.activeSessions(user.userId()).stream()
                .map(s -> new SessionResponse(s.getId(), s.getIssuedAt(), s.getExpiresAt(), s.getUserAgent(),
                        s.getFamilyId().equals(user.sessionFamilyId())))
                .toList();
    }

    @Operation(summary = "Revoke one of your sessions", description = "404 for ids that are not yours.")
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        sessions.revokeOwned(user.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<UserResponse> withCookies(HttpStatus status, AuthService.AuthResult result, UserResponse body) {
        HttpHeaders headers = new HttpHeaders();
        cookies.addSessionCookies(headers, result.accessToken(), result.refreshToken());
        return ResponseEntity.status(status).headers(headers).body(body);
    }
}
