package com.contentaggregation.auth.controller;

import com.contentaggregation.auth.dto.request.LoginRequest;
import com.contentaggregation.auth.dto.request.RefreshTokenRequest;
import com.contentaggregation.auth.dto.request.RegisterRequest;
import com.contentaggregation.auth.dto.response.AuthResponse;
import com.contentaggregation.auth.dto.response.ErrorResponse;
import com.contentaggregation.auth.dto.response.MessageResponse;
import com.contentaggregation.auth.service.AuthenticationService;
import com.contentaggregation.auth.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

/**
 * REST controller for authentication operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final OAuth2Service oAuth2Service;

    @Value("${spring.security.oauth2.client.registration.vk.redirect-uri}")
    private String vkRedirectUri;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Creates a new user account with email and password"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or username already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request for email: {}", request.email());
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user with email and password, returns JWT tokens"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for email: {}", request.email());
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Uses refresh token to obtain a new access token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.debug("Token refresh request");
        AuthResponse response = authenticationService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Revokes the refresh token",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logged out successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.debug("Logout request");
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/oauth2/vk/authorize")
    @Operation(
            summary = "VK OAuth2 authorization",
            description = "Redirects to VK authorization page"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to VK")
    })
    public void authorizeVK(
            @Parameter(description = "State parameter for CSRF protection")
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        String authState = state != null ? state : UUID.randomUUID().toString();
        String authorizationUrl = oAuth2Service.getVKAuthorizationUrl(vkRedirectUri, authState);

        log.info("Redirecting to VK authorization");
        response.sendRedirect(authorizationUrl);
    }

    @GetMapping("/oauth2/vk/callback")
    @Operation(
            summary = "VK OAuth2 callback",
            description = "Handles VK OAuth2 callback with authorization code"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to frontend with tokens"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid authorization code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public void callbackVK(
            @Parameter(description = "Authorization code from VK")
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF validation")
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        log.info("VK OAuth2 callback received");

        AuthResponse authResponse = oAuth2Service.authenticateWithVK(code, vkRedirectUri);

        // Redirect to frontend with tokens
        String redirectUrl = String.format(
                "%s/auth/callback#access_token=%s&refresh_token=%s&token_type=Bearer",
                frontendUrl,
                authResponse.accessToken(),
                authResponse.refreshToken()
        );

        response.sendRedirect(redirectUrl);
    }
}
