package com.mindspace.auth;

import com.mindspace.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived access tokens (HS256). Claims: sub = user id, sid = refresh-token family id, iat, exp.
 * The token lives only in an HttpOnly cookie; it is never readable by JavaScript.
 */
@Service
public class JwtService {

    private static final String ISSUER = "mindspace";

    private final SecretKey key;
    private final AppProperties.Security config;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        this.config = props.security();
        this.key = Keys.hmacShaKeyFor(config.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issue(UUID userId, UUID familyId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim("sid", familyId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(config.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** Empty for any invalid, expired, tampered or malformed token. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthUser(UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get("sid", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
