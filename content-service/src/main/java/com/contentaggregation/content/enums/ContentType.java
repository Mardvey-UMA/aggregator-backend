package com.contentaggregation.content.enums;

/**
 * Enumeration of content types supported by the aggregation system.
 *
 * <p>Content types are categorized by format and length:
 * <ul>
 *   <li>SHORT_POST: Less than 500 characters, Twitter-like</li>
 *   <li>MEDIUM_POST: 500-2000 characters</li>
 *   <li>LONG_ARTICLE: More than 2000 characters, up to 4096</li>
 *   <li>VIDEO_POST: Post with video content</li>
 *   <li>IMAGE_POST: Post with images</li>
 *   <li>MIXED_MEDIA: Post with multiple media types</li>
 * </ul>
 */
public enum ContentType {
    SHORT_POST,
    MEDIUM_POST,
    LONG_ARTICLE,
    VIDEO_POST,
    IMAGE_POST,
    MIXED_MEDIA
}
