package com.contentaggregation.content.controller;

import com.contentaggregation.content.dto.response.ContentDto;
import com.contentaggregation.content.dto.response.FeedResponse;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.repository.ContentBookmarkRepository;
import com.contentaggregation.content.repository.ContentPostRepository;
import com.contentaggregation.content.repository.ContentViewRepository;
import com.contentaggregation.content.service.content.ContentSeedingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ContentController using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class ContentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private ContentPostRepository contentPostRepository;

    @Autowired
    private ContentBookmarkRepository bookmarkRepository;

    @Autowired
    private ContentViewRepository viewRepository;

    @Autowired
    private ContentSeedingService seedingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_TOKEN = "test-jwt-token";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void teardownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/content";

        // Clear all data
        viewRepository.deleteAll();
        bookmarkRepository.deleteAll();
        contentPostRepository.deleteAll();

        // Reset WireMock
        wireMockServer.resetAll();

        // Setup auth service mock for valid token
        stubFor(get(urlEqualTo("/api/v1/auth/me"))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                    "id": "%s",
                                    "email": "test@example.com",
                                    "username": "testuser",
                                    "roles": ["USER"]
                                }
                                """, TEST_USER_ID))));

        // Setup auth service mock for invalid token
        stubFor(get(urlEqualTo("/api/v1/auth/me"))
                .withHeader("Authorization", notMatching("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Invalid token\"}")));
    }

    @AfterEach
    void cleanup() {
        viewRepository.deleteAll();
        bookmarkRepository.deleteAll();
        contentPostRepository.deleteAll();
    }

    @Test
    @DisplayName("Get feed returns diverse content")
    void getFeed_returnsDiverseContent() {
        // Seed content
        seedingService.seedContent(50);

        FeedResponse response = given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/feed")
                .then()
                .statusCode(200)
                .extract()
                .as(FeedResponse.class);

        assertNotNull(response);
        assertFalse(response.content().isEmpty());
        assertTrue(response.content().size() <= 20);

        // Verify diversity - not more than 3 consecutive same category
        List<ContentDto> content = response.content();
        int consecutiveCount = 1;
        String lastCategory = null;

        for (ContentDto item : content) {
            String currentCategory = item.getPrimaryCategory();
            if (currentCategory != null && currentCategory.equals(lastCategory)) {
                consecutiveCount++;
                assertTrue(consecutiveCount <= 3,
                        "Found " + consecutiveCount + " consecutive items with category: " + currentCategory);
            } else {
                consecutiveCount = 1;
            }
            lastCategory = currentCategory;
        }
    }

    @Test
    @DisplayName("Get feed requires authentication")
    void getFeed_requiresAuthentication() {
        seedingService.seedContent(10);

        given()
                .when()
                .get("/feed")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Get feed with invalid token returns 401")
    void getFeed_withInvalidToken_returns401() {
        seedingService.seedContent(10);

        given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get("/feed")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Pagination works correctly")
    void getFeed_paginationWorksCorrectly() {
        // Seed 50 items
        seedingService.seedContent(50);

        // Get first page
        FeedResponse page1 = given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/feed")
                .then()
                .statusCode(200)
                .extract()
                .as(FeedResponse.class);

        // Get second page
        FeedResponse page2 = given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/feed")
                .then()
                .statusCode(200)
                .extract()
                .as(FeedResponse.class);

        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(10, page1.content().size());
        assertTrue(page1.pagination().hasNext());
        assertEquals(0, page1.pagination().page());
        assertEquals(1, page2.pagination().page());

        // Ensure different content on different pages
        List<UUID> page1Ids = page1.content().stream().map(ContentDto::getId).toList();
        List<UUID> page2Ids = page2.content().stream().map(ContentDto::getId).toList();

        for (UUID id : page1Ids) {
            assertFalse(page2Ids.contains(id), "Page 2 should not contain items from page 1");
        }
    }

    @Test
    @DisplayName("Trending returns popular content")
    void getTrending_returnsPopularContent() {
        // Seed content and create some views
        seedingService.seedContent(30);

        List<ContentPost> posts = contentPostRepository.findAll();
        assertFalse(posts.isEmpty());

        // No authentication required for trending
        given()
                .queryParam("limit", 10)
                .when()
                .get("/trending")
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(10)));
    }

    @Test
    @DisplayName("Search finds relevant content")
    void searchContent_findsRelevantContent() {
        // Create content with specific title
        ContentPost post = ContentPost.builder()
                .externalId("search-test-" + UUID.randomUUID())
                .source(ContentSource.RSS)
                .title("Unique Search Test Title About Java Programming")
                .content("This is content about Java programming and Spring Boot")
                .contentType(com.contentaggregation.content.enums.ContentType.LONG_ARTICLE)
                .contentLength(100)
                .hasMedia(false)
                .publishedAt(LocalDateTime.now())
                .build();
        contentPostRepository.save(post);

        // Search for content
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("q", "Java Programming")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .body("content[0].title", containsString("Java"));
    }

    @Test
    @DisplayName("Search with short query returns bad request")
    void searchContent_shortQuery_returnsBadRequest() {
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("q", "a")
                .when()
                .get("/search")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Bookmark content succeeds")
    void bookmarkContent_succeeds() {
        // Create content
        ContentPost post = createTestPost("Bookmark Test");

        // Bookmark it
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .post("/{id}/bookmark", post.getId())
                .then()
                .statusCode(201)
                .body("message", equalTo("Bookmark created"));

        // Verify bookmark exists
        assertTrue(bookmarkRepository.existsByUserIdAndContentPostId(
                UUID.fromString(TEST_USER_ID), post.getId()));
    }

    @Test
    @DisplayName("Bookmark same content twice returns conflict")
    void bookmarkContent_twice_returnsConflict() {
        ContentPost post = createTestPost("Double Bookmark Test");

        // First bookmark
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .post("/{id}/bookmark", post.getId())
                .then()
                .statusCode(201);

        // Second bookmark - should conflict
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .post("/{id}/bookmark", post.getId())
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Delete bookmark succeeds")
    void deleteBookmark_succeeds() {
        ContentPost post = createTestPost("Delete Bookmark Test");

        // Create bookmark
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .post("/{id}/bookmark", post.getId())
                .then()
                .statusCode(201);

        // Delete bookmark
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .delete("/{id}/bookmark", post.getId())
                .then()
                .statusCode(204);

        // Verify deleted
        assertFalse(bookmarkRepository.existsByUserIdAndContentPostId(
                UUID.fromString(TEST_USER_ID), post.getId()));
    }

    @Test
    @DisplayName("Get bookmarks returns user's bookmarks")
    void getBookmarks_returnsUserBookmarks() {
        // Create and bookmark multiple posts
        ContentPost post1 = createTestPost("Bookmarked Post 1");
        ContentPost post2 = createTestPost("Bookmarked Post 2");

        given().header("Authorization", "Bearer " + TEST_TOKEN)
                .when().post("/{id}/bookmark", post1.getId()).then().statusCode(201);
        given().header("Authorization", "Bearer " + TEST_TOKEN)
                .when().post("/{id}/bookmark", post2.getId()).then().statusCode(201);

        // Get bookmarks
        FeedResponse response = given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .get("/bookmarks")
                .then()
                .statusCode(200)
                .extract()
                .as(FeedResponse.class);

        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("View content increments view count")
    void getContent_incrementsViewCount() {
        ContentPost post = createTestPost("View Count Test");
        UUID sessionId = UUID.randomUUID();

        // First view
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .header("X-Session-Id", sessionId.toString())
                .when()
                .get("/{id}", post.getId())
                .then()
                .statusCode(200);

        // Verify view recorded
        long viewCount = viewRepository.countByContentPostIdAndViewedAtAfter(
                post.getId(), LocalDateTime.now().minusHours(1));
        assertEquals(1, viewCount);

        // Second view with same session - should not increment
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .header("X-Session-Id", sessionId.toString())
                .when()
                .get("/{id}", post.getId())
                .then()
                .statusCode(200);

        viewCount = viewRepository.countByContentPostIdAndViewedAtAfter(
                post.getId(), LocalDateTime.now().minusHours(1));
        assertEquals(1, viewCount, "Same session should not increment view count");

        // Third view with different session - should increment
        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .header("X-Session-Id", UUID.randomUUID().toString())
                .when()
                .get("/{id}", post.getId())
                .then()
                .statusCode(200);

        viewCount = viewRepository.countByContentPostIdAndViewedAtAfter(
                post.getId(), LocalDateTime.now().minusHours(1));
        assertEquals(2, viewCount);
    }

    @Test
    @DisplayName("Get content by non-existent ID returns 404")
    void getContent_notFound_returns404() {
        UUID randomId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .when()
                .get("/{id}", randomId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Get similar content returns related items")
    void getSimilarContent_returnsRelatedItems() {
        seedingService.seedContent(30);

        ContentPost post = contentPostRepository.findAll().get(0);

        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("limit", 5)
                .when()
                .get("/{id}/similar", post.getId())
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("Get content by category returns filtered results")
    void getByCategory_returnsFilteredResults() {
        seedingService.seedContent(50);

        given()
                .header("Authorization", "Bearer " + TEST_TOKEN)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/category/{category}", "technology")
                .then()
                .statusCode(200)
                .body("content", not(empty()));
    }

    private ContentPost createTestPost(String title) {
        ContentPost post = ContentPost.builder()
                .externalId("test-" + UUID.randomUUID())
                .source(ContentSource.RSS)
                .title(title)
                .content("Test content for " + title)
                .contentType(com.contentaggregation.content.enums.ContentType.LONG_ARTICLE)
                .contentLength(100)
                .hasMedia(false)
                .publishedAt(LocalDateTime.now())
                .build();
        return contentPostRepository.save(post);
    }
}
