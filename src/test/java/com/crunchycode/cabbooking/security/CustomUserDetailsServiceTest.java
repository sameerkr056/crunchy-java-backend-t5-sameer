package com.crunchycode.cabbooking.security;

import com.crunchycode.cabbooking.model.AppUser;
import com.crunchycode.cabbooking.model.Role;
import com.crunchycode.cabbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUser_returnsUsernameHashAndRoleAuthority() {
        AppUser user = new AppUser();
        user.setUsername("admin1");
        user.setPassword("$2a$10$hash");
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin1");

        assertThat(details.getUsername()).isEqualTo("admin1");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUser_unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
