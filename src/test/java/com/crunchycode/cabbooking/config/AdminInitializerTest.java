package com.crunchycode.cabbooking.config;

import com.crunchycode.cabbooking.model.AppUser;
import com.crunchycode.cabbooking.model.Role;
import com.crunchycode.cabbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void noAdminConfigured_doesNothing() {
        AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder, "", "a@example.com", "");

        initializer.run();

        verifyNoInteractions(userRepository);
    }

    @Test
    void adminAlreadyExists_isNotCreatedAgain() {
        when(userRepository.existsByUsernameIgnoreCase("boss")).thenReturn(true);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "boss", "boss@example.com", "SomePass123");

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void adminConfigured_createsAnAdminWithAHashedPassword() {
        when(userRepository.existsByUsernameIgnoreCase("boss")).thenReturn(false);
        when(passwordEncoder.encode("SomePass123")).thenReturn("$2a$10$hash");
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "boss", "boss@example.com", "SomePass123");

        initializer.run();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("boss");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hash");
    }
}
