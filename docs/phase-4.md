# Phase 4: User Onboarding Service

## Overview

Implement a comprehensive user onboarding flow that collects user preferences, interests, and content type selections during first-time setup. This service creates the initial user profile that will be refined by the metrics service over time.

**Duration**: 2 days
**Complexity**: Medium
**Priority**: High (improves cold-start recommendations)

---

## Objectives

### Primary Goals
1. ✅ Create onboarding flow API for new users
2. ✅ Collect category preferences (interests selection)
3. ✅ Collect content type preferences (articles, videos, short posts)
4. ✅ Store onboarding data in user profiles
5. ✅ Integrate with metrics service for profile initialization
6. ✅ Provide onboarding status tracking
7. ✅ Support skippable onboarding steps
8. ✅ Validate preference selections

### Secondary Goals
1. ✅ A/B testing support for onboarding flows
2. ✅ Analytics for onboarding completion rates
3. ✅ Multi-language support for categories
4. ✅ Personalized onboarding recommendations
5. ✅ Skip onboarding option with smart defaults

---

## Technical Stack

### Core Technologies
- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.1+
- **Spring Data JPA**: Repository layer
- **PostgreSQL**: 15+ (onboarding data storage)
- **Redis**: 7+ (caching, session management)
- **Flyway**: Database migrations

### Integration
- **Spring WebClient**: Communication with metrics-service
- **Circuit Breaker**: Resilience4j for fault tolerance

---

## Architecture Overview

### Service Integration Options

**Option 1: Integrated into Auth Service (Recommended)**
- Add onboarding endpoints to existing auth-service
- Simpler deployment (no new service)
- Tight coupling with user registration flow
- Direct access to user data

**Option 2: Standalone Onboarding Service**
- Separate onboarding-service on port 8083
- Better separation of concerns
- Independent scaling
- More microservices overhead

**For this phase, we'll use Option 1 (extend auth-service).**

### Package Structure (in auth-service)
```
auth-service/src/main/java/com/contentaggregation/auth/
├── onboarding/
│   ├── controller/
│   │   └── OnboardingController.java
│   ├── service/
│   │   ├── OnboardingService.java
│   │   └── PreferenceValidationService.java
│   ├── repository/
│   │   ├── OnboardingStatusRepository.java
│   │   └── CategoryOptionRepository.java
│   ├── entity/
│   │   ├── OnboardingStatus.java
│   │   └── CategoryOption.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CategorySelectionRequest.java
│   │   │   ├── ContentTypeSelectionRequest.java
│   │   │   └── CompleteOnboardingRequest.java
│   │   └── response/
│   │       ├── OnboardingStatusResponse.java
│   │       └── CategoryOptionsResponse.java
│   └── client/
│       └── MetricsServiceClient.java
```

---

## Database Schema

### onboarding_status Table
```sql
CREATE TABLE onboarding_status (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    
    -- Progress tracking
    step VARCHAR(50) NOT NULL DEFAULT 'not_started',
    completed BOOLEAN DEFAULT FALSE,
    skipped BOOLEAN DEFAULT FALSE,
    
    -- Selected preferences
    selected_categories TEXT[] DEFAULT ARRAY[]::TEXT[],
    selected_content_types TEXT[] DEFAULT ARRAY[]::TEXT[],
    
    -- Metadata
    onboarding_version VARCHAR(10) DEFAULT 'v1',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_onboarding_user_id ON onboarding_status(user_id);
CREATE INDEX idx_onboarding_completed ON onboarding_status(completed);
```

### category_options Table
```sql
CREATE TABLE category_options (
    id UUID PRIMARY KEY,
    category_key VARCHAR(50) UNIQUE NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Seed data for categories
INSERT INTO category_options (id, category_key, category_name, description, display_order) VALUES
(gen_random_uuid(), 'technology', 'Technology', 'AI, Programming, Gadgets, Startups', 1),
(gen_random_uuid(), 'science', 'Science', 'Physics, Biology, Space, Research', 2),
(gen_random_uuid(), 'business', 'Business', 'Finance, Marketing, Entrepreneurship', 3),
(gen_random_uuid(), 'entertainment', 'Entertainment', 'Movies, Music, Gaming, TV Shows', 4),
(gen_random_uuid(), 'sports', 'Sports', 'Football, Basketball, Esports', 5),
(gen_random_uuid(), 'lifestyle', 'Lifestyle', 'Travel, Food, Fashion, Health', 6),
(gen_random_uuid(), 'education', 'Education', 'Courses, Tutorials, Books, Learning', 7),
(gen_random_uuid(), 'news', 'News', 'World News, Politics, Local Events', 8);
```

