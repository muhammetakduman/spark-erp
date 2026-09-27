package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetinKarsilastiriciTest {

    @Test
    void foldsTurkishCharactersAndCase() {
        assertThat(MetinKarsilastirici.normalize("ŞALTER Işık Çağrı Üğö"))
                .isEqualTo(MetinKarsilastirici.normalize("salter isik cagri ugo"));
    }

    @Test
    void treatsDottedAndDotlessCapitalIAlike() {
        assertThat(MetinKarsilastirici.normalize("İNCE KABLO")).isEqualTo("ince kablo");
        assertThat(MetinKarsilastirici.normalize("IŞIK")).isEqualTo("isik");
    }

    @Test
    void trimsAndCollapsesWhitespace() {
        assertThat(MetinKarsilastirici.normalize("  NYM   3x2,5 \t kablo ")).isEqualTo("nym 3x2,5 kablo");
    }

    @Test
    void returnsEmptyForNull() {
        assertThat(MetinKarsilastirici.normalize(null)).isEmpty();
    }
}
