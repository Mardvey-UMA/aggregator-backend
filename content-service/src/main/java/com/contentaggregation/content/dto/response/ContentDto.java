package com.contentaggregation.content.dto.response;

import com.contentaggregation.content.enums.ContentSource;
import com.contentaggregation.content.enums.ContentType;
import com.contentaggregation.content.enums.MediaType;
import com.contentaggregation.content.enums.Sentiment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for content post with metadata and recommendation information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Content post with metadata and recommendation data")
public class ContentDto {

    @Schema(description = "Unique content identifier")
    private UUID id;

    @Schema(description = "External identifier from source")
    private String externalId;

    @Schema(description = "Content source platform")
    private ContentSource source;

    @Schema(description = "Source channel identifier")
    private String sourceChannelId;

    @Schema(description = "Source channel name")
    private String sourceChannelName;

    @Schema(description = "Content title (may be null)")
    private String title;

    @Schema(description = "Content text")
    private String content;

    @Schema(description = "Type of content")
    private ContentType contentType;

    @Schema(description = "Content length in characters")
    private Integer contentLength;

    @Schema(description = "Whether content has media attachments")
    private Boolean hasMedia;

    @Schema(description = "Type of media attached")
    private MediaType mediaType;

    @Schema(description = "URLs of media attachments")
    private List<String> mediaUrls;

    @Schema(description = "External link URL")
    private String linkUrl;

    @Schema(description = "Publication timestamp")
    private LocalDateTime publishedAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    // Metadata fields
    @Schema(description = "Content categories with scores")
    private Map<String, Double> categories;

    @Schema(description = "Extracted entities")
    private Map<String, List<String>> entities;

    @Schema(description = "Content keywords")
    private List<String> keywords;

    @Schema(description = "Content sentiment")
    private Sentiment sentiment;

    @Schema(description = "Sentiment score (-1.0 to 1.0)")
    private Float sentimentScore;

    @Schema(description = "Content language")
    private String language;

    @Schema(description = "Estimated reading time in minutes")
    private Integer readingTimeMinutes;

    @Schema(description = "Content complexity score (0.0 to 1.0)")
    private Float complexityScore;

    // Recommendation fields
    @Schema(description = "Recommendation score (0.0 to 1.0)")
    private Double recommendationScore;

    @Schema(description = "Reason for recommendation")
    private String recommendationReason;

    @Schema(description = "Recommendation tracking ID")
    private UUID recommendationId;

    // Engagement metrics
    @Schema(description = "Number of views")
    private Long viewCount;

    @Schema(description = "Number of bookmarks")
    private Long bookmarkCount;

    @Schema(description = "Whether current user has bookmarked this content")
    private Boolean isBookmarked;

    /**
     * Gets the primary category name.
     *
     * @return the category with highest score, or null
     */
    public String getPrimaryCategory() {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Checks if content is long-form.
     *
     * @return true if content length exceeds 2000 characters
     */
    public boolean isLongForm() {
        return contentLength != null && contentLength > 2000;
    }
}