---

## Onboarding Flow

### Step-by-Step Process

**Step 1: Welcome Screen (Frontend)**
- Show welcome message
- Explain personalization benefits
- "Get Started" or "Skip for now" buttons

**Step 2: Category Selection (API: POST /api/v1/onboarding/categories)**
- Display 8 category cards
- User selects 3-5 categories
- Minimum 3, maximum 5 required
- Visual feedback on selection

**Step 3: Content Type Selection (API: POST /api/v1/onboarding/content-types)**
- Options:
  - Short Posts (< 500 chars)
  - Medium Articles (500-2000 chars)
  - Long Articles (> 2000 chars)
  - Video Content
  - Image Posts
- User selects 1-3 types
- Default: all types if none selected

**Step 4: Completion (API: POST /api/v1/onboarding/complete)**
- Save all preferences
- Create initial user profile in metrics-service
- Mark onboarding as complete
- Redirect to main feed

**Alternative: Skip Onboarding (API: POST /api/v1/onboarding/skip)**
- Mark onboarding as skipped
- Apply smart defaults (all categories, all content types)
- Still create profile with neutral preferences

---

## API Endpoints

### Onboarding Endpoints (Auth Service)

```
GET    /api/v1/onboarding/status           - Get onboarding status
GET    /api/v1/onboarding/categories       - Get available categories
POST   /api/v1/onboarding/categories       - Save category selections
POST   /api/v1/onboarding/content-types    - Save content type selections
POST   /api/v1/onboarding/complete         - Complete onboarding
POST   /api/v1/onboarding/skip             - Skip onboarding
PUT    /api/v1/onboarding/reset            - Reset onboarding (start over)
```

---

## Integration with Metrics Service

### Profile Initialization

When onboarding is completed:

1. **Auth Service** → **Metrics Service**:
   ```
   POST /api/v1/profiles/{userId}/initialize
   {
     "selectedCategories": ["technology", "science", "business"],
     "selectedContentTypes": ["MEDIUM_POST", "LONG_ARTICLE"],
     "onboardingVersion": "v1",
     "timestamp": 1700000000000
   }
   ```

2. **Metrics Service** creates user profile:
   - Initialize category preferences with equal weights for selected categories
   - Set content type preferences based on selections
   - Set default values for other fields
   - Mark as onboarding-initialized

### Category Preference Initialization

```java
// In Metrics Service
public void initializeProfileFromOnboarding(
    UUID userId,
    List<String> selectedCategories,
    List<String> selectedContentTypes
) {
    UserProfile profile = new UserProfile();
    profile.setUserId(userId);
    
    // Initialize category preferences
    Map<String, Double> categoryPrefs = new HashMap<>();
    double initialScore = 0.7; // Medium confidence from onboarding
    
    for (String category : selectedCategories) {
        categoryPrefs.put(category, initialScore);
    }
    
    profile.setCategoryPreferencesFromMap(categoryPrefs);
    
    // Initialize content type preferences
    Map<String, Double> contentTypePrefs = new HashMap<>();
    for (String type : selectedContentTypes) {
        contentTypePrefs.put(type, 0.8);
    }
    
    profile.setContentTypePreferencesFromMap(contentTypePrefs);
    
    // Set defaults
    profile.setClickbaitTolerance(0.5f);
    profile.setExplorationVsExploitation(0.6f); // Lean toward exploration for new users
    
    profileRepository.save(profile);
}
```

---

## Onboarding Steps Enum

```java
public enum OnboardingStep {
    NOT_STARTED,
    CATEGORY_SELECTION,
    CONTENT_TYPE_SELECTION,
    COMPLETED,
    SKIPPED
}
```

