package com.chatapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputValidator {

    @Value("${chat.max-username-length:30}")
    private int maxUsernameLength;

    @Value("${chat.max-message-length:2000}")
    private int maxMessageLength;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches() && username.length() <= maxUsernameLength;
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isValidMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        return message.length() <= maxMessageLength;
    }

    public boolean isValidRoomName(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return false;
        }
        return roomName.length() >= 3 && roomName.length() <= 100;
    }

    public String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        // Basic HTML/script tag sanitization
        return message.replaceAll("<script[^>]*>.*?</script>", "")
                     .replaceAll("<[^>]+>", "")
                     .trim();
    }
}
