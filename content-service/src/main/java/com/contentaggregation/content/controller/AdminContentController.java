package com.contentaggregation.content.controller;

import com.contentaggregation.content.dto.request.CreateContentRequest;
import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.entity.ContentMetadata;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.Sentiment;
import com.contentaggregation.content.exception.ContentNotFoundException;
import com.contentaggregation.content.mapper.ContentMapper;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.service.content.ContentSeedingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for content management operations.
 *
 * <p>Provides endpoints for seeding test data, managing content, and administrative tasks.
 * These endpoints should be protected by admin role in production.
 */
@RestController
@RequestMapping("/api/v1/admin/content")
@Tag(name = "Admin Content Management", description = "Administrative operations for content")
public class AdminContentController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentController.class);

    private final ContentSeedingService contentSeedingService;
    private final ContentPostRepository contentPostRepository;
    private final ContentMapper contentMapper;
    private final ObjectMapper objectMapper;

    public AdminContentController(ContentSeedingService contentSeedingService,
                                   ContentPostRepository contentPostRepository,
                                   ContentMapper contentMapper,
                                   ObjectMapper objectMapper) {
        this.contentSeedingService = contentSeedingService;
        this.contentPostRepository = contentPostRepository;
        this.contentMapper = contentMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed database with test content",
            description = "Generates realistic test content for development and testing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content seeded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid count parameter")
    })
    @CacheEvict(value = {"feed", "coldStartFeed", "trending"}, allEntries = true)
    public ResponseEntity<Map<String, Object>> seedContent(
            @Parameter(description = "Number of content items to generate", example = "100")
            @RequestParam(defaultValue = "100") int count) {

        log.info("Admin request to seed {} content items", count);

        if (count <= 0 || count > 10000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Count must be between 1 and 10000"));
        }

        long startTime = System.currentTimeMillis();
        int created = contentSeedingService.seedContent(count);
        long duration = System.currentTimeMillis() - startTime;

        log.info("Seeded {} content items in {} ms", created, duration);

        return ResponseEntity.ok(Map.of(
                "message", "Seeded " + created + " content items",
                "count", created,
                "durationMs", duration
        ));
    }

    @GetMapping
    @Operation(summary = "List all content with pagination",
            description = "Returns paginated list of all content posts")
    public ResponseEntity<Page<ContentDto>> listContent(
            @PageableDefault(size = 20, sort = "publishedAt") Pageable pageable) {

        Page<ContentPost> posts = contentPostRepository.findAll(pageable);
        Page<ContentDto> dtos = posts.map(contentMapper::toDto);

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by ID",
            description = "Returns a specific content post by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content found"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<ContentDto> getContent(
            @Parameter(description = "Content ID")
            @PathVariable UUID id) {

        return contentPostRepository.findById(id)
                .map(contentMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete content by ID",
            description = "Permanently deletes a content post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Content deleted"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    @CacheEvict(value = {"feed", "coldStartFeed", "trending", "content"}, allEntries = true)
    public ResponseEntity<Void> deleteContent(
            @Parameter(description = "Content ID")
            @PathVariable UUID id) {

        if (!contentPostRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        contentPostRepository.deleteById(id);
        log.info("Deleted content: {}", id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all content",
            description = "Permanently deletes all content posts - use with caution!")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All content deleted")
    })
    @CacheEvict(value = {"feed", "coldStartFeed", "trending", "content", "similarContent"}, allEntries = true)
    public ResponseEntity<Map<String, Object>> deleteAllContent() {
        long count = contentPostRepository.count();
        contentPostRepository.deleteAll();

        log.warn("Deleted all content: {} items", count);

        return ResponseEntity.ok(Map.of(
                "message", "Deleted all content",
                "count", count
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get content statistics",
            description = "Returns statistics about content in the database")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalContent = contentPostRepository.count();

        Map<String, Object> stats = Map.of(
                "totalContent", totalContent,
                "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(stats);
    }

    @PostMapping
    @Operation(summary = "Create content manually",
            description = "Creates a new content post with metadata",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Content created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @Transactional
    @CacheEvict(value = {"feed", "coldStartFeed", "trending"}, allEntries = true)
    public ResponseEntity<ContentDto> createContent(
            @Valid @RequestBody CreateContentRequest request) {

        log.info("Admin creating content: {}", request.title());

        try {
            // Serialize mediaUrls list to JSON string
            String mediaUrlsJson = request.mediaUrls() != null
                    ? objectMapper.writeValueAsString(request.mediaUrls())
                    : "[]";

            // Serialize categories map to JSON string
            String categoriesJson = request.categories() != null
                    ? objectMapper.writeValueAsString(request.categories())
                    : "{}";

            // Serialize keywords list to JSON string
            String keywordsJson = request.keywords() != null
                    ? objectMapper.writeValueAsString(request.keywords())
                    : "[]";

            // Create content post
            ContentPost post = ContentPost.builder()
                    .title(request.title())
                    .content(request.content())
                    .contentType(request.contentType())
                    .source(ContentSource.MANUAL)
                    .externalId("admin-" + UUID.randomUUID())
                    .linkUrl(request.linkUrl())
                    .mediaUrls(mediaUrlsJson)
                    .hasMedia(request.mediaUrls() != null && !request.mediaUrls().isEmpty())
                    .mediaType(request.mediaType())
                    .contentLength(request.content() != null ? request.content().length() : 0)
                    .sourceChannelName(request.sourceChannelName())
                    .publishedAt(LocalDateTime.now())
                    .build();

            // Create metadata
            ContentMetadata metadata = ContentMetadata.builder()
                    .contentPost(post)
                    .categories(categoriesJson)
                    .keywords(keywordsJson)
                    .sentiment(Sentiment.NEUTRAL)
                    .readingTimeMinutes(calculateReadingTime(request.content()))
                    .language("en")
                    .build();

            post.setMetadata(metadata);

            ContentPost savedPost = contentPostRepository.save(post);
            log.info("Created content with ID: {}", savedPost.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(contentMapper.toDto(savedPost));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize content data", e);
            throw new IllegalArgumentException("Invalid content data format");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content",
            description = "Updates an existing content post",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content updated successfully"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    @Transactional
    @CacheEvict(value = {"feed", "coldStartFeed", "trending", "content", "similarContent"}, allEntries = true)
    public ResponseEntity<ContentDto> updateContent(
            @Parameter(description = "Content ID") @PathVariable UUID id,
            @Valid @RequestBody CreateContentRequest request) {

        log.info("Admin updating content: {}", id);

        ContentPost post = contentPostRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));

        try {
            // Update content post fields
            if (request.title() != null) {
                post.setTitle(request.title());
            }
            if (request.content() != null) {
                post.setContent(request.content());
                post.setContentLength(request.content().length());
            }
            if (request.contentType() != null) {
                post.setContentType(request.contentType());
            }
            if (request.linkUrl() != null) {
                post.setLinkUrl(request.linkUrl());
            }
            if (request.mediaUrls() != null) {
                post.setMediaUrls(objectMapper.writeValueAsString(request.mediaUrls()));
                post.setHasMedia(!request.mediaUrls().isEmpty());
            }
            if (request.mediaType() != null) {
                post.setMediaType(request.mediaType());
            }
            if (request.sourceChannelName() != null) {
                post.setSourceChannelName(request.sourceChannelName());
            }

            // Update metadata
            ContentMetadata metadata = post.getMetadata();
            if (metadata != null) {
                if (request.categories() != null) {
                    metadata.setCategories(objectMapper.writeValueAsString(request.categories()));
                }
                if (request.keywords() != null) {
                    metadata.setKeywords(objectMapper.writeValueAsString(request.keywords()));
                }
                if (request.content() != null) {
                    metadata.setReadingTimeMinutes(calculateReadingTime(request.content()));
                }
            }

            ContentPost savedPost = contentPostRepository.save(post);
            log.info("Updated content with ID: {}", savedPost.getId());

            return ResponseEntity.ok(contentMapper.toDto(savedPost));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize content data", e);
            throw new IllegalArgumentException("Invalid content data format");
        }
    }

    private int calculateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        int wordCount = content.split("\\s+").length;
        return Math.max(1, wordCount / 200); // ~200 words per minute
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear all caches",
            description = "Clears all Redis caches for content service")
    @CacheEvict(value = {"feed", "coldStartFeed", "trending", "content", "similarContent", "userPreferences"},
            allEntries = true)
    public ResponseEntity<Map<String, String>> clearCaches() {
        log.info("Admin cleared all caches");
        return ResponseEntity.ok(Map.of("message", "All caches cleared"));
    }
}
