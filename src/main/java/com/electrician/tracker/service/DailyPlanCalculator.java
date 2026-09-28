package com.electrician.tracker.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.DailyJob;
import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobLink;
import com.electrician.tracker.dto.DayPlan;
import com.electrician.tracker.dto.WeekPlan;

/**
 * Pure arithmetic of the work plan: cards (urgent first, then by time, then
 * title), day counters, the week grid and how many times an entry was already
 * postponed (following the "moved from" chain, safe against loops).
 */
public final class DailyPlanCalculator {

    private static final int DAYS_IN_WEEK = 7;
    private static final Comparator<DailyJobCard> CARD_ORDER = Comparator
            .comparing(DailyJobCard::isUrgent).reversed()
            .thenComparing(DailyJobCard::timeOfDay, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DailyJobCard::title, String.CASE_INSENSITIVE_ORDER);

    private DailyPlanCalculator() {
    }

    public static DayPlan dayPlan(LocalDate date, List<DailyJob> entries, List<DailyJobLink> links) {
        List<DailyJobCard> cards = cards(entries, links);
        return new DayPlan(date, cards,
                count(cards, DailyJobStatus.PLANNED),
                count(cards, DailyJobStatus.COMPLETED),
                count(cards, DailyJobStatus.NOT_VISITED) + count(cards, DailyJobStatus.POSTPONED));
    }

    /** Monday of the week that contains {@code date}. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    public static WeekPlan weekPlan(LocalDate anyDay, List<DailyJob> entries, List<DailyJobLink> links,
            List<Employee> activeEmployees) {
        LocalDate monday = weekStart(anyDay);
        List<LocalDate> days = monday.datesUntil(monday.plusDays(DAYS_IN_WEEK)).toList();
        List<DailyJobCard> cards = cards(entries, links);
        Map<LocalDate, List<DailyJobCard>> byDay = cards.stream()
                .collect(Collectors.groupingBy(DailyJobCard::date, LinkedHashMap::new, Collectors.toList()));
        return new WeekPlan(days, byDay, employeeRows(entries, activeEmployees));
    }

    /** Cards of {@code entries} in plan order. */
    public static List<DailyJobCard> cards(List<DailyJob> entries, List<DailyJobLink> links) {
        Map<Long, Long> sourceById = links.stream()
                .collect(Collectors.toMap(DailyJobLink::id, DailyJobLink::sourceId, (first, second) -> first));
        return entries.stream()
                .map(entry -> DailyJobMapper.toCard(entry, postponeCount(entry.getId(), sourceById)))
                .sorted(CARD_ORDER)
                .toList();
    }

    /** How many earlier entries this one descends from; 0 for an entry planned directly. */
    public static int postponeCount(Long id, Map<Long, Long> sourceById) {
        Set<Long> seen = new HashSet<>();
        int count = 0;
        Long current = sourceById.get(id);
        while (current != null && seen.add(current)) {
            count++;
            current = sourceById.get(current);
        }
        return count;
    }

    /** Active employees plus anyone planned this week, each with the entries they go to per day. */
    private static List<WeekPlan.EmployeeWeek> employeeRows(List<DailyJob> entries, List<Employee> activeEmployees) {
        Map<Long, Employee> employees = new LinkedHashMap<>();
        activeEmployees.forEach(employee -> employees.put(employee.getId(), employee));
        entries.forEach(entry -> entry.getEmployees().forEach(employee -> employees.putIfAbsent(employee.getId(),
                employee)));
        List<DailyJob> counted = entries.stream().filter(entry -> entry.getStatus() != DailyJobStatus.CANCELLED)
                .sorted(Comparator.comparing(DailyJob::getTimeOfDay, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<WeekPlan.EmployeeWeek> rows = new ArrayList<>();
        for (Employee employee : employees.values()) {
            Map<LocalDate, List<String>> titles = counted.stream()
                    .filter(entry -> entry.getEmployees().stream().anyMatch(e -> e.getId().equals(employee.getId())))
                    .collect(Collectors.groupingBy(DailyJob::getJobDate,
                            Collectors.mapping(DailyJob::getTitle, Collectors.toList())));
            rows.add(new WeekPlan.EmployeeWeek(employee.getId(), employee.getName(), titles));
        }
        rows.sort(Comparator.comparing(WeekPlan.EmployeeWeek::name, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private static int count(List<DailyJobCard> cards, DailyJobStatus status) {
        return (int) cards.stream().filter(card -> card.status() == status).count();
    }
}
