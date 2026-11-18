package com.crunchycode.cabbooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
 * Runs on every request. If the request has "Authorization: Bearer <token>" and the token is valid,
 * the user (name + role) is put into the SecurityContext. If not, the request simply stays anonymous and
 * Spring Security answers 401 for protected endpoints.
 *
 * It is created as a @Bean in SecurityConfig (not with @Component).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parseToken(token);
                String role = claims.get("role", String.class);
                if (claims.getSubject() != null && role != null) {
                    // Spring Security expects role authorities to start with ROLE_
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Bad, tampered or expired token: stay anonymous (the caller gets 401)
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
