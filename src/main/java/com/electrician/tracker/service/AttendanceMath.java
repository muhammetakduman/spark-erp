package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.WorkedDay;

/**
 * Attendance arithmetic in one place. The daily wage is a FULL-day wage, so
 * a row earns dailyWage × dayFactor, and a day count is always the sum of
 * day factors, never a row count.
 */
public final class AttendanceMath {

    public static final BigDecimal HALF_DAY = new BigDecimal("0.5");
    public static final BigDecimal FULL_DAY = BigDecimal.ONE;
    private static final Set<BigDecimal> ALLOWED_FACTORS = Set.of(HALF_DAY, FULL_DAY);
    private static final int MONEY_SCALE = 2;

    private AttendanceMath() {
    }

    public static boolean isAllowedFactor(BigDecimal factor) {
        return factor != null && ALLOWED_FACTORS.stream().anyMatch(allowed -> allowed.compareTo(factor) == 0);
    }

    public static BigDecimal wageAmount(Attendance attendance) {
        return attendance.getDailyWage().multiply(factorOf(attendance));
    }

    public static BigDecimal dayCount(List<Attendance> attendances) {
        return attendances.stream().map(AttendanceMath::factorOf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalWage(List<Attendance> attendances) {
        return scale(attendances.stream().map(AttendanceMath::wageAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Average full-day wage: total / days; zero when there are no days. */
    public static BigDecimal averageWage(BigDecimal totalWage, BigDecimal dayCount) {
        if (dayCount.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }
        return totalWage.divide(dayCount, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static List<WorkedDay> workedDays(List<Attendance> attendances) {
        return attendances.stream()
                .sorted(Comparator.comparing(Attendance::getAttendanceDate))
                .map(a -> new WorkedDay(a.getAttendanceDate(), factorOf(a)))
                .toList();
    }

    /** Per-employee totals, sorted by name. */
    public static List<EmployeeWageSummary> summarizeByEmployee(List<Attendance> attendances) {
        // Keyed by entity reference: within one persistence context there is a
        // single instance per row, and this also works for unsaved entities.
        Map<Employee, List<Attendance>> byEmployee = groupBy(attendances, Attendance::getEmployee);
        return byEmployee.entrySet().stream()
                .map(entry -> toSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EmployeeWageSummary::employeeName))
                .toList();
    }

    static <K> Map<K, List<Attendance>> groupBy(List<Attendance> attendances, Function<Attendance, K> key) {
        Map<K, List<Attendance>> groups = new LinkedHashMap<>();
        for (Attendance attendance : attendances) {
            groups.computeIfAbsent(key.apply(attendance), k -> new java.util.ArrayList<>()).add(attendance);
        }
        return groups;
    }

    private static EmployeeWageSummary toSummary(Employee employee, List<Attendance> rows) {
        BigDecimal days = dayCount(rows);
        BigDecimal total = totalWage(rows);
        return new EmployeeWageSummary(employee.getId(), employee.getName(), employee.isMaster(), days,
                averageWage(total, days), total, workedDays(rows));
    }

    static BigDecimal factorOf(Attendance attendance) {
        return attendance.getDayFactor() == null ? FULL_DAY : attendance.getDayFactor();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
