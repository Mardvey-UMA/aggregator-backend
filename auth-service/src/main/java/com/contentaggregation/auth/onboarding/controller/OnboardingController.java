package com.contentaggregation.auth.onboarding.controller;

import com.contentaggregation.auth.onboarding.dto.request.CategorySelectionRequest;
import com.contentaggregation.auth.onboarding.dto.request.CompleteOnboardingRequest;
import com.contentaggregation.auth.onboarding.dto.request.ContentTypeSelectionRequest;
import com.contentaggregation.auth.onboarding.dto.response.CategoryOptionResponse;
import com.contentaggregation.auth.onboarding.dto.response.OnboardingStatusResponse;
import com.contentaggregation.auth.onboarding.service.OnboardingService;
import com.contentaggregation.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@Tag(name = "Onboarding", description = "User onboarding API")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    @Operation(
            summary = "Get onboarding status",
            description = "Returns the current onboarding progress for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OnboardingStatusResponse status = onboardingService.getOrCreateStatus(principal.getId());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Get available categories",
            description = "Lists all enabled onboarding categories for selection.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories retrieved",
                            content = @Content(schema = @Schema(implementation = CategoryOptionResponse.class)))
            }
    )
    public ResponseEntity<List<CategoryOptionResponse>> getCategories() {
        List<CategoryOptionResponse> categories = onboardingService.getAvailableCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/categories")
    @Operation(
            summary = "Save category selections",
            description = "Persists selected categories and advances onboarding to the next step.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories saved",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation failure"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> saveCategories(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategorySelectionRequest request
    ) {
        OnboardingStatusResponse status = onboardingService.saveCategories(principal.getId(), request);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/content-types")
    @Operation(
            summary = "Save content type selections",
            description = "Stores preferred content formats for personalization.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content types saved",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation failure"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Onboarding not found")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> saveContentTypes(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ContentTypeSelectionRequest request
    ) {
        OnboardingStatusResponse status = onboardingService.saveContentTypes(principal.getId(), request);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/complete")
    @Operation(
            summary = "Complete onboarding",
            description = "Finalizes onboarding selections and initializes the metrics profile.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Onboarding completed",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation failure"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        OnboardingStatusResponse status = onboardingService.completeOnboarding(principal.getId(), request);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/skip")
    @Operation(
            summary = "Skip onboarding",
            description = "Skips onboarding and applies smart defaults for the user.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Onboarding skipped",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> skip(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OnboardingStatusResponse status = onboardingService.skipOnboarding(principal.getId());
        return ResponseEntity.ok(status);
    }

    @PutMapping("/reset")
    @Operation(
            summary = "Reset onboarding",
            description = "Clears onboarding selections so the user can start over.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Onboarding reset",
                            content = @Content(schema = @Schema(implementation = OnboardingStatusResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Onboarding not found")
            }
    )
    public ResponseEntity<OnboardingStatusResponse> reset(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OnboardingStatusResponse status = onboardingService.resetOnboarding(principal.getId());
        return ResponseEntity.ok(status);
    }
}

