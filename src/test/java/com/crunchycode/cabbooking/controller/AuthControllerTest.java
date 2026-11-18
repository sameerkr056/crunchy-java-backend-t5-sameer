package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.request.LoginRequest;
import com.crunchycode.cabbooking.dto.request.RegisterRequest;
import com.crunchycode.cabbooking.dto.response.AuthResponse;
import com.crunchycode.cabbooking.dto.response.UserResponse;
import com.crunchycode.cabbooking.exception.ResourceConflictException;
import com.crunchycode.cabbooking.model.Role;
import com.crunchycode.cabbooking.service.AuthService;
import com.crunchycode.cabbooking.support.SecurityTestSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.crunchycode.cabbooking.support.RequestHelper.build;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@SecurityTestSetup
class AuthControllerTest {

    private static final String REGISTER_JSON =
            "{\"username\":\"student1\",\"email\":\"student1@example.com\",\"password\":\"StrongPass1\"}";
    private static final String LOGIN_JSON = "{\"username\":\"student1\",\"password\":\"StrongPass1\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_isPublic_returns201_andNeverReturnsThePassword() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(1L, "student1", "student1@example.com", Role.USER));

        mockMvc.perform(build("POST", "/api/v1/auth/register", REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("student1"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("StrongPass1"))));
    }

    @Test
    void register_cannotAskForAdminRole() throws Exception {
        // a "role" field in the JSON is simply ignored: the request class has no such field
        String sneaky = "{\"username\":\"hacker\",\"email\":\"h@example.com\",\"password\":\"StrongPass1\",\"role\":\"ADMIN\"}";
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(2L, "hacker", "h@example.com", Role.USER));

        mockMvc.perform(build("POST", "/api/v1/auth/register", sneaky))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        String bad = "{\"username\":\"ab\",\"email\":\"nope\",\"password\":\"short\"}";

        mockMvc.perform(build("POST", "/api/v1/auth/register", bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("username")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")));
        verify(authService, never()).register(any());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ResourceConflictException("Username 'student1' is already taken"));

        mockMvc.perform(build("POST", "/api/v1/auth/register", REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'student1' is already taken"));
    }

    @Test
    void login_isPublic_returnsToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("the.jwt.token", 60, "student1", "USER"));

        mockMvc.perform(build("POST", "/api/v1/auth/login", LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("the.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMinutes").value(60))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_wrongPassword_returns401InOurErrorShape() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(build("POST", "/api/v1/auth/login", LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(build("POST", "/api/v1/auth/login", "{\"username\":\"student1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("password")));
    }
}
