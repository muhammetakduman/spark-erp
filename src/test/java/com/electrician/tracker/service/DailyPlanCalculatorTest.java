package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.domain.DailyJob;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DayPlan;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DailyPlanCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 29);

    @Test
    void countsPostponementsAlongTheChain() {
        Map<Long, Long> sourceById = Map.of(3L, 2L, 2L, 1L);

        assertThat(DailyPlanCalculator.postponeCount(1L, sourceById)).isZero();
        assertThat(DailyPlanCalculator.postponeCount(2L, sourceById)).isEqualTo(1);
        assertThat(DailyPlanCalculator.postponeCount(3L, sourceById)).isEqualTo(2);
    }

    @Test
    void chainLoopDoesNotHang() {
        assertThat(DailyPlanCalculator.postponeCount(1L, Map.of(1L, 2L, 2L, 1L))).isEqualTo(2);
    }

    @Test
    void urgentFirstThenByTimeAndCountersPerStatus() {
        DailyJob late = entry(1L, "Pano bakımı", "14:00", DailyJobPriority.NORMAL);
        DailyJob early = entry(2L, "Priz arızası", "09:00", DailyJobPriority.NORMAL);
        DailyJob urgent = entry(3L, "Kaçak akım", "16:00", DailyJobPriority.URGENT);
        early.complete(null);
        late.postponeTo(DAY.plusDays(1), null);

        DayPlan plan = DailyPlanCalculator.dayPlan(DAY, List.of(late, early, urgent), List.of());

        assertThat(plan.cards()).extracting(DailyJobCard::title)
                .containsExactly("Kaçak akım", "Priz arızası", "Pano bakımı");
        assertThat(plan.plannedCount()).isEqualTo(1);
        assertThat(plan.completedCount()).isEqualTo(1);
        assertThat(plan.notVisitedCount()).isEqualTo(1);
    }

    @Test
    void weekStartsOnMonday() {
        assertThat(DailyPlanCalculator.weekStart(LocalDate.of(2026, 10, 4))).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    private static DailyJob entry(Long id, String title, String time, DailyJobPriority priority) {
        DailyJob job = new DailyJob(DAY, title, 1L, LocalDateTime.of(2026, 9, 28, 8, 0));
        ReflectionTestUtils.setField(job, "id", id);
        job.setTimeOfDay(time);
        job.setPriority(priority);
        return job;
    }
}
