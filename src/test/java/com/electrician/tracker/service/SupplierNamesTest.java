package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SupplierNamesTest {

    @Test
    void listsEachSupplierOnceWhateverTheSpelling() {
        assertThat(SupplierNames.distinct(List.of("Elektrik Market", "elektrik market", "ELEKTRİK  MARKET", "Ağaç Ltd")))
                .containsExactly("Ağaç Ltd", "Elektrik Market");
    }

    @Test
    void keepsTheExistingSpellingForAVariant() {
        List<String> existing = List.of("Elektrik Market");
        assertThat(SupplierNames.canonical("  elektrik   market ", existing)).isEqualTo("Elektrik Market");
        assertThat(SupplierNames.canonical("Yeni  Tedarikçi", existing)).isEqualTo("Yeni Tedarikçi");
        assertThat(SupplierNames.canonical("  ", existing)).isNull();
    }
}
