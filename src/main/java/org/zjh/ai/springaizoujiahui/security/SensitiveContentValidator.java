package org.zjh.ai.springaizoujiahui.security;

/**
 * Validates user input before calling LLM providers.
 * <p>
 * Prefer blocking unsafe inputs early (before streaming starts).
 */
public interface SensitiveContentValidator {
    void validateOrThrow(String message);
}

