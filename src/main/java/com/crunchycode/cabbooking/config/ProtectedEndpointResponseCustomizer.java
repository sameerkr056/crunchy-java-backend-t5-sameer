package com.crunchycode.cabbooking.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

/**
 * Every endpoint that has @PreAuthorize can answer 401 (no or bad token) and 403 (wrong role).
 * Instead of repeating these two answers on every method, they are added to the documentation here.
 */
public class ProtectedEndpointResponseCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!handlerMethod.hasMethodAnnotation(PreAuthorize.class)) {
            return operation;
        }
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        addIfMissing(operation, "401", "Missing, invalid or expired token");
        addIfMissing(operation, "403", "Your role is not allowed to call this endpoint");
        return operation;
    }

    private void addIfMissing(Operation operation, String code, String description) {
        if (operation.getResponses().containsKey(code)) {
            return;
        }
        Schema<Object> errorSchema = new Schema<>();
        errorSchema.set$ref("#/components/schemas/ErrorResponse");

        Content content = new Content().addMediaType("application/json", new MediaType().schema(errorSchema));
        operation.getResponses().addApiResponse(code, new ApiResponse().description(description).content(content));
    }
}
