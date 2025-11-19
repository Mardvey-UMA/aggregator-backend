-- Content Service Schema
-- V2__content_schema.sql

-- ===========================================
-- Content Posts Table
-- ===========================================
CREATE TABLE content_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) UNIQUE,
    source VARCHAR(50) NOT NULL,
    source_channel_id VARCHAR(255),
    source_channel_name VARCHAR(255),
    title TEXT,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content_length INTEGER NOT NULL,
    has_media BOOLEAN NOT NULL DEFAULT FALSE,
    media_type VARCHAR(50),
    media_urls JSONB,
    link_url VARCHAR(1024),
    published_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE content_posts IS 'Stores aggregated content posts from various sources';
COMMENT ON COLUMN content_posts.external_id IS 'Unique identifier from the content source';
COMMENT ON COLUMN content_posts.source IS 'Content source (TELEGRAM, VK, RSS, TWITTER, MANUAL)';
COMMENT ON COLUMN content_posts.content_type IS 'Type of content (SHORT_POST, MEDIUM_POST, LONG_ARTICLE, VIDEO_POST, IMAGE_POST, MIXED_MEDIA)';
COMMENT ON COLUMN content_posts.media_urls IS 'JSON array of media URLs';

-- Indexes for content_posts
CREATE INDEX idx_content_posts_external_id ON content_posts(external_id);
CREATE INDEX idx_content_posts_source ON content_posts(source);
CREATE INDEX idx_content_posts_published_at ON content_posts(published_at DESC);
CREATE INDEX idx_content_posts_content_type ON content_posts(content_type);
CREATE INDEX idx_content_posts_source_channel ON content_posts(source_channel_id);
CREATE INDEX idx_content_posts_created_at ON content_posts(created_at DESC);

-- ===========================================
-- Content Metadata Table
-- ===========================================
CREATE TABLE content_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_post_id UUID NOT NULL UNIQUE REFERENCES content_posts(id) ON DELETE CASCADE,
    categories JSONB,
    entities JSONB,
    keywords JSONB,
    sentiment VARCHAR(20),
    sentiment_score FLOAT,
    language VARCHAR(10),
    reading_time_minutes INTEGER,
    complexity_score FLOAT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE content_metadata IS 'Preprocessed metadata for content posts including NLP features';
COMMENT ON COLUMN content_metadata.categories IS 'Category scores as JSON object {"tech": 0.9, "business": 0.3}';
COMMENT ON COLUMN content_metadata.entities IS 'Named entities as JSON {"person": ["Elon Musk"], "company": ["Tesla"]}';
COMMENT ON COLUMN content_metadata.keywords IS 'Extracted keywords as JSON array';
COMMENT ON COLUMN content_metadata.sentiment IS 'Sentiment classification (POSITIVE, NEGATIVE, NEUTRAL)';
COMMENT ON COLUMN content_metadata.complexity_score IS 'Readability score from 0 to 1';

-- Indexes for content_metadata
CREATE INDEX idx_metadata_content_post ON content_metadata(content_post_id);
CREATE INDEX idx_metadata_sentiment ON content_metadata(sentiment);
CREATE INDEX idx_metadata_language ON content_metadata(language);
CREATE INDEX idx_metadata_categories ON content_metadata USING GIN(categories);
CREATE INDEX idx_metadata_keywords ON content_metadata USING GIN(keywords);

-- ===========================================
-- Content Bookmarks Table
-- ===========================================
CREATE TABLE content_bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_post_id UUID NOT NULL REFERENCES content_posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_bookmarks_user_content UNIQUE (user_id, content_post_id)
);

COMMENT ON TABLE content_bookmarks IS 'User bookmarks for content posts';
COMMENT ON COLUMN content_bookmarks.user_id IS 'User ID from auth service';

-- Indexes for content_bookmarks
CREATE INDEX idx_bookmarks_user_id ON content_bookmarks(user_id);
CREATE INDEX idx_bookmarks_content_post_id ON content_bookmarks(content_post_id);
CREATE INDEX idx_bookmarks_created_at ON content_bookmarks(created_at DESC);

-- ===========================================
-- Content Views Table
-- ===========================================
CREATE TABLE content_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_post_id UUID NOT NULL REFERENCES content_posts(id) ON DELETE CASCADE,
    user_id UUID,
    session_id UUID NOT NULL,
    viewed_at TIMESTAMP NOT NULL
);

COMMENT ON TABLE content_views IS 'View events for content posts used for trending calculations';
COMMENT ON COLUMN content_views.user_id IS 'User ID from auth service (nullable for anonymous views)';
COMMENT ON COLUMN content_views.session_id IS 'Session identifier for deduplication';

-- Indexes for content_views
CREATE INDEX idx_views_content_post_id ON content_views(content_post_id);
CREATE INDEX idx_views_viewed_at ON content_views(viewed_at DESC);
CREATE INDEX idx_views_user_id ON content_views(user_id);
CREATE INDEX idx_views_content_viewed ON content_views(content_post_id, viewed_at DESC);
CREATE INDEX idx_views_session ON content_views(session_id);

-- ===========================================
-- Triggers for updated_at
-- ===========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_content_posts_updated_at
    BEFORE UPDATE ON content_posts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_content_metadata_updated_at
    BEFORE UPDATE ON content_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ===========================================
