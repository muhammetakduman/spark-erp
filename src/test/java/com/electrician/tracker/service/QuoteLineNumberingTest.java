package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.QuoteLine;
import org.junit.jupiter.api.Test;

class QuoteLineNumberingTest {

    @Test
    void catalogAndFreeLinesAreNumberedTheSameWay() {
        List<QuoteLine> lines = List.of();
        lines = QuoteLineNumbering.append(lines, catalog("Kablo"));
        lines = QuoteLineNumbering.append(lines, free("Sarf malzeme"));
        lines = QuoteLineNumbering.append(lines, catalog("Priz"));
        lines = QuoteLineNumbering.append(lines, free("İşçilik"));
        lines = QuoteLineNumbering.append(lines, catalog("Sigorta"));

        assertThat(lines).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3, 4, 5);
        assertThat(lines).extracting(QuoteLine::productName)
                .containsExactly("Kablo", "Sarf malzeme", "Priz", "İşçilik", "Sigorta");
    }

    @Test
    void movingUpThenRemovingKeepsNumbersWithoutGaps() {
        List<QuoteLine> lines = fiveLines();

        lines = QuoteLineNumbering.moveUp(lines, 2);
        assertThat(lines).extracting(QuoteLine::productName).containsExactly("A", "C", "B", "D", "E");

        lines = QuoteLineNumbering.remove(lines, 1);
        assertThat(lines).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3, 4);
        assertThat(lines).extracting(QuoteLine::productName).containsExactly("A", "B", "D", "E");
    }

    @Test
    void movingPastTheEndsChangesNothing() {
        List<QuoteLine> lines = fiveLines();

        assertThat(QuoteLineNumbering.moveUp(lines, 0)).extracting(QuoteLine::productName)
                .containsExactly("A", "B", "C", "D", "E");
        assertThat(QuoteLineNumbering.moveDown(lines, 4)).extracting(QuoteLine::productName)
                .containsExactly("A", "B", "C", "D", "E");
    }

    @Test
    void insertedLineShiftsTheRestDown() {
        List<QuoteLine> lines = QuoteLineNumbering.insertAt(fiveLines(), 1, free("X"));

        assertThat(lines).extracting(QuoteLine::productName).containsExactly("A", "X", "B", "C", "D", "E");
        assertThat(lines).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void orderComesOnlyFromTheNumberNotFromTheName() {
        List<QuoteLine> shuffled = List.of(free("Z").withLineNo(1), free("A").withLineNo(3), free("M").withLineNo(2));

        assertThat(QuoteLineNumbering.normalize(shuffled)).extracting(QuoteLine::productName)
                .containsExactly("Z", "M", "A");
    }

    @Test
    void allowsAtMostTwentyFiveLines() {
        List<QuoteLine> lines = new ArrayList<>();
        for (int i = 0; i < QuoteLineNumbering.MAX_LINES; i++) {
            lines = new ArrayList<>(QuoteLineNumbering.append(lines, free("Kalem " + i)));
        }
        List<QuoteLine> full = lines;

        assertThat(QuoteLineNumbering.canAdd(full.size())).isFalse();
        assertThatThrownBy(() -> QuoteLineNumbering.append(full, free("26")))
                .hasMessage("error.quote.lines.tooMany");
    }

    @Test
    void itemSetGoesBelowAndNumberingContinues() {
        List<QuoteLine> two = QuoteLineNumbering.appendAll(List.of(), List.of(free("A"), free("B")));
        List<QuoteLine> seven = QuoteLineNumbering.appendAll(two,
                List.of(free("C"), free("D"), free("E"), free("F"), free("G")));

        assertThat(seven).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(seven.get(0).productName()).isEqualTo("A");
    }

    private static List<QuoteLine> fiveLines() {
        return QuoteLineNumbering.appendAll(List.of(),
                List.of(free("A"), free("B"), free("C"), free("D"), free("E")));
    }

    private static QuoteLine catalog(String name) {
        return QuoteLine.unnumbered(1L, name, "Marka", BigDecimal.ONE, ProductUnit.PIECE, BigDecimal.TEN, null);
    }

    private static QuoteLine free(String name) {
        return QuoteLine.unnumbered(null, name, null, BigDecimal.ONE, ProductUnit.PIECE, BigDecimal.TEN, null);
    }
}
