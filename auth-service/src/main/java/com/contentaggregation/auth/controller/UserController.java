package com.contentaggregation.auth.controller;

import com.contentaggregation.auth.dto.request.UpdateUserRequest;
import com.contentaggregation.auth.dto.response.ErrorResponse;
import com.contentaggregation.auth.dto.response.UserDto;
import com.contentaggregation.auth.security.UserPrincipal;
import com.contentaggregation.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user's profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Getting current user: {}", userPrincipal.getId());
        UserDto user = userService.getUserById(userPrincipal.getId());
        return ResponseEntity.ok(user);
    }

    @PutMapping
    @Operation(
            summary = "Update current user",
            description = "Updates the authenticated user's profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or username already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("Updating user: {}", userPrincipal.getId());
        UserDto user = userService.updateUser(userPrincipal.getId(), request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping
    @Operation(
            summary = "Delete current user",
            description = "Deletes the authenticated user's account"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Deleting user: {}", userPrincipal.getId());
        userService.deleteUser(userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