-- Sample Data (50 diverse content posts)
-- ===========================================

-- Technology Posts
INSERT INTO content_posts (id, external_id, source, source_channel_id, source_channel_name, title, content, content_type, content_length, has_media, media_type, media_urls, link_url, published_at, created_at, updated_at)
VALUES
('11111111-1111-1111-1111-111111111101', 'tg-tech-001', 'TELEGRAM', 'tech_news', 'Tech News Daily', 'Breaking: New AI Model Released', 'OpenAI has released a new version of their flagship model with improved reasoning capabilities. The model shows 40% better performance on complex tasks and significantly reduced hallucinations.', 'SHORT_POST', 242, false, NULL, NULL, 'https://openai.com/blog', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'),

('11111111-1111-1111-1111-111111111102', 'tg-tech-002', 'TELEGRAM', 'programming_tips', 'Programming Tips', 'Understanding Virtual Threads in Java 21', 'Virtual threads are lightweight threads that dramatically reduce the effort of writing, maintaining, and observing high-throughput concurrent applications. Unlike platform threads, virtual threads are not wrappers around OS threads. Instead, they are lightweight implementations of threads provided by the JDK.

Key benefits include:
- Massive scalability (millions of concurrent threads)
- Simplified concurrency model
- Better resource utilization
- Familiar Thread API

Virtual threads are ideal for I/O-bound workloads where tasks spend most of their time waiting for external resources like databases, file systems, or network calls.

Example usage in Spring Boot 3.2+:
```java
spring.threads.virtual.enabled=true
```

This single configuration enables virtual threads for all request handling, making your application ready for high concurrency without complex async programming.', 'LONG_ARTICLE', 843, false, NULL, NULL, 'https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),

('11111111-1111-1111-1111-111111111103', 'vk-tech-001', 'VK', 'gadgets_review', 'Gadgets Review', NULL, 'New smartphone comparison: iPhone 15 Pro vs Samsung S24 Ultra. Both devices excel in different areas. Camera comparison in the gallery below.', 'IMAGE_POST', 147, true, 'GALLERY', '["https://example.com/img1.jpg", "https://example.com/img2.jpg", "https://example.com/img3.jpg"]', NULL, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),

('11111111-1111-1111-1111-111111111104', 'rss-tech-001', 'RSS', 'hackernews', 'Hacker News', 'The State of WebAssembly in 2024', 'WebAssembly continues to evolve beyond its browser origins. In 2024, we see increasing adoption in:

1. Server-side computing with WASI
2. Edge computing platforms
3. Plugin systems for desktop applications
4. Blockchain smart contracts

The component model proposal promises to make WASM modules more composable, while the garbage collection proposal opens doors for languages like Java and C# to target WASM efficiently.

Companies like Cloudflare, Fastly, and Vercel are betting big on WASM for edge computing, citing near-native performance and sandboxed security as key advantages.

For developers, this means WASM skills are becoming increasingly valuable across the stack, not just for frontend optimization.', 'MEDIUM_POST', 705, false, NULL, NULL, 'https://news.ycombinator.com', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours'),

('11111111-1111-1111-1111-111111111105', 'tw-tech-001', 'TWITTER', 'elonmusk', 'Elon Musk', NULL, 'Starlink now available in 60+ countries. Working on reducing latency further.', 'SHORT_POST', 78, false, NULL, NULL, NULL, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours'),

-- Science Posts
('11111111-1111-1111-1111-111111111106', 'tg-sci-001', 'TELEGRAM', 'science_daily', 'Science Daily', 'Mars Rover Discovers New Evidence of Ancient Water', 'NASA''s Perseverance rover has discovered mineral deposits that strongly suggest Mars once had flowing water on its surface for millions of years. The sedimentary layers show clear signs of ancient riverbeds.', 'SHORT_POST', 252, true, 'IMAGE', '["https://example.com/mars-rover.jpg"]', 'https://nasa.gov/mars', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),

('11111111-1111-1111-1111-111111111107', 'rss-sci-001', 'RSS', 'nature', 'Nature Journal', 'Breakthrough in Quantum Computing: 1000 Qubit System Achieved', 'In a landmark achievement, researchers have successfully demonstrated a quantum computer with over 1000 stable qubits. This represents a major step toward quantum supremacy for practical applications.

The system uses a novel error correction technique that maintains coherence for significantly longer periods than previous designs. This breakthrough could accelerate progress in drug discovery, materials science, and cryptography.

Dr. Sarah Chen, lead researcher, stated: "We''ve crossed a critical threshold. Our error rates are now low enough for meaningful quantum algorithms to execute reliably."

The implications for fields like climate modeling and financial optimization are substantial, with experts predicting practical quantum advantage within 5-7 years.', 'LONG_ARTICLE', 775, false, NULL, NULL, 'https://nature.com/articles/quantum', NOW() - INTERVAL '7 hours', NOW() - INTERVAL '7 hours', NOW() - INTERVAL '7 hours'),

('11111111-1111-1111-1111-111111111108', 'vk-sci-001', 'VK', 'space_exploration', 'Space Exploration', NULL, 'Watch the SpaceX Starship test flight highlights. The vehicle successfully reached orbit before the planned splashdown.', 'VIDEO_POST', 118, true, 'VIDEO', '["https://example.com/starship-launch.mp4"]', 'https://spacex.com', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours'),

-- Business Posts
('11111111-1111-1111-1111-111111111109', 'tg-biz-001', 'TELEGRAM', 'startup_news', 'Startup News', 'YC Demo Day Highlights: Top 10 Startups to Watch', 'The latest Y Combinator Demo Day showcased impressive innovation across AI, climate tech, and healthcare. Here are the standout companies:

1. **MediSync AI** - AI-powered diagnostic assistance
2. **CarbonTrack** - Real-time carbon footprint monitoring
3. **QuantumShield** - Post-quantum cryptography solutions
4. **AgriDrone** - Autonomous agricultural drones
5. **NeuroLink Health** - Brain-computer interface for medical applications

Total funding sought: $150M across all presenting startups. Investor interest was particularly high in AI infrastructure and climate solutions.', 'MEDIUM_POST', 589, false, NULL, NULL, 'https://ycombinator.com', NOW() - INTERVAL '9 hours', NOW() - INTERVAL '9 hours', NOW() - INTERVAL '9 hours'),

('11111111-1111-1111-1111-111111111110', 'rss-biz-001', 'RSS', 'bloomberg', 'Bloomberg', 'Tech Giants Report Strong Q4 Earnings', 'Apple, Microsoft, and Google parent Alphabet all exceeded analyst expectations in their Q4 earnings reports. Cloud services and AI investments were key growth drivers.', 'SHORT_POST', 195, false, NULL, NULL, 'https://bloomberg.com', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours'),

-- Entertainment Posts
('11111111-1111-1111-1111-111111111111', 'tg-ent-001', 'TELEGRAM', 'movie_reviews', 'Movie Reviews', 'Review: Dune Part Two - A Cinematic Masterpiece', 'Denis Villeneuve delivers an epic conclusion to Frank Herbert''s story. The film expands on the first installment with breathtaking visuals and powerful performances. Timothée Chalamet and Zendaya shine in their roles.

Rating: 9/10

The sandworm riding sequences alone are worth the IMAX ticket. Hans Zimmer''s score adds another layer of intensity to the desert planet atmosphere.', 'MEDIUM_POST', 412, true, 'IMAGE', '["https://example.com/dune2-poster.jpg"]', 'https://imdb.com/dune2', NOW() - INTERVAL '11 hours', NOW() - INTERVAL '11 hours', NOW() - INTERVAL '11 hours'),

('11111111-1111-1111-1111-111111111112', 'vk-ent-001', 'VK', 'gaming_news', 'Gaming News', 'GTA 6 Trailer Breaks YouTube Records', 'The first trailer for Grand Theft Auto 6 has broken all YouTube viewing records, accumulating over 90 million views in 24 hours. Set in Vice City, the game promises the most detailed open world ever created.', 'SHORT_POST', 236, true, 'VIDEO', '["https://example.com/gta6-trailer.mp4"]', 'https://rockstargames.com', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours'),

('11111111-1111-1111-1111-111111111113', 'tw-ent-001', 'TWITTER', 'spotify', 'Spotify', NULL, 'Taylor Swift''s new album breaks first-day streaming records with 300M+ streams globally!', 'SHORT_POST', 89, false, NULL, NULL, 'https://spotify.com', NOW() - INTERVAL '13 hours', NOW() - INTERVAL '13 hours', NOW() - INTERVAL '13 hours'),

-- Sports Posts
('11111111-1111-1111-1111-111111111114', 'tg-sport-001', 'TELEGRAM', 'football_news', 'Football News', 'Champions League Quarterfinal Draw Results', 'The Champions League quarterfinal draw has produced exciting matchups:
- Real Madrid vs Manchester City
- Bayern Munich vs Arsenal
- Barcelona vs PSG
- Inter Milan vs Atletico Madrid

First legs scheduled for April 9-10.', 'SHORT_POST', 267, false, NULL, NULL, 'https://uefa.com', NOW() - INTERVAL '14 hours', NOW() - INTERVAL '14 hours', NOW() - INTERVAL '14 hours'),

('11111111-1111-1111-1111-111111111115', 'vk-sport-001', 'VK', 'basketball_highlights', 'Basketball Highlights', NULL, 'LeBron James becomes the first NBA player to reach 40,000 career points. An incredible achievement spanning 21 seasons.', 'VIDEO_POST', 132, true, 'VIDEO', '["https://example.com/lebron-40k.mp4"]', 'https://nba.com', NOW() - INTERVAL '15 hours', NOW() - INTERVAL '15 hours', NOW() - INTERVAL '15 hours'),

('11111111-1111-1111-1111-111111111116', 'rss-sport-001', 'RSS', 'espn', 'ESPN', 'Winter Olympics 2026: New Events Announced', 'The IOC has confirmed several new events for the 2026 Milan-Cortina Winter Olympics, including ski mountaineering. The games will feature a record number of mixed-gender events.', 'SHORT_POST', 208, false, NULL, NULL, 'https://espn.com', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '16 hours'),

-- Lifestyle Posts
('11111111-1111-1111-1111-111111111117', 'tg-life-001', 'TELEGRAM', 'travel_tips', 'Travel Tips', '10 Hidden Gems in Southeast Asia', 'Escape the tourist crowds with these lesser-known destinations:

1. Kampot, Cambodia - French colonial charm
2. Pai, Thailand - Mountain retreat
3. Luang Prabang, Laos - UNESCO heritage
4. Ninh Binh, Vietnam - Karst landscapes
5. Siquijor, Philippines - Mystical island

Each destination offers authentic experiences at a fraction of the cost of popular spots like Bali or Phuket.', 'MEDIUM_POST', 418, true, 'GALLERY', '["https://example.com/travel1.jpg", "https://example.com/travel2.jpg"]', NULL, NOW() - INTERVAL '17 hours', NOW() - INTERVAL '17 hours', NOW() - INTERVAL '17 hours'),

('11111111-1111-1111-1111-111111111118', 'vk-life-001', 'VK', 'food_recipes', 'Food Recipes', 'Simple Japanese Ramen Recipe', 'Make restaurant-quality ramen at home with this simple recipe. The secret is in the tare (seasoning) and proper noodle cooking technique.', 'IMAGE_POST', 157, true, 'GALLERY', '["https://example.com/ramen1.jpg", "https://example.com/ramen2.jpg", "https://example.com/ramen3.jpg"]', NULL, NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours'),

('11111111-1111-1111-1111-111111111119', 'tw-life-001', 'TWITTER', 'fashion_week', 'Fashion Week', NULL, 'Milan Fashion Week 2024 wrapped up with bold colors dominating the runway. Oversized silhouettes and sustainable materials were key trends.', 'SHORT_POST', 147, false, NULL, NULL, 'https://vogue.com', NOW() - INTERVAL '19 hours', NOW() - INTERVAL '19 hours', NOW() - INTERVAL '19 hours'),

-- Education Posts
('11111111-1111-1111-1111-111111111120', 'tg-edu-001', 'TELEGRAM', 'learn_programming', 'Learn Programming', 'Complete Guide to Microservices Architecture', 'Microservices architecture has become the standard for building scalable applications. This guide covers:

**Core Concepts:**
- Service decomposition strategies
- Inter-service communication (sync vs async)
- Data management patterns
- API gateway design

**Best Practices:**
- One database per service
- Implement circuit breakers
- Use event-driven communication
- Centralized logging and monitoring

**Common Pitfalls:**
- Distributed monolith anti-pattern
- Ignoring network latency
- Overcomplicating simple systems

Start with a modular monolith and extract services only when needed. Premature decomposition leads to unnecessary complexity.

Tools to consider: Kubernetes, Istio, Kafka, Redis, PostgreSQL', 'LONG_ARTICLE', 762, false, NULL, NULL, 'https://microservices.io', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'),

('11111111-1111-1111-1111-111111111121', 'rss-edu-001', 'RSS', 'coursera', 'Coursera', 'Free AI Courses from Stanford and MIT', 'Top universities are offering free AI and machine learning courses. Stanford''s CS229 and MIT''s 6.S191 are highly recommended for beginners with programming experience.', 'SHORT_POST', 203, false, NULL, NULL, 'https://coursera.org', NOW() - INTERVAL '21 hours', NOW() - INTERVAL '21 hours', NOW() - INTERVAL '21 hours'),

('11111111-1111-1111-1111-111111111122', 'vk-edu-001', 'VK', 'book_reviews', 'Book Reviews', 'Must-Read Books for Software Engineers in 2024', 'These books will level up your engineering skills:

1. "Designing Data-Intensive Applications" - Martin Kleppmann
2. "System Design Interview" - Alex Xu
3. "Clean Code" - Robert Martin
4. "The Pragmatic Programmer" - Dave Thomas

Each offers practical insights applicable to daily work.', 'MEDIUM_POST', 343, true, 'IMAGE', '["https://example.com/books.jpg"]', NULL, NOW() - INTERVAL '22 hours', NOW() - INTERVAL '22 hours', NOW() - INTERVAL '22 hours'),

-- News Posts
('11111111-1111-1111-1111-111111111123', 'rss-news-001', 'RSS', 'reuters', 'Reuters', 'Global Climate Summit Reaches Historic Agreement', 'World leaders have agreed to phase out fossil fuel subsidies by 2030 and increase renewable energy investments. The agreement includes provisions for climate financing to developing nations.', 'SHORT_POST', 225, false, NULL, NULL, 'https://reuters.com', NOW() - INTERVAL '23 hours', NOW() - INTERVAL '23 hours', NOW() - INTERVAL '23 hours'),

('11111111-1111-1111-1111-111111111124', 'tg-news-001', 'TELEGRAM', 'world_news', 'World News', 'EU Passes Comprehensive AI Regulation', 'The European Union has passed the AI Act, the world''s first comprehensive AI regulation framework. Key provisions include:

- Prohibited AI applications (social scoring, biometric surveillance)
- High-risk AI requirements (medical devices, hiring systems)
- Transparency obligations for general-purpose AI
- Fines up to 7% of global revenue

Tech companies have 2 years to comply with most provisions.', 'MEDIUM_POST', 437, false, NULL, NULL, 'https://ec.europa.eu', NOW() - INTERVAL '24 hours', NOW() - INTERVAL '24 hours', NOW() - INTERVAL '24 hours'),

-- Additional diverse content to reach 50 posts
('11111111-1111-1111-1111-111111111125', 'manual-001', 'MANUAL', NULL, 'Content Team', 'Welcome to Content Aggregation Platform', 'Welcome to our platform! Here you''ll find curated content from across the web on technology, science, business, and more. Our recommendation system learns your preferences to deliver personalized feeds.', 'SHORT_POST', 235, false, NULL, NULL, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

('11111111-1111-1111-1111-111111111126', 'tg-tech-003', 'TELEGRAM', 'dev_ops', 'DevOps Daily', 'Kubernetes 1.30 Release Notes', 'The latest Kubernetes release brings several improvements including better pod scheduling, reduced memory usage, and enhanced security features. The sidecar container feature is now stable.', 'SHORT_POST', 219, false, NULL, NULL, 'https://kubernetes.io', NOW() - INTERVAL '1 day 1 hour', NOW() - INTERVAL '1 day 1 hour', NOW() - INTERVAL '1 day 1 hour'),

('11111111-1111-1111-1111-111111111127', 'vk-tech-002', 'VK', 'ai_art', 'AI Art', NULL, 'Midjourney v6 comparison gallery: See the evolution of AI image generation quality across versions.', 'IMAGE_POST', 102, true, 'GALLERY', '["https://example.com/ai-art1.jpg", "https://example.com/ai-art2.jpg", "https://example.com/ai-art3.jpg", "https://example.com/ai-art4.jpg"]', NULL, NOW() - INTERVAL '1 day 2 hours', NOW() - INTERVAL '1 day 2 hours', NOW() - INTERVAL '1 day 2 hours'),

('11111111-1111-1111-1111-111111111128', 'rss-tech-002', 'RSS', 'verge', 'The Verge', 'Apple Vision Pro Review: The Future of Computing?', 'After two weeks with Apple''s spatial computer, here''s our comprehensive review. The hardware is impressive, but is the $3,500 price justified?

**Pros:**
- Incredible display quality
- Seamless ecosystem integration
- Eye tracking accuracy

**Cons:**
- Limited app ecosystem
- Battery life constraints
- Weight and comfort issues

For early adopters and developers, it''s a glimpse into the future. For everyone else, wait for version 2.', 'LONG_ARTICLE', 523, true, 'IMAGE', '["https://example.com/vision-pro.jpg"]', 'https://theverge.com', NOW() - INTERVAL '1 day 3 hours', NOW() - INTERVAL '1 day 3 hours', NOW() - INTERVAL '1 day 3 hours'),

('11111111-1111-1111-1111-111111111129', 'tw-tech-002', 'TWITTER', 'nvidia', 'NVIDIA', NULL, 'Introducing Blackwell: Our next-generation GPU architecture delivering unprecedented AI performance.', 'SHORT_POST', 96, false, NULL, NULL, 'https://nvidia.com', NOW() - INTERVAL '1 day 4 hours', NOW() - INTERVAL '1 day 4 hours', NOW() - INTERVAL '1 day 4 hours'),

('11111111-1111-1111-1111-111111111130', 'tg-sci-002', 'TELEGRAM', 'physics_today', 'Physics Today', 'CERN Announces New Particle Discovery', 'The Large Hadron Collider has detected a new exotic particle that doesn''t fit existing models. This tetraquark state could provide insights into the strong nuclear force.', 'SHORT_POST', 192, false, NULL, NULL, 'https://cern.ch', NOW() - INTERVAL '1 day 5 hours', NOW() - INTERVAL '1 day 5 hours', NOW() - INTERVAL '1 day 5 hours'),

('11111111-1111-1111-1111-111111111131', 'vk-sci-002', 'VK', 'biology_news', 'Biology News', NULL, 'CRISPR gene editing successfully treats genetic blindness in clinical trial. Watch the patient''s reaction to seeing colors for the first time.', 'VIDEO_POST', 140, true, 'VIDEO', '["https://example.com/crispr-trial.mp4"]', NULL, NOW() - INTERVAL '1 day 6 hours', NOW() - INTERVAL '1 day 6 hours', NOW() - INTERVAL '1 day 6 hours'),

('11111111-1111-1111-1111-111111111132', 'rss-biz-002', 'RSS', 'wsj', 'Wall Street Journal', 'Federal Reserve Signals Rate Cuts in 2024', 'The Fed has indicated multiple interest rate cuts this year as inflation continues to cool. Markets rallied on the news with the S&P 500 reaching new all-time highs.', 'SHORT_POST', 205, false, NULL, NULL, 'https://wsj.com', NOW() - INTERVAL '1 day 7 hours', NOW() - INTERVAL '1 day 7 hours', NOW() - INTERVAL '1 day 7 hours'),

('11111111-1111-1111-1111-111111111133', 'tg-biz-002', 'TELEGRAM', 'crypto_news', 'Crypto News', 'Bitcoin ETF Sees Record Inflows', 'Spot Bitcoin ETFs have attracted over $5 billion in net inflows since launch. BlackRock''s IBIT leads the pack with institutional demand driving adoption.

Price analysis suggests strong support at current levels with potential upside as halving approaches.', 'MEDIUM_POST', 298, false, NULL, NULL, 'https://coindesk.com', NOW() - INTERVAL '1 day 8 hours', NOW() - INTERVAL '1 day 8 hours', NOW() - INTERVAL '1 day 8 hours'),

('11111111-1111-1111-1111-111111111134', 'vk-ent-002', 'VK', 'anime_news', 'Anime News', 'One Piece Final Saga Announcement', 'Eiichiro Oda confirms One Piece is entering its final saga after 25 years. The manga will continue for approximately 3-4 more years to reach its conclusion.', 'SHORT_POST', 175, true, 'IMAGE', '["https://example.com/one-piece.jpg"]', NULL, NOW() - INTERVAL '1 day 9 hours', NOW() - INTERVAL '1 day 9 hours', NOW() - INTERVAL '1 day 9 hours'),

('11111111-1111-1111-1111-111111111135', 'tw-sport-001', 'TWITTER', 'f1', 'Formula 1', NULL, 'Verstappen takes pole position in Bahrain! Leclerc 0.3s behind in P2. Race starts tomorrow at 15:00 GMT.', 'SHORT_POST', 107, false, NULL, NULL, 'https://formula1.com', NOW() - INTERVAL '1 day 10 hours', NOW() - INTERVAL '1 day 10 hours', NOW() - INTERVAL '1 day 10 hours'),

('11111111-1111-1111-1111-111111111136', 'tg-life-002', 'TELEGRAM', 'mindfulness', 'Mindfulness Daily', '5-Minute Morning Meditation Guide', 'Start your day with clarity using this simple meditation practice:

1. Sit comfortably with a straight spine
2. Close your eyes and take three deep breaths
3. Focus on the sensation of breathing
4. Notice thoughts without judgment
5. Return attention to breath when distracted

Consistency matters more than duration. Five minutes daily beats one hour weekly.', 'MEDIUM_POST', 395, false, NULL, NULL, NULL, NOW() - INTERVAL '1 day 11 hours', NOW() - INTERVAL '1 day 11 hours', NOW() - INTERVAL '1 day 11 hours'),

('11111111-1111-1111-1111-111111111137', 'rss-edu-002', 'RSS', 'mit_news', 'MIT News', 'MIT Launches Free Online AI Safety Course', 'MIT''s new course covers AI alignment, robustness, and governance. Open to everyone, it addresses critical challenges in developing safe AI systems.', 'SHORT_POST', 177, false, NULL, NULL, 'https://mit.edu', NOW() - INTERVAL '1 day 12 hours', NOW() - INTERVAL '1 day 12 hours', NOW() - INTERVAL '1 day 12 hours'),

('11111111-1111-1111-1111-111111111138', 'vk-news-001', 'VK', 'local_news', 'Local News', NULL, 'City council approves new public transit expansion plan. Light rail construction begins next spring with completion expected by 2027.', 'SHORT_POST', 145, false, NULL, NULL, NULL, NOW() - INTERVAL '1 day 13 hours', NOW() - INTERVAL '1 day 13 hours', NOW() - INTERVAL '1 day 13 hours'),

('11111111-1111-1111-1111-111111111139', 'tg-tech-004', 'TELEGRAM', 'rust_lang', 'Rust Language', 'Rust 2024 Edition Preview', 'The upcoming Rust 2024 edition brings several quality-of-life improvements including better async syntax and enhanced pattern matching. Migration from 2021 edition will be straightforward.', 'SHORT_POST', 218, false, NULL, NULL, 'https://rust-lang.org', NOW() - INTERVAL '1 day 14 hours', NOW() - INTERVAL '1 day 14 hours', NOW() - INTERVAL '1 day 14 hours'),

('11111111-1111-1111-1111-111111111140', 'rss-sci-002', 'RSS', 'science_mag', 'Science Magazine', 'Antarctic Ice Loss Accelerating Faster Than Models Predicted', 'New satellite data shows Antarctic ice sheets are losing mass 20% faster than previous estimates. Sea level rise projections may need significant upward revision.

The study analyzed 25 years of satellite observations, revealing accelerating trends particularly in West Antarctica.', 'MEDIUM_POST', 350, false, NULL, NULL, 'https://science.org', NOW() - INTERVAL '1 day 15 hours', NOW() - INTERVAL '1 day 15 hours', NOW() - INTERVAL '1 day 15 hours'),

('11111111-1111-1111-1111-111111111141', 'vk-tech-003', 'VK', 'cybersecurity', 'Cybersecurity News', NULL, 'Critical vulnerability discovered in widely-used open source library. Patch immediately if you use libXML2 versions below 2.12.0.', 'SHORT_POST', 137, false, NULL, NULL, 'https://cve.mitre.org', NOW() - INTERVAL '1 day 16 hours', NOW() - INTERVAL '1 day 16 hours', NOW() - INTERVAL '1 day 16 hours'),

('11111111-1111-1111-1111-111111111142', 'tw-biz-001', 'TWITTER', 'sequoia', 'Sequoia Capital', NULL, 'AI will create more jobs than it destroys, but they''ll be different jobs. The key is continuous learning and adaptation.', 'SHORT_POST', 131, false, NULL, NULL, NULL, NOW() - INTERVAL '1 day 17 hours', NOW() - INTERVAL '1 day 17 hours', NOW() - INTERVAL '1 day 17 hours'),

('11111111-1111-1111-1111-111111111143', 'tg-sport-002', 'TELEGRAM', 'tennis_news', 'Tennis News', 'Djokovic Wins Record 25th Grand Slam', 'Novak Djokovic has won his 25th Grand Slam title at the Australian Open, extending his lead as the most decorated male tennis player in history.', 'SHORT_POST', 169, true, 'IMAGE', '["https://example.com/djokovic-ao.jpg"]', 'https://ausopen.com', NOW() - INTERVAL '1 day 18 hours', NOW() - INTERVAL '1 day 18 hours', NOW() - INTERVAL '1 day 18 hours'),

('11111111-1111-1111-1111-111111111144', 'rss-life-001', 'RSS', 'health_news', 'Health News', 'Study: Mediterranean Diet Reduces Dementia Risk by 30%', 'A 20-year longitudinal study confirms the neuroprotective benefits of Mediterranean diet rich in olive oil, fish, and vegetables. Participants showed better cognitive performance and brain volume preservation.', 'SHORT_POST', 257, false, NULL, NULL, 'https://healthline.com', NOW() - INTERVAL '1 day 19 hours', NOW() - INTERVAL '1 day 19 hours', NOW() - INTERVAL '1 day 19 hours'),

('11111111-1111-1111-1111-111111111145', 'vk-ent-003', 'VK', 'music_news', 'Music News', NULL, 'Coachella 2024 lineup announced! Headliners include Bad Bunny, No Doubt reunion, and Lana Del Rey. Tickets on sale Friday.', 'IMAGE_POST', 128, true, 'IMAGE', '["https://example.com/coachella-lineup.jpg"]', 'https://coachella.com', NOW() - INTERVAL '1 day 20 hours', NOW() - INTERVAL '1 day 20 hours', NOW() - INTERVAL '1 day 20 hours'),

('11111111-1111-1111-1111-111111111146', 'tg-edu-002', 'TELEGRAM', 'data_science', 'Data Science', 'Understanding Transformers: A Visual Guide', 'The transformer architecture revolutionized NLP and is now dominating other domains. This visual guide explains:

**Key Components:**
- Self-attention mechanism
- Multi-head attention
- Positional encoding
- Feed-forward layers

**Why It Works:**
Transformers can process entire sequences in parallel (unlike RNNs) and capture long-range dependencies through attention.

The famous "Attention is All You Need" paper showed transformers outperform previous architectures on translation while being more parallelizable.

Modern LLMs like GPT-4 and Claude are transformer-based models scaled to billions of parameters.', 'LONG_ARTICLE', 671, true, 'IMAGE', '["https://example.com/transformer-diagram.png"]', 'https://arxiv.org', NOW() - INTERVAL '1 day 21 hours', NOW() - INTERVAL '1 day 21 hours', NOW() - INTERVAL '1 day 21 hours'),

('11111111-1111-1111-1111-111111111147', 'rss-news-002', 'RSS', 'bbc', 'BBC', 'Electric Vehicle Sales Surpass 20% Global Market Share', 'For the first time, electric vehicles have captured more than 20% of global new car sales. China leads adoption with 35% EV market share, followed by Europe at 25%.', 'SHORT_POST', 211, false, NULL, NULL, 'https://bbc.com', NOW() - INTERVAL '1 day 22 hours', NOW() - INTERVAL '1 day 22 hours', NOW() - INTERVAL '1 day 22 hours'),

('11111111-1111-1111-1111-111111111148', 'tw-sci-001', 'TWITTER', 'nasa', 'NASA', NULL, 'Artemis III crew announced! Four astronauts will return humans to the lunar surface in 2025, including the first woman to walk on the Moon.', 'SHORT_POST', 154, false, NULL, NULL, 'https://nasa.gov/artemis', NOW() - INTERVAL '1 day 23 hours', NOW() - INTERVAL '1 day 23 hours', NOW() - INTERVAL '1 day 23 hours'),

('11111111-1111-1111-1111-111111111149', 'manual-002', 'MANUAL', NULL, 'Content Team', 'Platform Update: New Features Available', 'We''ve added several new features to improve your content discovery experience:

- Enhanced search with filters
- Personalized recommendations
- Reading time estimates
- Bookmark collections

Share your feedback using the feedback form in settings.', 'SHORT_POST', 275, false, NULL, NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

('11111111-1111-1111-1111-111111111150', 'tg-tech-005', 'TELEGRAM', 'web_dev', 'Web Development', 'React Server Components: Complete Guide', 'React Server Components (RSC) represent a paradigm shift in React development. They allow components to execute on the server and send rendered HTML to the client.

**Benefits:**
- Zero bundle size for server components
- Direct database access
- Automatic code splitting
- Better SEO

**When to Use:**
- Data fetching components
- Heavy computation
- Static content

**When NOT to Use:**
- Components with interactivity
- Hooks usage (useState, useEffect)
- Browser APIs

Next.js 14 app router uses RSC by default. Components in the app directory are server components unless you add ''use client'' directive.

This hybrid model lets you choose the right execution environment for each component, optimizing both performance and developer experience.', 'LONG_ARTICLE', 853, false, NULL, NULL, 'https://react.dev/blog/2023/03/22/react-labs-what-we-have-been-working-on-march-2023', NOW() - INTERVAL '2 days 1 hour', NOW() - INTERVAL '2 days 1 hour', NOW() - INTERVAL '2 days 1 hour');

-- ===========================================
-- Sample Metadata for Content Posts
-- ===========================================

INSERT INTO content_metadata (content_post_id, categories, entities, keywords, sentiment, sentiment_score, language, reading_time_minutes, complexity_score)
SELECT
    id,
    CASE
        WHEN source_channel_name LIKE '%Tech%' OR source_channel_name LIKE '%Programming%' OR source_channel_name LIKE '%DevOps%' OR source_channel_name LIKE '%Web%' THEN '{"technology": 0.9, "programming": 0.7}'
        WHEN source_channel_name LIKE '%Science%' OR source_channel_name LIKE '%Physics%' OR source_channel_name LIKE '%Biology%' THEN '{"science": 0.9, "education": 0.5}'
        WHEN source_channel_name LIKE '%Business%' OR source_channel_name LIKE '%Startup%' OR source_channel_name LIKE '%Crypto%' THEN '{"business": 0.9, "finance": 0.6}'
        WHEN source_channel_name LIKE '%Movie%' OR source_channel_name LIKE '%Gaming%' OR source_channel_name LIKE '%Music%' OR source_channel_name LIKE '%Anime%' THEN '{"entertainment": 0.9, "lifestyle": 0.4}'
        WHEN source_channel_name LIKE '%Sport%' OR source_channel_name LIKE '%Football%' OR source_channel_name LIKE '%Basketball%' OR source_channel_name LIKE '%Tennis%' THEN '{"sports": 0.9, "news": 0.5}'
        WHEN source_channel_name LIKE '%Travel%' OR source_channel_name LIKE '%Food%' OR source_channel_name LIKE '%Fashion%' OR source_channel_name LIKE '%Health%' OR source_channel_name LIKE '%Mindfulness%' THEN '{"lifestyle": 0.9, "health": 0.5}'
        WHEN source_channel_name LIKE '%Learn%' OR source_channel_name LIKE '%Book%' OR source_channel_name LIKE '%Data%' THEN '{"education": 0.9, "technology": 0.6}'
        WHEN source_channel_name LIKE '%News%' OR source_channel_name LIKE '%Reuters%' OR source_channel_name LIKE '%BBC%' THEN '{"news": 0.9, "world": 0.7}'
        ELSE '{"general": 0.8, "mixed": 0.5}'
    END,
    CASE
        WHEN content LIKE '%AI%' OR content LIKE '%OpenAI%' THEN '{"company": ["OpenAI", "Anthropic"], "technology": ["AI", "ML"]}'
        WHEN content LIKE '%Apple%' OR content LIKE '%iPhone%' THEN '{"company": ["Apple"], "product": ["iPhone", "Vision Pro"]}'
        WHEN content LIKE '%Google%' OR content LIKE '%Microsoft%' THEN '{"company": ["Google", "Microsoft"]}'
        WHEN content LIKE '%NASA%' OR content LIKE '%SpaceX%' THEN '{"organization": ["NASA", "SpaceX"], "person": ["Elon Musk"]}'
        ELSE '{"entities": []}'
    END,
    CASE
        WHEN content_type = 'SHORT_POST' THEN '["news", "update", "brief"]'
        WHEN content_type = 'MEDIUM_POST' THEN '["analysis", "overview", "guide"]'
        WHEN content_type = 'LONG_ARTICLE' THEN '["tutorial", "deep-dive", "comprehensive"]'
        WHEN content_type = 'VIDEO_POST' THEN '["video", "visual", "multimedia"]'
        WHEN content_type = 'IMAGE_POST' THEN '["image", "gallery", "visual"]'
        ELSE '["content", "post"]'
    END,
    CASE
        WHEN content LIKE '%breakthrough%' OR content LIKE '%success%' OR content LIKE '%win%' OR content LIKE '%record%' OR content LIKE '%best%' THEN 'POSITIVE'
        WHEN content LIKE '%fail%' OR content LIKE '%loss%' OR content LIKE '%crisis%' OR content LIKE '%vulnerability%' THEN 'NEGATIVE'
        ELSE 'NEUTRAL'
    END,
    CASE
        WHEN content LIKE '%breakthrough%' OR content LIKE '%success%' OR content LIKE '%win%' THEN 0.8
        WHEN content LIKE '%fail%' OR content LIKE '%loss%' OR content LIKE '%crisis%' THEN -0.6
        ELSE 0.1
    END,
    'en',
    CEIL(content_length / 200.0)::INTEGER,
    CASE
        WHEN content_type = 'LONG_ARTICLE' THEN 0.7
        WHEN content_type = 'MEDIUM_POST' THEN 0.5
        ELSE 0.3
    END
FROM content_posts;

-- ===========================================
-- Sample Views for Trending Data
-- ===========================================

-- Generate some views for trending calculation
INSERT INTO content_views (content_post_id, user_id, session_id, viewed_at)
SELECT
    cp.id,
    CASE WHEN random() > 0.5 THEN gen_random_uuid() ELSE NULL END,
    gen_random_uuid(),
    NOW() - (random() * INTERVAL '48 hours')
FROM content_posts cp
CROSS JOIN generate_series(1, (random() * 20 + 5)::integer) AS s;
