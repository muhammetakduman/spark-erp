package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.dto.VatBreakdown.VatRateAmount;

/**
 * The only place VAT is calculated.
 * <ul>
 *   <li>Included: excl. = amount / (1 + rate/100); VAT = amount − excl.</li>
 *   <li>Excluded: VAT = amount × rate/100; incl. = amount + VAT.</li>
 *   <li>No rate: no VAT.</li>
 * </ul>
 * Lines are never rounded one by one: amounts are summed per rate first and
 * rounded once per rate (HALF_UP, 2 digits), so no cent differences add up.
 */
public final class KdvHesaplayici {

    private static final int MONEY_SCALE = 2;
    private static final int INTERMEDIATE_SCALE = 10;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private KdvHesaplayici() {
    }

    /** One priced amount; {@code included == null} with a rate is treated as excluded. */
    public record Line(BigDecimal amount, Integer rate, Boolean included) {
    }

    public static VatBreakdown calculate(List<Line> lines) {
        List<Line> priced = lines.stream().filter(line -> line.amount() != null).toList();
        BigDecimal noVatTotal = round(priced.stream()
                .filter(line -> line.rate() == null)
                .map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<Integer, List<Line>> byRate = priced.stream()
                .filter(line -> line.rate() != null)
                .collect(Collectors.groupingBy(Line::rate, TreeMap::new, Collectors.toList()));
        List<VatRateAmount> rates = byRate.entrySet().stream()
                .map(entry -> calculateRate(entry.getKey(), entry.getValue()))
                .toList();

        BigDecimal excludingVat = rates.stream().map(VatRateAmount::excludingVat)
                .reduce(noVatTotal, BigDecimal::add);
        BigDecimal vatTotal = round(rates.stream().map(VatRateAmount::vatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return new VatBreakdown(excludingVat, rates, vatTotal, excludingVat.add(vatTotal));
    }

    private static VatRateAmount calculateRate(int rate, List<Line> lines) {
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(rate).divide(ONE_HUNDRED));
        BigDecimal grossOfIncluded = sum(lines, true);
        BigDecimal netOfExcluded = sum(lines, false);

        BigDecimal net = round(grossOfIncluded.divide(factor, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                .add(netOfExcluded));
        BigDecimal gross = round(grossOfIncluded.add(netOfExcluded.multiply(factor)));
        return new VatRateAmount(rate, net, gross.subtract(net));
    }

    private static BigDecimal sum(List<Line> lines, boolean included) {
        return lines.stream()
                .filter(line -> Objects.equals(Boolean.TRUE.equals(line.included()), included))
                .map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
