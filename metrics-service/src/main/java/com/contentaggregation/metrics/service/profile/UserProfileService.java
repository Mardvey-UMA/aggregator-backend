package com.contentaggregation.metrics.service.profile;

import com.contentaggregation.metrics.dto.event.UserEventPayload;
import com.contentaggregation.metrics.dto.response.UserProfileDto;
import com.contentaggregation.metrics.entity.UserEvent;
import com.contentaggregation.metrics.entity.UserProfile;
import com.contentaggregation.metrics.exception.ProfileNotFoundException;
import com.contentaggregation.metrics.repository.UserEventRepository;
import com.contentaggregation.metrics.repository.UserProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final String PROFILE_CACHE = "user-profiles";
    private static final String PREFERENCES_CACHE = "user-profile-preferences";

    private final UserProfileRepository profileRepo;
    private final UserEventRepository userEventRepository;
    private final ProfileBuilderService profileBuilderService;
    private final ObjectMapper objectMapper;
    private final Cache profileCache;
    private final Cache preferencesCache;

    public UserProfileService(
        UserProfileRepository profileRepo,
        UserEventRepository userEventRepository,
        ProfileBuilderService profileBuilderService,
        ObjectMapper objectMapper,
        CacheManager cacheManager
    ) {
        this.profileRepo = profileRepo;
        this.userEventRepository = userEventRepository;
        this.profileBuilderService = profileBuilderService;
        this.objectMapper = objectMapper;
        this.profileCache = cacheManager != null ? cacheManager.getCache(PROFILE_CACHE) : null;
        this.preferencesCache = cacheManager != null ? cacheManager.getCache(PREFERENCES_CACHE) : null;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getProfile(UUID userId) {
        UserProfileDto cached = getCachedProfile(userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return profileRepo.findByUserId(userId)
            .map(this::toDto)
            .map(dto -> cacheProfile(userId, dto));
    }

    @Transactional
    public UserProfileDto getOrCreateProfile(UUID userId) {
        return getProfile(userId)
            .orElseGet(() -> {
                UserProfile created = profileRepo.save(createDefaultProfile(userId));
                return cacheProfile(userId, toDto(created));
            });
    }

    @Transactional(readOnly = true)
    public Map<String, Double> getCategoryPreferences(UUID userId) {
        Map<String, Double> cached = getCachedPreferences(userId);
        if (cached != null) {
            return cached;
        }
        Map<String, Double> preferences = profileRepo.findByUserId(userId)
            .map(UserProfile::getCategoryPreferencesMap)
            .orElse(Map.of());
        return cachePreferences(userId, preferences);
    }

    @Transactional
    public void updatePreferencesManually(UUID userId, Map<String, Double> categories) {
        UserProfile profile = profileRepo.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException(userId));
        profile.setCategoryPreferencesFromMap(categories != null ? categories : Map.of());
        UserProfile saved = profileRepo.save(profile);
        evictCaches(userId);
        cacheProfile(userId, toDto(saved));
        cachePreferences(userId, saved.getCategoryPreferencesMap());
    }

    @Transactional
    public void deleteProfile(UUID userId) {
        profileRepo.deleteByUserId(userId);
        evictCaches(userId);
    }

    @Transactional
    public int recalculateAllProfiles() {
        List<UserEvent> allEvents = userEventRepository.findAll();
        if (allEvents.isEmpty()) {
            profileRepo.deleteAllInBatch();
            clearCaches();
            return 0;
        }

        Map<UUID, List<UserEventPayload>> grouped = allEvents.stream()
            .sorted(Comparator.comparing(UserEvent::getTimestamp))
            .collect(Collectors.groupingBy(
                UserEvent::getUserId,
                LinkedHashMap::new,
                Collectors.mapping(this::deserializeEvent, Collectors.toList())
            ));

        profileRepo.deleteAllInBatch();
        clearCaches();

        grouped.forEach((userId, payloads) -> payloads.stream()
            .filter(Objects::nonNull)
            .forEach(profileBuilderService::processEvent));

        return grouped.size();
    }

    @Transactional
    public byte[] exportProfiles(String format) {
        List<UserProfileDto> profiles = profileRepo.findAll().stream()
            .map(this::toDto)
            .toList();
        String normalizedFormat = (format == null || format.isBlank()) ? "csv" : format.toLowerCase();

        try {
            if ("json".equals(normalizedFormat)) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(profiles);
            }
            return buildCsv(profiles).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to export profiles", ex);
        }
    }

    @Transactional
    public void replayEvents(UUID userId, LocalDateTime fromDate) {
        LocalDateTime from = fromDate != null ? fromDate : LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
        List<UserEventPayload> payloads = userEventRepository.findEventsWithinRange(userId, from, to).stream()
            .sorted(Comparator.comparing(UserEvent::getTimestamp))
            .map(this::deserializeEvent)
            .filter(Objects::nonNull)
            .toList();

        profileRepo.deleteByUserId(userId);
        evictCaches(userId);

        if (payloads.isEmpty()) {
            log.info("No events available to replay for user {}", userId);
            cacheProfile(userId, toDto(profileRepo.save(createDefaultProfile(userId))));
            return;
        }

        payloads.forEach(profileBuilderService::processEvent);
        evictCaches(userId);
        getProfile(userId);
    }

    private UserProfileDto toDto(UserProfile profile) {
        return UserProfileDto.fromEntity(profile);
    }

    private UserEventPayload deserializeEvent(UserEvent event) {
        try {
            return objectMapper.readValue(event.getEventData(), UserEventPayload.class);
        } catch (JsonProcessingException ex) {
            log.error(
                "Failed to deserialize event {} for user {}: {}",
                event.getId(),
                event.getUserId(),
                ex.getMessage()
            );
            return null;
        }
    }

    private UserProfile createDefaultProfile(UUID userId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return UserProfile.builder()
            .userId(userId)
            .lastUpdated(now)
            .build();
    }

    private UserProfileDto getCachedProfile(UUID userId) {
        if (profileCache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = profileCache.get(userId);
        if (wrapper == null) {
            return null;
        }
        return (UserProfileDto) wrapper.get();
    }

    private Map<String, Double> getCachedPreferences(UUID userId) {
        if (preferencesCache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = preferencesCache.get(userId);
        if (wrapper == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Double> cached = (Map<String, Double>) wrapper.get();
        return cached;
    }

    private UserProfileDto cacheProfile(UUID userId, UserProfileDto dto) {
        if (profileCache != null && dto != null) {
            profileCache.put(userId, dto);
        }
        return dto;
    }

    private Map<String, Double> cachePreferences(UUID userId, Map<String, Double> preferences) {
        Map<String, Double> immutable = preferences.isEmpty() ? Map.of() : Map.copyOf(preferences);
        if (preferencesCache != null) {
            preferencesCache.put(userId, immutable);
        }
        return immutable;
    }

    private void evictCaches(UUID userId) {
        if (profileCache != null) {
            profileCache.evict(userId);
        }
        if (preferencesCache != null) {
            preferencesCache.evict(userId);
        }
    }

    private void clearCaches() {
        if (profileCache != null) {
            profileCache.clear();
        }
        if (preferencesCache != null) {
            preferencesCache.clear();
        }
    }

    private String buildCsv(List<UserProfileDto> profiles) throws JsonProcessingException {
        String header = String.join(",",
            "userId",
            "lastUpdated",
            "totalViews",
            "totalClicks",
            "totalLikes",
            "totalDislikes",
            "totalBookmarks",
            "categoryPreferences",
            "entityPreferences",
            "contentTypePreferences",
            "avgDwellTimeSeconds",
            "avgSessionLengthSeconds",
            "activeHours",
            "preferredContentLength",
            "clickbaitTolerance",
            "explorationVsExploitation",
            "recentLikedPosts",
            "recentViewedPosts"
        );

        StringBuilder builder = new StringBuilder(header).append('\n');
        for (UserProfileDto dto : profiles) {
            builder.append(dto.userId()).append(',')
                .append(nullSafe(dto.lastUpdated())).append(',')
                .append(dto.totalViews()).append(',')
                .append(dto.totalClicks()).append(',')
                .append(dto.totalLikes()).append(',')
                .append(dto.totalDislikes()).append(',')
                .append(dto.totalBookmarks()).append(',')
                .append(quoteJson(dto.categoryPreferences())).append(',')
                .append(quoteJson(dto.entityPreferences())).append(',')
                .append(quoteJson(dto.contentTypePreferences())).append(',')
                .append(dto.avgDwellTimeSeconds()).append(',')
                .append(dto.avgSessionLengthSeconds()).append(',')
                .append(quoteJson(dto.activeHours())).append(',')
                .append(nullSafe(dto.preferredContentLength())).append(',')
                .append(dto.clickbaitTolerance()).append(',')
                .append(dto.explorationVsExploitation()).append(',')
                .append(quoteJson(dto.recentLikedPosts())).append(',')
                .append(quoteJson(dto.recentViewedPosts()))
                .append('\n');
        }
        return builder.toString();
    }

    private String quoteJson(Object value) throws JsonProcessingException {
        String serialized = objectMapper.writeValueAsString(value);
        return "\"" + serialized.replace("\"", "\"\"") + "\"";
    }

    private String nullSafe(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}


