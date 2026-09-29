package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import org.junit.jupiter.api.Test;

class QuoteRulesTest {

    @Test
    void acceptsTextsUpToTheirLimits() {
        assertThatCode(() -> QuoteRules.validate(draft("x".repeat(QuoteFieldLimits.COMPANY_NAME),
                "n".repeat(QuoteFieldLimits.NOTES), line("b".repeat(QuoteFieldLimits.BRAND))))).doesNotThrowAnyException();
    }

    @Test
    void rejectsLongerTexts() {
        assertThatThrownBy(() -> QuoteRules.validate(draft("x".repeat(QuoteFieldLimits.COMPANY_NAME + 1), null,
                line(null)))).hasMessage("error.quote.text.tooLong");
        assertThatThrownBy(() -> QuoteRules.validate(draft("Firma", "n".repeat(QuoteFieldLimits.NOTES + 1),
                line(null)))).hasMessage("error.quote.text.tooLong");
        assertThatThrownBy(() -> QuoteRules.validateLine(line("b".repeat(QuoteFieldLimits.BRAND + 1))))
                .hasMessage("error.quote.text.tooLong");
    }

    @Test
    void emptyBrandIsAllowed() {
        assertThatCode(() -> QuoteRules.validateLine(line(""))).doesNotThrowAnyException();
    }

    private static QuoteLine line(String brand) {
        return new QuoteLine(1, 5L, "Kablo", brand, BigDecimal.ONE, ProductUnit.METER, BigDecimal.TEN, null);
    }

    private static QuoteDraft draft(String companyName, String notes, QuoteLine line) {
        return new QuoteDraft("2026/0001", LocalDate.of(2026, 9, 28), 15, null, companyName, null, null, null, null,
                null, null, DiscountType.NONE, null, null, 20, notes, QuoteStatus.DRAFT, List.of(line), false,
                null, null);
    }
}
