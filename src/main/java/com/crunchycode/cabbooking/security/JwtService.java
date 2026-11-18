package com.crunchycode.cabbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Creates and reads JWT tokens.
 * The secret and the lifetime come from configuration (environment variables), never from the source code.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("app.jwt.expiration-minutes must be greater than 0 (tokens must expire)");
        }
        // Throws an exception if the secret is shorter than 256 bits (32 bytes)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMinutes * 60_000L))
                .signWith(key)
                .compact();
    }

    /**
     * Checks the signature and the expiry date and returns the token content.
     * Throws a JwtException (or IllegalArgumentException) if the token is invalid, tampered or expired.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
