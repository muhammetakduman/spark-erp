package com.electrician.tracker.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Monday to Sunday of the work plan: each day's entries, and per employee
 * where they are on each day (titles of the entries they are on).
 */
public record WeekPlan(List<LocalDate> days, Map<LocalDate, List<DailyJobCard>> cardsByDay,
        List<EmployeeWeek> employees) {

    public List<DailyJobCard> cardsOn(LocalDate day) {
        return cardsByDay.getOrDefault(day, List.of());
    }

    /** One employee's row: day → titles of the entries they go to. */
    public record EmployeeWeek(Long employeeId, String name, Map<LocalDate, List<String>> titlesByDay) {

        public List<String> titlesOn(LocalDate day) {
            return titlesByDay.getOrDefault(day, List.of());
        }
    }
}
