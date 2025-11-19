package com.contentaggregation.auth.onboarding;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.onboarding.repository.OnboardingStatusRepository;
import com.contentaggregation.auth.repository.UserRepository;
import com.contentaggregation.auth.security.UserPrincipal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "metrics.service.url=http://localhost:${wiremock.server.port}")
class OnboardingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OnboardingStatusRepository onboardingStatusRepository;

    @BeforeEach
    void resetStubs() {
        reset();
        WireMock.stubFor(WireMock.post(urlMatching("/api/v1/profiles/.*/initialize"))
                .willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void cleanDatabase() {
        onboardingStatusRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCompleteFullOnboardingFlow() throws Exception {
        User user = persistUser();

        postCategories(user, List.of("technology", "science", "business"))
                .andExpect(status().isOk());

        postContentTypes(user, List.of("MEDIUM_POST", "LONG_ARTICLE"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectedCategories":["technology","science","business"],
                                  "selectedContentTypes":["MEDIUM_POST","LONG_ARTICLE"]
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult statusResult = mockMvc.perform(get("/api/v1/onboarding/status").with(auth(user)))
                .andExpect(status().isOk())
                .andReturn();

        OnboardingStatusResponseDto response = parseStatus(statusResult);
        assertThat(response.completed()).isTrue();
        assertThat(response.currentStep()).isEqualTo("COMPLETED");
        assertThat(response.selectedCategories()).contains("technology", "science", "business");

        WireMock.verify(1, postRequestedFor(urlMatching("/api/v1/profiles/.*/initialize")));
    }

    @Test
    void shouldSkipOnboardingWithDefaults() throws Exception {
        User user = persistUser();

        mockMvc.perform(post("/api/v1/onboarding/skip").with(auth(user)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/onboarding/status").with(auth(user)))
                .andExpect(status().isOk())
                .andReturn();

        OnboardingStatusResponseDto status = parseStatus(result);
        assertThat(status.skipped()).isTrue();
        assertThat(status.selectedCategories()).isNotEmpty();
        assertThat(status.selectedContentTypes()).contains("SHORT_POST", "MEDIUM_POST", "LONG_ARTICLE", "VIDEO_POST", "IMAGE_POST");
    }

    @Test
    void shouldResetOnboarding() throws Exception {
        User user = persistUser();

        postCategories(user, List.of("technology", "science", "business"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/onboarding/reset").with(auth(user)))
                .andExpect(status().isOk());

        MvcResult statusResult = mockMvc.perform(get("/api/v1/onboarding/status").with(auth(user)))
                .andExpect(status().isOk())
                .andReturn();

        OnboardingStatusResponseDto status = parseStatus(statusResult);
        assertThat(status.currentStep()).isEqualTo("NOT_STARTED");
        assertThat(status.selectedCategories()).isEmpty();
    }

    @Test
    void shouldValidateCategorySelectionLimits() throws Exception {
        User user = persistUser();

        postCategories(user, List.of("technology", "science"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidCategory() throws Exception {
        User user = persistUser();

        postCategories(user, List.of("technology", "invalid_key", "business"))
                .andExpect(status().isBadRequest());
    }

    private User persistUser() {
        return userRepository.save(User.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .username("user-" + UUID.randomUUID())
                .passwordHash("secret")
                .build());
    }

    private RequestPostProcessor auth(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(token);
    }

    private OnboardingStatusResponseDto parseStatus(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), OnboardingStatusResponseDto.class);
    }

    private org.springframework.test.web.servlet.ResultActions postCategories(User user, List<String> categories) throws Exception {
        String payload = objectMapper.writeValueAsString(new CategoryRequest(categories));
        return mockMvc.perform(post("/api/v1/onboarding/categories")
                .with(auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private org.springframework.test.web.servlet.ResultActions postContentTypes(User user, List<String> contentTypes) throws Exception {
        String payload = objectMapper.writeValueAsString(new ContentTypeRequest(contentTypes));
        return mockMvc.perform(post("/api/v1/onboarding/content-types")
                .with(auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private record CategoryRequest(List<String> selectedCategories) {
    }

    private record ContentTypeRequest(List<String> selectedContentTypes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OnboardingStatusResponseDto(
            String userId,
            String currentStep,
            boolean completed,
            boolean skipped,
            List<String> selectedCategories,
            List<String> selectedContentTypes) {
    }
}

