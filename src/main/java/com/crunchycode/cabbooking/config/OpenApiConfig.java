package com.crunchycode.cabbooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI settings.
 * - Describes the API.
 * - Adds the "Authorize" button for the JWT (Bearer) token.
 * - Adds the 401 / 403 answers to every protected endpoint.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cabBookingOpenApi() {
        SecurityScheme bearer = new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Log in with POST /api/v1/auth/login, copy the \"token\" value and paste it here. "
                        + "Swagger adds the \"Bearer \" prefix for you.");

        return new OpenAPI()
                .info(new Info()
                        .title("Cab Booking Management API")
                        .version("1.0.0")
                        .description("""
                                REST API for drivers, rides and vehicle types (CrunchyCode internship, Task 5).

                                **How to try it**
                                1. Open `POST /api/v1/auth/register` and create a user (you become a USER).
                                2. Open `POST /api/v1/auth/login` and copy the `token` from the response.
                                3. Click **Authorize** (top right), paste the token and confirm.
                                4. Call any endpoint. A USER can read data and create/edit rides. An ADMIN can do everything.

                                **Pagination:** every list endpoint accepts `page`, `size`, `sortBy` and `direction`.

                                **Errors** always look the same: `timestamp`, `status`, `message`, `path`.""")
                        .contact(new Contact().name("Sameer Kumar").url("https://github.com/sameerkr056")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                // every endpoint needs the token unless it says otherwise (the auth endpoints override this)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public OperationCustomizer protectedEndpointResponses() {
        return new ProtectedEndpointResponseCustomizer();
    }
}
