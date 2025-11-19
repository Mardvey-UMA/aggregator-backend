package com.contentaggregation.content.enums;

/**
 * Enumeration of content sources for the aggregation system.
 *
 * <p>Supported sources:
 * <ul>
 *   <li>TELEGRAM: Content from Telegram channels</li>
 *   <li>VK: Content from VKontakte groups/pages</li>
 *   <li>RSS: Content from RSS/Atom feeds</li>
 *   <li>TWITTER: Content from Twitter/X</li>
 *   <li>MANUAL: Manually created content</li>
 * </ul>
 */
public enum ContentSource {
    TELEGRAM,
    VK,
    RSS,
    TWITTER,
    MANUAL
}
