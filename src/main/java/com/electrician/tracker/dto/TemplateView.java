package com.electrician.tracker.dto;

import java.time.LocalDateTime;

import com.electrician.tracker.domain.TemplateType;

/**
 * A template as screens show it. {@code content} is the text of a text
 * template; for an item set it is empty and {@code lineCount} tells its size.
 * {@code useCount} / {@code lastUsedAt}: how often and when it was last used.
 */
public record TemplateView(Long id, String name, TemplateType type, String content, boolean defaultTemplate,
        int lineCount, int useCount, LocalDateTime lastUsedAt) {

    @Override
    public String toString() {
        return name;
    }
}
