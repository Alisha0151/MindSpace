package com.mindspace.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates a request from the HttpOnly access-token cookie. The token is valid only while its refresh-token
 * family is still active, so logout and "log out everywhere" take effect immediately.
 * Not a Spring bean on purpose: it is added to the security filter chain explicitly (a bean would also be
 * registered as a global servlet filter and run twice).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final SessionService sessions;

    public JwtAuthenticationFilter(JwtService jwt, SessionService sessions) {
        this.jwt = jwt;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = CookieFactory.read(request, CookieFactory.ACCESS_COOKIE);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwt.parse(token)
                    .filter(user -> sessions.isFamilyActive(user.sessionFamilyId()))
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
        }
        chain.doFilter(request, response);
    }
}
