package com.electrician.tracker.ui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.dto.WorkedDay;
import org.junit.jupiter.api.Test;

class BicimlendiriciTest {

    @Test
    void formatsMoneyInTurkishConvention() {
        assertThat(Bicimlendirici.money(new BigDecimal("12450.5"))).isEqualTo("12.450,50 ₺");
        assertThat(Bicimlendirici.money(new BigDecimal("0.125"))).isEqualTo("0,13 ₺");
        assertThat(Bicimlendirici.money(null)).isEmpty();
    }

    @Test
    void parsesMoneyWithCommaDecimalsAndOptionalSymbol() {
        assertThat(Bicimlendirici.parseMoney("12.450,50 ₺")).isEqualByComparingTo("12450.50");
        assertThat(Bicimlendirici.parseMoney("2500")).isEqualByComparingTo("2500");
        assertThat(Bicimlendirici.parseMoney("  ")).isNull();
        assertThatThrownBy(() -> Bicimlendirici.parseMoney("abc")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void differentlyTypedSameAmountsParseEqually() {
        for (String text : new String[] { "1000", "1.000", "1000,00", "1.000,00", "1.000,00 ₺", "1.000.00", "1000.00" }) {
            assertThat(Bicimlendirici.parseMoney(text)).as(text).isEqualByComparingTo("1000");
        }
        assertThat(Bicimlendirici.parseMoney("12.5")).isEqualByComparingTo("12.5");
    }

    @Test
    void rejectsAmbiguousDots() {
        assertThatThrownBy(() -> Bicimlendirici.parseMoney("1.00.0")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> Bicimlendirici.parseMoney("1,000,00")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void formatsDatesQuantitiesAndDays() {
        assertThat(Bicimlendirici.date(LocalDate.of(2026, 9, 3))).isEqualTo("03.09.2026");
        assertThat(Bicimlendirici.quantity(new BigDecimal("2.50"))).isEqualTo("2,5");
        assertThat(Bicimlendirici.quantity(new BigDecimal("1200"))).isEqualTo("1.200");
        assertThat(Bicimlendirici.days(new BigDecimal("2.5"))).isEqualTo("2,5");
        assertThat(Bicimlendirici.days(new BigDecimal("3.0"))).isEqualTo("3");
        assertThat(Bicimlendirici.factor(new BigDecimal("0.5"))).isEqualTo("0,5");
    }

    @Test
    void listsWorkedDaysMarkingHalfDays() {
        List<WorkedDay> days = List.of(
                new WorkedDay(LocalDate.of(2026, 9, 12), BigDecimal.ONE),
                new WorkedDay(LocalDate.of(2026, 9, 13), new BigDecimal("0.5")));

        assertThat(Bicimlendirici.workedDays(days)).isEqualTo("12.09, 13.09 (½)");
    }
}
