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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Anyone can register, but only as a normal USER.
     * The client can never choose the role (otherwise everybody could make themselves ADMIN).
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ResourceConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceConflictException("A user with email " + request.getEmail() + " already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt hash
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Spring Security checks the username and the BCrypt password.
     * A wrong username or password throws BadCredentialsException, which the GlobalExceptionHandler turns into 401.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER")
                .replace("ROLE_", "");

        String token = jwtService.generateToken(authentication.getName(), role);
        return new AuthResponse(token, jwtService.getExpirationMinutes(), authentication.getName(), role);
    }
}
