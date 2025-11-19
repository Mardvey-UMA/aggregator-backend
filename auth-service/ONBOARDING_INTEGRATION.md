# Frontend Onboarding Integration Guide

## Overview
User onboarding flow collects preferences during first-time setup.

## Flow
1. User registers → Check onboarding status
2. If not completed → Show onboarding screens
3. Categories selection → Content types selection → Complete
4. Alternative: Skip button → Apply defaults

## API Endpoints

### 1. Get Onboarding Status
```
GET /api/v1/onboarding/status
Authorization: Bearer {jwt}

Response:
{
  "userId": "uuid",
  "currentStep": "NOT_STARTED",
  "completed": false,
  "skipped": false,
  "selectedCategories": [],
  "selectedContentTypes": []
}
```

### 2. Get Available Categories
```
GET /api/v1/onboarding/categories

Response:
[
  {
    "key": "technology",
    "name": "Technology",
    "description": "AI, Programming, Gadgets",
    "displayOrder": 1
  },
  ...
]
```

### 3. Save Category Selections
```
POST /api/v1/onboarding/categories
Authorization: Bearer {jwt}

Request:
{
  "selectedCategories": ["technology", "science", "business"]
}

Response: Updated status
```

### 4. Save Content Type Selections
```
POST /api/v1/onboarding/content-types
Authorization: Bearer {jwt}

Request:
{
  "selectedContentTypes": ["MEDIUM_POST", "LONG_ARTICLE"]
}
```

### 5. Complete Onboarding
```
POST /api/v1/onboarding/complete
Authorization: Bearer {jwt}

Request:
{
  "selectedCategories": ["technology", "science", "business"],
  "selectedContentTypes": ["MEDIUM_POST", "LONG_ARTICLE"]
}
```

### 6. Skip Onboarding
```
POST /api/v1/onboarding/skip
Authorization: Bearer {jwt}

Response: Status with all defaults applied
```

## UI Guidelines

### Category Selection Screen
- Show 8 category cards in grid
- User selects 3-5 categories
- Disable "Continue" button until min 3 selected
- Show counter: "3/5 selected"
- "Skip" button always available

### Content Type Selection Screen
- Show 5 content type options
- User selects 1+ types
- Pre-select all by default
- "Skip" button available

### Validation
- Min 3, max 5 categories
- At least 1 content type
- Show validation errors from API

## Error Handling
- 400: Validation error → Show error message
- 401: Unauthorized → Redirect to login
- 503: Service unavailable → Show retry option

## Analytics Events to Track
- Onboarding started
- Category selected (which ones)
- Content type selected
- Onboarding completed
- Onboarding skipped
- Time spent on each step

