package com.contentaggregation.content.controller;

import com.contentaggregation.content.dto.UserPreferences;
import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.dto.response.FeedResponse;
import com.contentaggregation.content.entity.ContentBookmark;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.entity.ContentView;
import com.contentaggregation.content.exception.ContentNotFoundException;
import com.contentaggregation.content.mapper.ContentMapper;
import com.contentaggregation.content.repository.ContentBookmarkRepository;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.repository.ContentViewRepository;
import com.contentaggregation.content.security.UserPrincipal;
import com.contentaggregation.content.service.recommendation.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public controller for content retrieval and recommendation.
 *
 * <p>Provides endpoints for personalized feeds, trending content, bookmarks, and content discovery.
 */
@RestController
@RequestMapping("/api/v1/content")
@Tag(name = "Content", description = "Content retrieval and recommendation")
public class ContentController {

    private static final Logger log = LoggerFactory.getLogger(ContentController.class);

    private final RecommendationService recommendationService;
    private final ContentPostRepository contentPostRepository;
    private final ContentBookmarkRepository bookmarkRepository;
    private final ContentViewRepository viewRepository;
    private final ContentMapper contentMapper;

    public ContentController(RecommendationService recommendationService,
                              ContentPostRepository contentPostRepository,
                              ContentBookmarkRepository bookmarkRepository,
                              ContentViewRepository viewRepository,
                              ContentMapper contentMapper) {
        this.recommendationService = recommendationService;
        this.contentPostRepository = contentPostRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.viewRepository = viewRepository;
        this.contentMapper = contentMapper;
    }

