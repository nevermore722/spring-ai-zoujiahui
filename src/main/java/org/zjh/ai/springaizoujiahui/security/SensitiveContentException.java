package org.zjh.ai.springaizoujiahui.security;

/**
 * Thrown when user input contains sensitive/unsafe content and must be blocked.
 */
public class SensitiveContentException extends RuntimeException {
    public SensitiveContentException(String message) {
        super(message);
    }
}

