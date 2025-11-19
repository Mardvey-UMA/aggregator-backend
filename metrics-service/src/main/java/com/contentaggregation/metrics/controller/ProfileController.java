package com.contentaggregation.metrics.controller;

import com.contentaggregation.metrics.dto.request.UpdatePreferencesRequest;
import com.contentaggregation.metrics.dto.response.UserProfileDto;
import com.contentaggregation.metrics.service.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "User Profiles", description = "Profile retrieval and preference management APIs")
public class ProfileController {

    private final UserProfileService profileService;

    public ProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(
        summary = "Get aggregated user profile",
        description = "Returns the full preference profile used by the recommender service.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile found",
                content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found")
        }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID userId) {
        return profileService.getProfile(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get preference snapshots",
        description = "Returns category, entity, and content-type preferences. Creates a default profile on first access.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Preferences returned",
                content = @Content(examples = @ExampleObject(
                    name = "preferenceResponse",
                    value = """
                        {
                          "categories": { "technology": 0.84, "finance": 0.64 },
                          "contentTypes": { "long_form": 0.6, "short_form": 0.4 },
                          "entities": { "topics": { "ai": 12, "cloud": 5 } }
                        }
                        """
                ))
            )
        }
    )
    @GetMapping("/{userId}/preferences")
    public Map<String, Object> getPreferences(@PathVariable UUID userId) {
        UserProfileDto profile = profileService.getOrCreateProfile(userId);
        return Map.of(
            "categories", profile.categoryPreferences(),
            "contentTypes", profile.contentTypePreferences(),
            "entities", profile.entityPreferences()
        );
    }

    @Operation(
        summary = "Manually update category preferences",
        description = "Allows the authenticated user (or an admin) to override category weights.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Preferences updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PutMapping("/{userId}/preferences")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<Void> updatePreferences(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        profileService.updatePreferencesManually(userId, request.categories());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Delete user profile",
        description = "Admin or profile owner can delete the aggregated profile. It will be rebuilt from future events.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "204", description = "Profile deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @DeleteMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProfile(@Parameter(description = "User identifier") @PathVariable UUID userId) {
        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}


