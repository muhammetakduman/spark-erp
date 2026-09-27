package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import org.junit.jupiter.api.Test;

class MaterialPriceCalculatorTest {

    @Test
    void unitEntryTotalIsQuantityTimesUnitPrice() {
        MaterialItem item = item(PriceEntryType.UNIT, "3", "12.50", null);

        assertThat(MaterialPriceCalculator.saleTotal(item)).isEqualByComparingTo("37.50");
    }

    @Test
    void totalEntryUsesEnteredTotalRegardlessOfUnitPrice() {
        MaterialItem item = item(PriceEntryType.TOTAL, "3", "33.33", "100");

        assertThat(MaterialPriceCalculator.saleTotal(item)).isEqualByComparingTo("100");
    }

    @Test
    void derivesUnitPriceFromTotalWithTwoDecimalsHalfUp() {
        assertThat(MaterialPriceCalculator.unitPriceFromTotal(new BigDecimal("100"), new BigDecimal("3")))
                .isEqualByComparingTo("33.33");
        assertThat(MaterialPriceCalculator.unitPriceFromTotal(new BigDecimal("2"), new BigDecimal("3")))
                .isEqualByComparingTo("0.67");
    }

    @Test
    void returnsNullWhenInputsAreMissingOrQuantityIsNotPositive() {
        assertThat(MaterialPriceCalculator.unitPriceFromTotal(new BigDecimal("10"), BigDecimal.ZERO)).isNull();
        assertThat(MaterialPriceCalculator.unitPriceFromTotal(null, BigDecimal.ONE)).isNull();
        assertThat(MaterialPriceCalculator.totalFromUnitPrice(BigDecimal.ONE, null)).isNull();
    }

    private MaterialItem item(PriceEntryType type, String quantity, String unitPrice, String total) {
        return new MaterialItem(null, null, null, new BigDecimal(quantity), null, null,
                unitPrice == null ? null : new BigDecimal(unitPrice), type,
                total == null ? null : new BigDecimal(total), null, null);
    }
}
