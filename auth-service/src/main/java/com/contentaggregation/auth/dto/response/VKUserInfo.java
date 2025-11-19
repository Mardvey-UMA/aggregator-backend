package com.contentaggregation.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for VK API user information response.
 *
 * @param id VK user ID
 * @param email user's email from VK
 * @param firstName user's first name
 * @param lastName user's last name
 * @param screenName user's screen name (username)
 * @param photo user's profile photo URL
 */
public record VKUserInfo(
    @JsonProperty("id")
    Long id,

    @JsonProperty("email")
    String email,

    @JsonProperty("first_name")
    String firstName,

    @JsonProperty("last_name")
    String lastName,

    @JsonProperty("screen_name")
    String screenName,

    @JsonProperty("photo_200")
    String photo
) {
    /**
     * Gets the full name of the user.
     *
     * @return combined first and last name
     */
    public String getFullName() {
        return (firstName != null ? firstName : "") +
               (lastName != null ? " " + lastName : "");
    }

    /**
     * Gets a username, preferring screen name over id.
     *
     * @return screen name or id-based username
     */
    public String getUsername() {
        if (screenName != null && !screenName.isEmpty()) {
            return screenName;
        }
        return "vk_" + id;
    }
}
