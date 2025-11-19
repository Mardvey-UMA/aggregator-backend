package com.contentaggregation.content.exception;

import java.util.UUID;

/**
 * Exception thrown when a content post is not found.
 */
public class ContentNotFoundException extends RuntimeException {

    private final String code;

    public ContentNotFoundException(UUID contentId) {
        super("Content not found with id: " + contentId);
        this.code = "CONTENT_NOT_FOUND";
    }

    public ContentNotFoundException(String message) {
        super(message);
        this.code = "CONTENT_NOT_FOUND";
    }

    public String getCode() {
        return code;
    }
}
