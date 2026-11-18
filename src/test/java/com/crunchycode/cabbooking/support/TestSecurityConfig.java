package com.crunchycode.cabbooking.support;

import com.crunchycode.cabbooking.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Beans that the controller tests need in place of the real database-backed ones.
 * The JWT secret is generated randomly for every test run, so no secret exists anywhere in the source code.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtService jwtService() {
        String randomSecret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        return new JwtService(randomSecret, 30);
    }

    // Not used in these tests (login is mocked), but Spring Security wants one UserDetailsService bean
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
