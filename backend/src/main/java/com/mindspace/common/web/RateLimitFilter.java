package com.mindspace.common.web;

import com.mindspace.common.ratelimit.RateLimiter;
import com.mindspace.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-IP limits on the endpoints worth brute-forcing or abusing:
 * <ul>
 *   <li>auth group: login, register, refresh</li>
 *   <li>sensitive group: password change, data export, account deletion</li>
 * </ul>
 * The client address is the TCP peer address. Behind a reverse proxy, configure Tomcat's RemoteIpValve
 * ({@code server.forward-headers-strategy=native}) with a trusted proxy list so this is the real client.
 * A per-e-mail login limit is applied separately in AuthService.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter authLimiter;
    private final RateLimiter sensitiveLimiter;

    public RateLimitFilter(AppProperties props) {
        AppProperties.RateLimit rl = props.rateLimit();
        this.authLimiter = new RateLimiter(rl.authIpMax(), rl.authIpWindow());
        this.sensitiveLimiter = new RateLimiter(rl.sensitiveIpMax(), rl.sensitiveIpWindow());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateLimiter limiter = limiterFor(request);
        if (limiter != null) {
            String key = request.getRemoteAddr();
            if (!limiter.tryAcquire(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(limiter.retryAfterSeconds(key)));
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"type\":\"urn:mindspace:problem:rate-limited\","
                        + "\"title\":\"Too many requests\",\"status\":429,"
                        + "\"detail\":\"Too many attempts. Please wait before trying again.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private RateLimiter limiterFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method) && !"GET".equals(method)) {
            return null;
        }
        if ("POST".equals(method) && (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh"))) {
            return authLimiter;
        }
        if ("PUT".equals(method) && path.equals("/api/v1/users/me/password")) {
            return sensitiveLimiter;
        }
        if ("GET".equals(method) && path.equals("/api/v1/privacy/export")) {
            return sensitiveLimiter;
        }
        if ("DELETE".equals(method) && path.equals("/api/v1/account")) {
            return sensitiveLimiter;
        }
        return null;
    }

    @Scheduled(fixedDelay = 600_000)
    void purge() {
        authLimiter.purgeIdle();
        sensitiveLimiter.purgeIdle();
    }
}
