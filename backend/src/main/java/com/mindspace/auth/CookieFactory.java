package com.mindspace.auth;

import com.mindspace.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the two authentication cookies. Both are HttpOnly. The refresh cookie is scoped to the auth
 * endpoints so it is not sent with ordinary API calls.
 */
@Component
public class CookieFactory {

    public static final String ACCESS_COOKIE = "ms_access";
    public static final String REFRESH_COOKIE = "ms_refresh";
    private static final String REFRESH_PATH = "/api/v1/auth";

    private final AppProperties.Security config;

    public CookieFactory(AppProperties props) {
        this.config = props.security();
    }

    public String access(String token) {
        return build(ACCESS_COOKIE, token, "/", config.accessTokenTtl()).toString();
    }

    public String refresh(String token) {
        return build(REFRESH_COOKIE, token, REFRESH_PATH, config.refreshTokenTtl()).toString();
    }

    public void addSessionCookies(org.springframework.http.HttpHeaders headers, String accessToken, String refreshToken) {
        headers.add(HttpHeaders.SET_COOKIE, access(accessToken));
        headers.add(HttpHeaders.SET_COOKIE, refresh(refreshToken));
    }

    public void addClearingCookies(org.springframework.http.HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, build(ACCESS_COOKIE, "", "/", Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE, build(REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO).toString());
    }

    public static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(config.cookieSecure())
                .sameSite(config.cookieSameSite())
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
