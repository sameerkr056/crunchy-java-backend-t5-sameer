package com.crunchycode.cabbooking.service;

import com.crunchycode.cabbooking.dto.request.LoginRequest;
import com.crunchycode.cabbooking.dto.request.RegisterRequest;
import com.crunchycode.cabbooking.dto.response.AuthResponse;
import com.crunchycode.cabbooking.dto.response.UserResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.model.AppUser;
import com.crunchycode.cabbooking.model.Role;
import com.crunchycode.cabbooking.repository.UserRepository;
import com.crunchycode.cabbooking.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // a real JwtService with a random secret generated for this test
        String secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        jwtService = new JwtService(secret, 15);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    private static RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student1");
        request.setEmail("student1@example.com");
        request.setPassword("PlainPass123");
        return request;
    }

    @Test
    void register_hashesThePasswordAndAlwaysCreatesAUser() {
        when(userRepository.existsByUsernameIgnoreCase("student1")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("student1@example.com")).thenReturn(false);
        when(passwordEncoder.encode("PlainPass123")).thenReturn("$2a$10$hashedvalue");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        UserResponse response = authService.register(registerRequest());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashedvalue").isNotEqualTo("PlainPass123");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_usernameTaken_throwsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("student1")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("student1");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailTaken_throwsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("student1")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("student1@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("student1@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsATokenThatContainsTheUsernameAndRole() {
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(
                "admin1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin1");
        request.setPassword("whatever");
        AuthResponse response = authService.login(request);

        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInMinutes()).isEqualTo(15);
        Claims claims = jwtService.parseToken(response.getToken());
        assertThat(claims.getSubject()).isEqualTo("admin1");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt()); // the token expires
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin1");
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }
}