---

## Success Criteria

### Functional Requirements
- ✅ User can complete onboarding flow
- ✅ User can skip onboarding
- ✅ Category selections validated (3-5 required)
- ✅ Content type selections saved
- ✅ Profile initialized in metrics-service
- ✅ Onboarding status tracked
- ✅ User can reset and restart onboarding

### Non-Functional Requirements
- ✅ API response time < 200ms
- ✅ Onboarding completion tracked for analytics
- ✅ All endpoints documented (OpenAPI)
- ✅ Integration tests cover all flows
- ✅ Graceful degradation if metrics-service unavailable

### User Experience
- ✅ Clear progress indication
- ✅ Helpful descriptions for categories
- ✅ Skip option always available
- ✅ No data loss if user closes browser
- ✅ Can go back and change selections

---

## Implementation Steps (Prompts)

Phase 4 consists of 3 sequential prompts:

### Prompt 1: Database Schema & Entities (45 min)
- Create Flyway migration for onboarding tables
- Create OnboardingStatus and CategoryOption entities
- Create repositories
- Seed category data

### Prompt 2: Onboarding API & Service Logic (60 min)
- Implement OnboardingService
- Create OnboardingController with all endpoints
- Add preference validation
- Integrate with metrics-service

### Prompt 3: Testing & Documentation (45 min)
- Write integration tests
- Add analytics metrics
- Document API endpoints
- Create frontend integration guide

---

## Configuration Requirements

### Environment Variables

Add to auth-service:
```bash
# Metrics Service Integration
METRICS_SERVICE_URL=http://localhost:8082
METRICS_SERVICE_TIMEOUT_MS=5000

# Onboarding Configuration
ONBOARDING_MIN_CATEGORIES=3
ONBOARDING_MAX_CATEGORIES=5
ONBOARDING_VERSION=v1
```

---

## Monitoring & Analytics

### Custom Metrics
- `onboarding.started.total` - Counter
- `onboarding.completed.total` - Counter
- `onboarding.skipped.total` - Counter
- `onboarding.category_selected.total` - Counter by category
- `onboarding.completion_time_seconds` - Timer

### Analytics Events
Track for product analytics:
- Onboarding started
- Step completed
- Categories selected (which ones)
- Content types selected
- Onboarding completed
- Onboarding skipped
- Time spent per step

---

## A/B Testing Support

### Onboarding Variants

Support multiple onboarding flows:
- **v1**: Standard flow (categories → content types)
- **v2**: Reverse flow (content types → categories)
- **v3**: Combined single-step selection

Store `onboarding_version` to track which variant user saw.

---

## Future Enhancements (Post Phase 4)

- Machine learning-based category recommendations
- Social proof (show popular categories)
- Personalized category descriptions
- Image-based category selection
- Sample content preview during onboarding
- Friend recommendations integration
- Import preferences from other platforms

---

## Phase Completion Checklist

Before moving to production:

### Code Complete
- [ ] All entities created
- [ ] All endpoints implemented
- [ ] Metrics service integration working
- [ ] Validation logic complete

### Tests Passing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Can complete full onboarding flow
- [ ] Can skip onboarding
- [ ] Profile initialized correctly

### Documentation
- [ ] API endpoints documented
- [ ] Frontend integration guide complete
- [ ] Analytics events documented

### Deployment
- [ ] Flyway migration tested
- [ ] Category data seeded
- [ ] Metrics tracked
- [ ] Ready for frontend integration

---

## Next Phase Preview

After Phase 4, the core backend is complete! The system will have:
- ✅ Authentication & Authorization (Phase 1)
- ✅ Content Service with Recommendations (Phase 2)
- ✅ Metrics Collection & User Profiling (Phase 3)
- ✅ User Onboarding (Phase 4)

**Optional future phases:**
- **Phase 5**: ML-based Recommender System (replace stub)
- **Phase 6**: Admin Dashboard & Analytics
- **Phase 7**: Real-time Notifications Service
- **Phase 8**: Content Ingestion Pipelines