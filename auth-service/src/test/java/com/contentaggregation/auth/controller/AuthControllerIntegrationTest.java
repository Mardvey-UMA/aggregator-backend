package com.contentaggregation.auth.controller;

import com.contentaggregation.auth.dto.request.LoginRequest;
import com.contentaggregation.auth.dto.request.RefreshTokenRequest;
import com.contentaggregation.auth.dto.request.RegisterRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/auth";
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "newuser",
                "Password123"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("user.email", equalTo("newuser@example.com"));
    }

    @Test
    @DisplayName("Should fail registration with existing email")
    void shouldFailRegistrationWithExistingEmail() {
        // Register first user
        RegisterRequest firstUser = new RegisterRequest(
                "duplicate@example.com",
                "firstuser",
                "Password123"
        );

        given()
            .contentType(ContentType.JSON)
            .body(firstUser)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.CREATED.value());

        // Try to register with same email
        RegisterRequest duplicateEmail = new RegisterRequest(
                "duplicate@example.com",
                "seconduser",
                "Password123"
        );

        given()
            .contentType(ContentType.JSON)
            .body(duplicateEmail)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Should fail registration with invalid password")
    void shouldFailRegistrationWithInvalidPassword() {
        RegisterRequest request = new RegisterRequest(
                "invalidpass@example.com",
                "invalidpass",
                "weak"  // Too short, no uppercase, no digit
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should login with valid credentials")
    void shouldLoginWithValidCredentials() {
        // First register
        RegisterRequest register = new RegisterRequest(
                "login@example.com",
                "loginuser",
                "Password123"
        );

        given()
            .contentType(ContentType.JSON)
            .body(register)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.CREATED.value());

        // Then login
        LoginRequest login = new LoginRequest("login@example.com", "Password123");

        given()
            .contentType(ContentType.JSON)
            .body(login)
        .when()
            .post("/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() {
        LoginRequest login = new LoginRequest(
                "nonexistent@example.com",
                "WrongPassword123"
        );

        given()
            .contentType(ContentType.JSON)
            .body(login)
        .when()
            .post("/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshToken() {
        // Register and get tokens
        RegisterRequest register = new RegisterRequest(
                "refresh@example.com",
                "refreshuser",
                "Password123"
        );

        Response registerResponse = given()
            .contentType(ContentType.JSON)
            .body(register)
        .when()
            .post("/register")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .response();

        String refreshToken = registerResponse.jsonPath().getString("refreshToken");

        // Refresh the token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        given()
            .contentType(ContentType.JSON)
            .body(refreshRequest)
        .when()
            .post("/refresh")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("Should fail refresh with invalid token")
    void shouldFailRefreshWithInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/refresh")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
