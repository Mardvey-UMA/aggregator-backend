package com.contentaggregation.content.service.content;

import com.contentaggregation.content.entity.ContentMetadata;
import com.contentaggregation.content.entity.ContentPost;
import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.ContentType;
import com.contentaggregation.content.enums.MediaType;
import com.contentaggregation.content.enums.Sentiment;
import com.contentaggregation.content.mapper.ContentMapper;
import com.contentaggregation.content.repository.ContentPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for generating realistic test content.
 *
 * <p>Creates diverse content posts with metadata for testing and development.
 */
@Service
public class ContentSeedingService {

    private static final Logger log = LoggerFactory.getLogger(ContentSeedingService.class);

    private final ContentPostRepository contentPostRepository;
    private final ContentMapper contentMapper;

    // Content templates
    private static final String[] CATEGORIES = {
            "technology", "science", "business", "entertainment",
            "sports", "lifestyle", "education", "news"
    };

    private static final String[] TECH_TOPICS = {
            "AI and Machine Learning", "Cloud Computing", "Cybersecurity",
            "Web Development", "Mobile Apps", "DevOps", "Blockchain", "IoT"
    };

    private static final String[] SCIENCE_TOPICS = {
            "Space Exploration", "Climate Change", "Quantum Physics",
            "Biotechnology", "Neuroscience", "Renewable Energy"
    };

    private static final String[] BUSINESS_TOPICS = {
            "Startups", "Venture Capital", "Market Analysis",
            "Leadership", "Remote Work", "Cryptocurrency"
    };

    private static final String[] CHANNELS = {
            "Tech Daily", "Science News", "Business Insider", "Entertainment Weekly",
            "Sports Central", "Lifestyle Tips", "Learning Hub", "World News"
    };

    private static final String[] PERSON_NAMES = {
            "Elon Musk", "Tim Cook", "Satya Nadella", "Jensen Huang",
            "Sam Altman", "Sundar Pichai", "Mark Zuckerberg"
    };

    private static final String[] COMPANY_NAMES = {
            "Apple", "Google", "Microsoft", "Tesla", "OpenAI",
            "NVIDIA", "Amazon", "Meta", "Netflix"
    };

    private static final String[] LOCATIONS = {
            "Silicon Valley", "New York", "London", "Tokyo", "Berlin"
    };

    public ContentSeedingService(ContentPostRepository contentPostRepository,
                                  ContentMapper contentMapper) {
        this.contentPostRepository = contentPostRepository;
        this.contentMapper = contentMapper;
    }

    /**
     * Seeds the database with realistic test content.
     *
     * @param count number of content items to generate
     * @return number of items created
     */
    @Transactional
    public int seedContent(int count) {
        log.info("Starting content seeding: {} items", count);

        List<ContentPost> posts = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            ContentPost post = generateContentPost(i, random);
            posts.add(post);

            // Batch save every 100 items
            if (posts.size() >= 100) {
                contentPostRepository.saveAll(posts);
                posts.clear();
                log.debug("Saved batch of 100 items, progress: {}/{}", i + 1, count);
            }
        }

        // Save remaining items
        if (!posts.isEmpty()) {
            contentPostRepository.saveAll(posts);
        }

