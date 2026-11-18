package com.crunchycode.cabbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private SecretKey key;
    private String base64Secret;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        key = Jwts.SIG.HS256.key().build();
        base64Secret = Encoders.BASE64.encode(key.getEncoded());
        jwtService = new JwtService(base64Secret, 60);
    }

    @Test
    void generatedToken_canBeReadBack() {
        String token = jwtService.generateToken("sameer", "USER");

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("sameer");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void generatedToken_expiresAfterTheConfiguredTime() {
        Claims claims = jwtService.parseToken(jwtService.generateToken("sameer", "USER"));

        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(lifetimeMillis).isEqualTo(60 * 60_000L);
        assertThat(jwtService.getExpirationMinutes()).isEqualTo(60);
    }

    @Test
    void expiredToken_isRejected() {
        String expired = Jwts.builder().subject("sameer").claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key).compact();

        assertThatThrownBy(() -> jwtService.parseToken(expired)).isInstanceOf(ExpiredJwtException.class);
        assertThat(jwtService.isValid(expired)).isFalse();
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtService.generateToken("sameer", "USER");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtService.parseToken(tampered)).isInstanceOf(JwtException.class);
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String forged = Jwts.builder().subject("sameer").claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Jwts.SIG.HS256.key().build()).compact();

        assertThat(jwtService.isValid(forged)).isFalse();
    }

    @Test
    void garbageToken_isRejected() {
        assertThat(jwtService.isValid("not-a-token")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void expiryOfZeroOrLess_isNotAllowed() {
        assertThatThrownBy(() -> new JwtService(base64Secret, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(base64Secret, -5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tooShortSecret_isNotAllowed() {
        String shortSecret = Encoders.BASE64.encode("too-short".getBytes());

        assertThatThrownBy(() -> new JwtService(shortSecret, 60)).isInstanceOf(RuntimeException.class);
    }
}