    @GetMapping("/feed")
    @Operation(summary = "Get personalized feed",
            description = "Returns a personalized content feed based on user preferences",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feed retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FeedResponse> getFeed(
            @AuthenticationPrincipal UserPrincipal user,

            @Parameter(description = "Selected categories for personalization")
            @RequestParam(required = false) List<String> categories,

            @Parameter(description = "Preferred content types")
            @RequestParam(required = false) List<String> contentTypes,

            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = user != null ? user.getId() : null;
        log.debug("Getting feed for user: {}, categories: {}", userId, categories);

        // Build preferences from request
        UserPreferences preferences = UserPreferences.builder()
                .selectedCategories(categories != null ? categories : List.of())
                .selectedContentTypes(contentTypes != null ? contentTypes : List.of())
                .coldStart(userId == null || (categories == null && contentTypes == null))
                .preferredLanguage("en")
                .build();

        Page<ContentDto> feed = recommendationService.getPersonalizedFeed(userId, preferences, pageable);

        return ResponseEntity.ok(FeedResponse.from(feed));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending content",
            description = "Returns trending content based on recent engagement")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trending content retrieved")
    })
    public ResponseEntity<List<ContentDto>> getTrending(
            @Parameter(description = "Maximum number of items to return")
            @RequestParam(defaultValue = "20") int limit) {

        log.debug("Getting trending content, limit: {}", limit);

        if (limit <= 0 || limit > 100) {
            limit = 20;
        }

        List<ContentDto> trending = recommendationService.getTrendingContent(limit);

        return ResponseEntity.ok(trending);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by ID",
            description = "Returns a specific content post with full details and records view",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content found"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    @Transactional
    public ResponseEntity<ContentDto> getContent(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "Content ID") @PathVariable UUID id,
            @RequestHeader(value = "X-Session-Id", required = false) UUID sessionId) {

        log.debug("Getting content: {}", id);

        ContentPost post = contentPostRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));

        // Record view
        UUID userId = user != null ? user.getId() : null;
        UUID session = sessionId != null ? sessionId : UUID.randomUUID();

        // Only record if session hasn't viewed this content before
        if (!viewRepository.existsBySessionIdAndContentPostId(session, id)) {
            ContentView view = ContentView.builder()
                    .contentPost(post)
                    .userId(userId)
                    .sessionId(session)
                    .viewedAt(LocalDateTime.now())
                    .build();
            viewRepository.save(view);
        }

        // Get engagement data
        long viewCount = viewRepository.countByContentPostIdAndViewedAtAfter(
                id, LocalDateTime.now().minusHours(24));
        long bookmarkCount = bookmarkRepository.countByContentPostId(id);
        boolean isBookmarked = userId != null &&
                bookmarkRepository.existsByUserIdAndContentPostId(userId, id);

        ContentDto dto = contentMapper.toDtoWithEngagement(post, viewCount, bookmarkCount, isBookmarked);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get similar content",
            description = "Returns content similar to the specified item",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Similar content retrieved"),
            @ApiResponse(responseCode = "404", description = "Source content not found")
    })
    public ResponseEntity<List<ContentDto>> getSimilar(
            @Parameter(description = "Source content ID") @PathVariable UUID id,
            @Parameter(description = "Maximum number of items") @RequestParam(defaultValue = "5") int limit) {

        log.debug("Getting similar content for: {}, limit: {}", id, limit);

        if (!contentPostRepository.existsById(id)) {
            throw new ContentNotFoundException(id);
        }

        if (limit <= 0 || limit > 50) {
            limit = 5;
        }

        List<ContentDto> similar = recommendationService.getSimilarContent(id, limit);

        return ResponseEntity.ok(similar);
    }

    @PostMapping("/{id}/bookmark")
    @Operation(summary = "Bookmark content",
            description = "Creates a bookmark for the specified content",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bookmark created"),
            @ApiResponse(responseCode = "404", description = "Content not found"),
            @ApiResponse(responseCode = "409", description = "Already bookmarked")
    })
    @Transactional
    public ResponseEntity<Map<String, String>> createBookmark(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "Content ID") @PathVariable UUID id) {

        log.debug("Creating bookmark for content: {} by user: {}", id, user.getId());

        ContentPost post = contentPostRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));

        // Check if already bookmarked
        if (bookmarkRepository.existsByUserIdAndContentPostId(user.getId(), id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Content already bookmarked"));
        }

        ContentBookmark bookmark = ContentBookmark.builder()
                .userId(user.getId())
                .contentPost(post)
                .build();

        bookmarkRepository.save(bookmark);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Bookmark created"));
    }

    @DeleteMapping("/{id}/bookmark")
    @Operation(summary = "Remove bookmark",
            description = "Removes bookmark for the specified content",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bookmark removed"),
            @ApiResponse(responseCode = "404", description = "Bookmark not found")
    })
    @Transactional
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "Content ID") @PathVariable UUID id) {

        log.debug("Deleting bookmark for content: {} by user: {}", id, user.getId());

        if (!bookmarkRepository.existsByUserIdAndContentPostId(user.getId(), id)) {
            return ResponseEntity.notFound().build();
        }

        bookmarkRepository.deleteByUserIdAndContentPostId(user.getId(), id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Get user bookmarks",
            description = "Returns all bookmarked content for the current user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookmarks retrieved")
    })
    public ResponseEntity<FeedResponse> getBookmarks(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting bookmarks for user: {}", user.getId());

        Page<ContentBookmark> bookmarks = bookmarkRepository
                .findByUserIdWithContent(user.getId(), pageable);

        Page<ContentDto> content = bookmarks.map(bookmark ->
                contentMapper.toDto(bookmark.getContentPost()));

        return ResponseEntity.ok(FeedResponse.from(content));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get content by category",
            description = "Returns content in a specific category",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category content retrieved")
    })
    public ResponseEntity<FeedResponse> getByCategory(
            @Parameter(description = "Category name") @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting content for category: {}", category);

        Page<ContentDto> content = recommendationService.getContentByCategories(
                List.of(category), pageable);

        return ResponseEntity.ok(FeedResponse.from(content));
    }

    @GetMapping("/search")
    @Operation(summary = "Search content",
            description = "Searches content by title or text")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid search query")
    })
    public ResponseEntity<FeedResponse> searchContent(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Searching content: {}", q);

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        Page<ContentDto> results = contentPostRepository.searchByTitleOrContent(q, pageable)
                .map(contentMapper::toDto);

        return ResponseEntity.ok(FeedResponse.from(results));
    }
}
