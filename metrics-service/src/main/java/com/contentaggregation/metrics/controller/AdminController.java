package com.contentaggregation.metrics.controller;

import com.contentaggregation.metrics.dto.response.RecalculateResponse;
import com.contentaggregation.metrics.service.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/profiles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Profile Operations", description = "Administrative controls for user profiles")
public class AdminController {

    private final UserProfileService profileService;

    public AdminController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(
        summary = "Recalculate all profiles from stored events",
        description = "Deletes existing profiles and rebuilds them from historical events.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Recalculation complete",
                content = @Content(mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RecalculateResponse.class)))
        }
    )
    @PostMapping("/recalculate")
    public ResponseEntity<RecalculateResponse> recalculateAllProfiles() {
        int processed = profileService.recalculateAllProfiles();
        return ResponseEntity.ok(new RecalculateResponse(processed));
    }

    @Operation(
        summary = "Export user profiles",
        description = "Exports all profiles as CSV (default) or JSON for downstream ML pipelines.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Export generated",
                content = {
                    @Content(
                        mediaType = "text/csv",
                        examples = @ExampleObject(name = "csv", value = "userId,lastUpdated,totalViews,...")
                    ),
                    @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(name = "json", value = "[{ \"userId\": \"...\" }]")
                    )
                })
        }
    )
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProfiles(@RequestParam(required = false, defaultValue = "csv") String format) {
        String normalized = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        byte[] data = profileService.exportProfiles(normalized);
        MediaType contentType = "json".equals(normalized) ? MediaType.APPLICATION_JSON : MediaType.parseMediaType("text/csv");
        String filename = "profiles." + ("json".equals(normalized) ? "json" : "csv");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(contentType)
            .body(data);
    }

    @Operation(
        summary = "Replay events for a single user",
        description = "Deletes the user's profile and replays events from the specified timestamp (inclusive).",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "202", description = "Replay accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
        }
    )
    @PostMapping("/events/replay")
    public ResponseEntity<Void> replayEventsForUser(
        @RequestParam UUID userId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime fromDate
    ) {
        profileService.replayEvents(userId, fromDate);
        return ResponseEntity.accepted().build();
    }

}


