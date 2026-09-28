package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CanonicalNamesTest {

    @Test
    void listsEachSupplierOnceWhateverTheSpelling() {
        assertThat(CanonicalNames.distinct(List.of("Elektrik Market", "elektrik market", "ELEKTRİK  MARKET", "Ağaç Ltd")))
                .containsExactly("Ağaç Ltd", "Elektrik Market");
    }

    @Test
    void keepsTheExistingSpellingForAVariant() {
        List<String> existing = List.of("Elektrik Market");
        assertThat(CanonicalNames.canonical("  elektrik   market ", existing)).isEqualTo("Elektrik Market");
        assertThat(CanonicalNames.canonical("Yeni  Tedarikçi", existing)).isEqualTo("Yeni Tedarikçi");
        assertThat(CanonicalNames.canonical("  ", existing)).isNull();
    }
}
