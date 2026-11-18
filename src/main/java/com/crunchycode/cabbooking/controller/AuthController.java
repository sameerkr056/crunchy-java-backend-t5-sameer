package com.crunchycode.cabbooking.controller;

import com.crunchycode.cabbooking.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.crunchycode.cabbooking.dto.request.LoginRequest;
import com.crunchycode.cabbooking.dto.request.RegisterRequest;
import com.crunchycode.cabbooking.dto.response.AuthResponse;
import com.crunchycode.cabbooking.dto.response.UserResponse;
import com.crunchycode.cabbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints (no token needed). */
@Tag(name = "Authentication", description = "Register and log in. These two endpoints are public (no token needed).")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user",
            description = "Creates a normal USER. The role cannot be chosen in the request. Password: 8 to 72 characters.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already used",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Log in",
            description = "Returns a JWT. Send it in the header `Authorization: Bearer <token>` (in Swagger: click Authorize).")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid input (validation error)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Wrong username or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
