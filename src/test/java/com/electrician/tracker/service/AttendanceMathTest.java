package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.dto.EmployeeWageSummary;
import org.junit.jupiter.api.Test;

class AttendanceMathTest {

    private static final BigDecimal WAGE = new BigDecimal("2500.00");

    private final Employee mustafa = new Employee("Mustafa", WAGE, true, true);
    private final Employee ali = new Employee("Ali", WAGE, false, true);

    @Test
    void dayCountIsTheSumOfDayFactorsNotTheRowCount() {
        List<Attendance> rows = List.of(row(mustafa, 12, "1.0"), row(mustafa, 13, "0.5"), row(mustafa, 14, "0.5"));

        assertThat(AttendanceMath.dayCount(rows)).isEqualByComparingTo("2.0");
    }

    @Test
    void halfDayEarnsHalfTheFullDayWage() {
        List<Attendance> rows = List.of(row(mustafa, 12, "1.0"), row(mustafa, 13, "0.5"));

        assertThat(AttendanceMath.totalWage(rows)).isEqualByComparingTo("3750.00");
    }

    @Test
    void averageWageIsTotalDividedByDaysAndZeroWithoutDays() {
        assertThat(AttendanceMath.averageWage(new BigDecimal("3750.00"), new BigDecimal("1.5")))
                .isEqualByComparingTo("2500.00");
        assertThat(AttendanceMath.averageWage(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }

    @Test
    void onlyHalfAndFullDayFactorsAreAllowed() {
        assertThat(AttendanceMath.isAllowedFactor(new BigDecimal("0.50"))).isTrue();
        assertThat(AttendanceMath.isAllowedFactor(new BigDecimal("1.0"))).isTrue();
        assertThat(AttendanceMath.isAllowedFactor(new BigDecimal("0.75"))).isFalse();
        assertThat(AttendanceMath.isAllowedFactor(null)).isFalse();
    }

    @Test
    void summarizesPerEmployeeSortedByName() {
        List<Attendance> rows = List.of(row(mustafa, 12, "1.0"), row(ali, 12, "0.5"), row(mustafa, 13, "1.0"));

        List<EmployeeWageSummary> summaries = AttendanceMath.summarizeByEmployee(rows);

        assertThat(summaries).extracting(EmployeeWageSummary::employeeName).containsExactly("Ali", "Mustafa");
        assertThat(summaries.get(0).dayCount()).isEqualByComparingTo("0.5");
        assertThat(summaries.get(0).totalWage()).isEqualByComparingTo("1250.00");
        assertThat(summaries.get(1).dayCount()).isEqualByComparingTo("2.0");
        assertThat(summaries.get(1).totalWage()).isEqualByComparingTo("5000.00");
    }

    private static Attendance row(Employee employee, int dayOfMonth, String factor) {
        return new Attendance(null, employee, LocalDate.of(2026, 9, dayOfMonth), WAGE, new BigDecimal(factor), null);
    }
}