        log.info("Content seeding completed: {} items created", count);
        return count;
    }

    /**
     * Generates a single content post with metadata.
     */
    private ContentPost generateContentPost(int index, Random random) {
        // Determine content type based on distribution
        ContentType contentType = selectContentType(random);
        int contentLength = generateContentLength(contentType, random);
        String content = generateContent(contentType, contentLength, random);

        // Determine media
        boolean hasMedia = shouldHaveMedia(random);
        MediaType mediaType = hasMedia ? selectMediaType(random) : null;
        String mediaUrls = hasMedia ? generateMediaUrls(mediaType, random) : null;

        // Determine source and channel
        ContentSource source = selectSource(random);
        String channelName = CHANNELS[random.nextInt(CHANNELS.length)];
        String channelId = "channel_" + channelName.toLowerCase().replace(" ", "_");

        // Generate title for longer content
        String title = contentLength > 500 ? generateTitle(random) : null;

        // Published date based on distribution
        LocalDateTime publishedAt = generatePublishedDate(random);

        ContentPost post = ContentPost.builder()
                .externalId("seed_" + index + "_" + UUID.randomUUID().toString().substring(0, 8))
                .source(source)
                .sourceChannelId(channelId)
                .sourceChannelName(channelName)
                .title(title)
                .content(content)
                .contentType(contentType)
                .contentLength(contentLength)
                .hasMedia(hasMedia)
                .mediaType(mediaType)
                .mediaUrls(mediaUrls)
                .linkUrl(random.nextBoolean() ? "https://example.com/article/" + index : null)
                .publishedAt(publishedAt)
                .build();

        // Generate and associate metadata
        ContentMetadata metadata = generateMetadata(post, random);
        post.setMetadata(metadata);

        return post;
    }

    /**
     * Selects content type based on distribution: 30% short, 40% medium, 30% long.
     */
    private ContentType selectContentType(Random random) {
        int roll = random.nextInt(100);
        if (roll < 30) {
            return ContentType.SHORT_POST;
        } else if (roll < 70) {
            return ContentType.MEDIUM_POST;
        } else {
            return ContentType.LONG_ARTICLE;
        }
    }

    /**
     * Generates content length based on type.
     */
    private int generateContentLength(ContentType type, Random random) {
        return switch (type) {
            case SHORT_POST -> 200 + random.nextInt(301); // 200-500
            case MEDIUM_POST -> 500 + random.nextInt(1001); // 500-1500
            case LONG_ARTICLE -> 2000 + random.nextInt(2001); // 2000-4000
            case VIDEO_POST -> 100 + random.nextInt(201); // 100-300
            case IMAGE_POST -> 50 + random.nextInt(151); // 50-200
            case MIXED_MEDIA -> 300 + random.nextInt(501); // 300-800
        };
    }

    /**
     * Generates content text of specified length.
     */
    private String generateContent(ContentType type, int targetLength, Random random) {
        StringBuilder sb = new StringBuilder();

        String[] topics = switch (random.nextInt(3)) {
            case 0 -> TECH_TOPICS;
            case 1 -> SCIENCE_TOPICS;
            default -> BUSINESS_TOPICS;
        };

        String topic = topics[random.nextInt(topics.length)];

        // Opening
        String[] openings = {
                "Breaking news about " + topic + ". ",
                "Latest developments in " + topic + " show promising results. ",
                "Experts are discussing the future of " + topic + ". ",
                "A new study reveals insights about " + topic + ". ",
                "Industry leaders weigh in on " + topic + ". "
        };
        sb.append(openings[random.nextInt(openings.length)]);

        // Middle content - fill to target length
        String[] fillers = {
                "This represents a significant advancement in the field. ",
                "Researchers have been working on this for years. ",
                "The implications for industry are substantial. ",
                "Market analysts predict strong growth in this sector. ",
                "Early adopters are seeing positive results. ",
                "The technology continues to evolve rapidly. ",
                "Experts recommend careful consideration of these developments. ",
                "Consumer interest remains high despite challenges. ",
                "Investment in this area has increased significantly. ",
                "Collaboration between teams has accelerated progress. ",
                "New partnerships are forming to advance these goals. ",
                "Regulatory frameworks are adapting to accommodate innovation. ",
                "Training and education programs are expanding. ",
                "Infrastructure improvements support continued growth. ",
                "Quality metrics show consistent improvement over time. "
        };

        while (sb.length() < targetLength - 100) {
            sb.append(fillers[random.nextInt(fillers.length)]);
        }

        // Closing
        String[] closings = {
                "Stay tuned for more updates on this story.",
                "We'll continue to monitor these developments.",
                "More details are expected in the coming weeks.",
                "The full report will be available soon.",
                "Follow us for the latest news and analysis."
        };
        sb.append(closings[random.nextInt(closings.length)]);

        // Trim to target length
        if (sb.length() > targetLength) {
            return sb.substring(0, targetLength);
        }

        return sb.toString();
    }

    /**
     * Determines if content should have media: 60% with media.
     */
    private boolean shouldHaveMedia(Random random) {
        return random.nextInt(100) >= 40;
    }

    /**
     * Selects media type based on distribution.
     */
    private MediaType selectMediaType(Random random) {
        int roll = random.nextInt(100);
        if (roll < 50) { // 50% image
            return MediaType.IMAGE;
        } else if (roll < 83) { // 33% video
            return MediaType.VIDEO;
        } else { // 17% gallery
            return MediaType.GALLERY;
        }
    }

    /**
     * Generates media URLs as JSON array.
     */
    private String generateMediaUrls(MediaType type, Random random) {
        List<String> urls = new ArrayList<>();
        int count = switch (type) {
            case IMAGE -> 1;
            case GALLERY -> 2 + random.nextInt(3); // 2-4 images
            case VIDEO -> 1;
            case AUDIO -> 1;
        };

        String extension = switch (type) {
            case IMAGE, GALLERY -> ".jpg";
            case VIDEO -> ".mp4";
            case AUDIO -> ".mp3";
        };

        for (int i = 0; i < count; i++) {
            urls.add("https://example.com/media/" + UUID.randomUUID().toString().substring(0, 8) + extension);
        }

        return contentMapper.toJson(urls);
    }

    /**
     * Selects content source randomly.
     */
    private ContentSource selectSource(Random random) {
        ContentSource[] sources = ContentSource.values();
        return sources[random.nextInt(sources.length)];
    }

    /**
     * Generates a title for longer content.
     */
    private String generateTitle(Random random) {
        String[] adjectives = {"New", "Latest", "Breaking", "Exclusive", "In-Depth", "Complete"};
        String[] subjects = {"Analysis", "Report", "Guide", "Review", "Update", "Overview"};
        String[] topics = {"Technology Trends", "Market Insights", "Research Findings",
                "Industry News", "Innovation", "Development"};

        return adjectives[random.nextInt(adjectives.length)] + " " +
               subjects[random.nextInt(subjects.length)] + ": " +
               topics[random.nextInt(topics.length)];
    }

    /**
     * Generates published date based on distribution.
     */
    private LocalDateTime generatePublishedDate(Random random) {
        LocalDateTime now = LocalDateTime.now();
        int roll = random.nextInt(100);

        if (roll < 40) { // 40% today
            return now.minusHours(random.nextInt(24));
        } else if (roll < 70) { // 30% yesterday
            return now.minusDays(1).minusHours(random.nextInt(24));
        } else if (roll < 90) { // 20% this week
            return now.minusDays(2 + random.nextInt(5));
        } else { // 10% older
            return now.minusDays(7 + random.nextInt(23));
        }
    }

    /**
     * Generates metadata for a content post.
     */
    private ContentMetadata generateMetadata(ContentPost post, Random random) {
        // Categories - 1-3 with scores
        Map<String, Double> categories = generateCategories(random);

        // Entities
        Map<String, List<String>> entities = generateEntities(random);

        // Keywords
        List<String> keywords = generateKeywords(random);

        // Sentiment based on distribution
        Sentiment sentiment = selectSentiment(random);
        float sentimentScore = generateSentimentScore(sentiment, random);

        // Reading time
        int readingTime = Math.max(1, post.getContentLength() / 200);

        // Complexity based on content length
        float complexity = switch (post.getContentType()) {
            case LONG_ARTICLE -> 0.6f + random.nextFloat() * 0.4f;
            case MEDIUM_POST -> 0.4f + random.nextFloat() * 0.3f;
            default -> 0.2f + random.nextFloat() * 0.3f;
        };

        return ContentMetadata.builder()
                .contentPost(post)
                .categories(contentMapper.toJson(categories))
                .entities(contentMapper.toJson(entities))
                .keywords(contentMapper.toJson(keywords))
                .sentiment(sentiment)
                .sentimentScore(sentimentScore)
                .language("en")
                .readingTimeMinutes(readingTime)
                .complexityScore(complexity)
                .build();
    }

    /**
     * Generates 1-3 categories with scores.
     */
    private Map<String, Double> generateCategories(Random random) {
        Map<String, Double> categories = new LinkedHashMap<>();
        int count = 1 + random.nextInt(3); // 1-3 categories

        List<String> available = new ArrayList<>(List.of(CATEGORIES));
        Collections.shuffle(available, random);

        for (int i = 0; i < count && i < available.size(); i++) {
            double score = 0.5 + random.nextDouble() * 0.5; // 0.5-1.0
            if (i > 0) {
                score *= 0.7; // Secondary categories have lower scores
            }
            categories.put(available.get(i), Math.round(score * 100) / 100.0);
        }

        return categories;
    }

    /**
     * Generates entities (people, companies, locations).
     */
    private Map<String, List<String>> generateEntities(Random random) {
        Map<String, List<String>> entities = new LinkedHashMap<>();

        // 50% chance of person mention
        if (random.nextBoolean()) {
            int count = 1 + random.nextInt(2);
            List<String> persons = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                persons.add(PERSON_NAMES[random.nextInt(PERSON_NAMES.length)]);
            }
            entities.put("person", persons);
        }

        // 60% chance of company mention
        if (random.nextInt(100) < 60) {
            int count = 1 + random.nextInt(3);
            List<String> companies = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                companies.add(COMPANY_NAMES[random.nextInt(COMPANY_NAMES.length)]);
            }
            entities.put("company", companies);
        }

        // 30% chance of location mention
        if (random.nextInt(100) < 30) {
            entities.put("location", List.of(LOCATIONS[random.nextInt(LOCATIONS.length)]));
        }

        return entities;
    }

    /**
     * Generates 3-6 keywords.
     */
    private List<String> generateKeywords(Random random) {
        String[] allKeywords = {
                "technology", "innovation", "research", "development", "analysis",
                "market", "industry", "growth", "investment", "strategy",
                "future", "trends", "digital", "data", "AI", "cloud",
                "mobile", "security", "platform", "solution"
        };

        int count = 3 + random.nextInt(4); // 3-6 keywords
        List<String> keywords = new ArrayList<>();
        List<String> available = new ArrayList<>(List.of(allKeywords));
        Collections.shuffle(available, random);

        for (int i = 0; i < count && i < available.size(); i++) {
            keywords.add(available.get(i));
        }

        return keywords;
    }

    /**
     * Selects sentiment based on distribution: 60% positive, 30% neutral, 10% negative.
     */
    private Sentiment selectSentiment(Random random) {
        int roll = random.nextInt(100);
        if (roll < 60) {
            return Sentiment.POSITIVE;
        } else if (roll < 90) {
            return Sentiment.NEUTRAL;
        } else {
            return Sentiment.NEGATIVE;
        }
    }

    /**
     * Generates sentiment score based on sentiment type.
     */
    private float generateSentimentScore(Sentiment sentiment, Random random) {
        return switch (sentiment) {
            case POSITIVE -> 0.3f + random.nextFloat() * 0.6f; // 0.3 to 0.9
            case NEUTRAL -> -0.2f + random.nextFloat() * 0.4f; // -0.2 to 0.2
            case NEGATIVE -> -0.9f + random.nextFloat() * 0.6f; // -0.9 to -0.3
        };
    }
}
