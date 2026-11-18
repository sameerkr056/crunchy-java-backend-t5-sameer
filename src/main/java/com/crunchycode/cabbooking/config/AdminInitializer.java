package com.crunchycode.cabbooking.config;

import com.crunchycode.cabbooking.model.AppUser;
import com.crunchycode.cabbooking.model.Role;
import com.crunchycode.cabbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Creates the first ADMIN account at startup, because registration only creates normal USERs.
 * The username and password come from environment variables (ADMIN_USERNAME, ADMIN_PASSWORD).
 * If they are not set, nothing happens. If the admin already exists, nothing happens.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username:}") String adminUsername,
                            @Value("${app.admin.email:admin@example.com}") String adminEmail,
                            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase(adminUsername)) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
    }
}
