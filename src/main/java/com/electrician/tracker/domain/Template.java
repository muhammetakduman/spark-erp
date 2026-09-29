package com.electrician.tracker.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reusable content ("Şablon"). Text types keep plain text; an item set keeps
 * its lines as JSON. At most one template of a type is the default.
 */
@Entity
@Table(name = "template")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType type;

    @Column(nullable = false)
    private String content;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTemplate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    protected Template() {
    }

    public Template(String name, TemplateType type, String content, LocalDateTime createdAt) {
        this.name = name;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TemplateType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /** Counts one more use (terms picked, list added, job title used). */
    public void markUsed(LocalDateTime when) {
        this.useCount++;
        this.lastUsedAt = when;
    }

    public int getUseCount() {
        return useCount;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public boolean isDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(boolean defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
