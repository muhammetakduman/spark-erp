package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.dto.VatBreakdown.VatRateAmount;
import com.electrician.tracker.service.KdvHesaplayici.Line;
import org.junit.jupiter.api.Test;

class KdvHesaplayiciTest {

    @Test
    void includedVatIsExtractedFromTheAmount() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(line("120.00", 20, true)));

        assertThat(result.excludingVat()).isEqualByComparingTo("100.00");
        assertThat(result.vatTotal()).isEqualByComparingTo("20.00");
        assertThat(result.includingVat()).isEqualByComparingTo("120.00");
    }

    @Test
    void excludedVatIsAddedOnTopOfTheAmount() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(line("100.00", 20, false)));

        assertThat(result.excludingVat()).isEqualByComparingTo("100.00");
        assertThat(result.vatTotal()).isEqualByComparingTo("20.00");
        assertThat(result.includingVat()).isEqualByComparingTo("120.00");
    }

    @Test
    void nullRateMeansNoVat() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(line("50.00", null, null)));

        assertThat(result.hasVat()).isFalse();
        assertThat(result.excludingVat()).isEqualByComparingTo("50.00");
        assertThat(result.vatTotal()).isEqualByComparingTo("0.00");
        assertThat(result.includingVat()).isEqualByComparingTo("50.00");
    }

    @Test
    void mixedRatesAreReportedPerRateInAscendingOrder() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(
                line("100.00", 20, false),
                line("110.00", 10, true),
                line("30.00", null, null)));

        assertThat(result.rates()).extracting(VatRateAmount::rate).containsExactly(10, 20);
        assertThat(result.rates().get(0).excludingVat()).isEqualByComparingTo("100.00");
        assertThat(result.rates().get(0).vatAmount()).isEqualByComparingTo("10.00");
        assertThat(result.rates().get(1).excludingVat()).isEqualByComparingTo("100.00");
        assertThat(result.rates().get(1).vatAmount()).isEqualByComparingTo("20.00");
        assertThat(result.excludingVat()).isEqualByComparingTo("230.00");
        assertThat(result.vatTotal()).isEqualByComparingTo("30.00");
        assertThat(result.includingVat()).isEqualByComparingTo("260.00");
    }

    @Test
    void linesAreGroupedByRateSoNoCentDifferenceAccumulates() {
        // Rounded line by line each 0,10 would give 0,08 + 0,02, summing to 0,24 + 0,06.
        VatBreakdown result = KdvHesaplayici.calculate(List.of(
                line("0.10", 20, true),
                line("0.10", 20, true),
                line("0.10", 20, true)));

        assertThat(result.excludingVat()).isEqualByComparingTo("0.25");
        assertThat(result.vatTotal()).isEqualByComparingTo("0.05");
        assertThat(result.includingVat()).isEqualByComparingTo("0.30");
    }

    @Test
    void missingIncludedFlagWithRateIsTreatedAsExcluded() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(line("100.00", 10, null)));

        assertThat(result.vatTotal()).isEqualByComparingTo("10.00");
        assertThat(result.includingVat()).isEqualByComparingTo("110.00");
    }

    @Test
    void linesWithoutAmountAreIgnored() {
        VatBreakdown result = KdvHesaplayici.calculate(List.of(new Line(null, 20, true)));

        assertThat(result.hasVat()).isFalse();
        assertThat(result.includingVat()).isEqualByComparingTo("0.00");
    }

    private static Line line(String amount, Integer rate, Boolean included) {
        return new Line(new BigDecimal(amount), rate, included);
    }
}
