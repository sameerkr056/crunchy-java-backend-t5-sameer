package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.service.AuthService;
import com.crunchycode.cabbooking.support.SecurityTestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger and the health check must be reachable without a token (otherwise nobody could read the docs
 * or let Railway check the app), while the API itself must stay protected.
 * In this small test no Swagger controller exists, so a public path answers 404 - the important part is that
 * it is NOT 401 or 403.
 */
@WebMvcTest(AuthController.class)
@SecurityTestSetup
class PublicEndpointsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui/index.html",
            "/swagger-ui.html", "/actuator/health"})
    void documentationAndHealthCheck_doNotNeedAToken(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/drivers", "/api/v1/rides", "/api/v1/vehicle-types", "/api/v1/drivers/with-rides",
            "/actuator/env", "/actuator/beans"})
    void everythingElse_stillNeedsAToken(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unauthorizedAnswer_doesNotLeakInternalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/drivers")).andExpect(result ->
                assertThat(result.getResponse().getContentAsString()).doesNotContain("Exception", "at com."));
    }
}
