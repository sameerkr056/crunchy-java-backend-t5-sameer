package com.crunchycode.cabbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests for the Swagger configuration (no Spring context, no database). */
class OpenApiConfigTest {

    /** A tiny fake controller: one protected method, one public method. */
    static class FakeController {
        @PreAuthorize("hasRole('ADMIN')")
        public void protectedMethod() {
        }

        public void publicMethod() {
        }
    }

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void openApi_describesTheApi() {
        OpenAPI openAPI = config.cabBookingOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Cab Booking Management API");
        assertThat(openAPI.getInfo().getDescription()).contains("Authorize");
    }

    @Test
    void openApi_hasJwtBearerSecurityScheme() {
        OpenAPI openAPI = config.cabBookingOpenApi();

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_SCHEME);
        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void openApi_requiresTheBearerTokenByDefault() {
        OpenAPI openAPI = config.cabBookingOpenApi();

        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().get(0)).containsKey(OpenApiConfig.BEARER_SCHEME);
    }

    private Operation customize(String methodName) throws NoSuchMethodException {
        Method method = FakeController.class.getMethod(methodName);
        HandlerMethod handlerMethod = new HandlerMethod(new FakeController(), method);
        return config.protectedEndpointResponses().customize(new Operation(), handlerMethod);
    }

    @Test
    void protectedEndpoint_gets401And403Documented() throws Exception {
        Operation operation = customize("protectedMethod");

        assertThat(operation.getResponses()).containsKeys("401", "403");
        assertThat(operation.getResponses().get("401").getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/ErrorResponse");
    }

    @Test
    void publicEndpoint_isLeftUntouched() throws Exception {
        Operation operation = customize("publicMethod");

        assertThat(operation.getResponses()).isNull();
    }

    @Test
    void existingResponsesAreNotOverwritten() throws Exception {
        Method method = FakeController.class.getMethod("protectedMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new FakeController(), method);
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("403", new io.swagger.v3.oas.models.responses.ApiResponse().description("custom text"));
        operation.setResponses(responses);

        config.protectedEndpointResponses().customize(operation, handlerMethod);

        assertThat(operation.getResponses().get("403").getDescription()).isEqualTo("custom text");
        assertThat(operation.getResponses()).containsKey("401");
    }
}
