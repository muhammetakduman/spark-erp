package com.electrician.tracker.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Free-text names typed again and again (suppliers on material lines, product
 * brands and categories). Names that differ only in case, Turkish letters or
 * spacing ("Elektrik Market" / "elektrik  market") are the same name: the
 * suggestion list shows each once, and a newly typed variant is stored with
 * the spelling already in use.
 */
public final class CanonicalNames {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private CanonicalNames() {
    }

    /** One entry per name (first spelling wins), sorted alphabetically in Turkish. */
    public static List<String> distinct(List<String> names) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                byKey.putIfAbsent(MetinKarsilastirici.normalize(name), name.trim());
            }
        }
        List<String> result = new ArrayList<>(byKey.values());
        result.sort(Collator.getInstance(TURKISH));
        return result;
    }

    /** The existing spelling of {@code typed}, if any. */
    public static Optional<String> findSame(String typed, List<String> existing) {
        String key = MetinKarsilastirici.normalize(typed);
        return existing.stream()
                .filter(name -> MetinKarsilastirici.normalize(name).equals(key))
                .findFirst();
    }

    /** {@code null} for blank text, otherwise the existing spelling or the trimmed, single-spaced text. */
    public static String canonical(String typed, List<String> existing) {
        if (typed == null || typed.isBlank()) {
            return null;
        }
        return findSame(typed, existing).orElseGet(() -> typed.trim().replaceAll("\\s+", " "));
    }
}
