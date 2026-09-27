package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.AttendanceConflict;
import com.electrician.tracker.dto.AttendanceEntry;
import com.electrician.tracker.dto.AttendancePreview;
import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.EmployeeRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttendanceServiceTest {

    private static final Long JOB_ID = 7L;
    private static final Long MUSTAFA_ID = 1L;
    private static final Long ALI_ID = 2L;
    private static final BigDecimal WAGE = new BigDecimal("2500.00");
    private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 11);
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    private final Job completedJob = new Job(new Customer("Ahmet Bey", null, null, null, null), JobType.SITE,
            "Şantiye", null, LocalDate.of(2026, 9, 1), null, JobStatus.COMPLETED, null, null, false, null);

    private AttendanceRepository attendanceRepository;
    private JobRepository jobRepository;
    private EmployeeRepository employeeRepository;
    private AttendanceService service;
    private Employee mustafa;
    private Employee ali;

    @BeforeEach
    void setUp() {
        attendanceRepository = mock(AttendanceRepository.class);
        jobRepository = mock(JobRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new AttendanceService(attendanceRepository, jobRepository, employeeRepository);
        mustafa = employee(MUSTAFA_ID, "Mustafa");
        ali = employee(ALI_ID, "Ali");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(completedJob));
        when(employeeRepository.findAllById(anyList())).thenReturn(List.of(mustafa, ali));
    }

    @Test
    void expandsRangeAndSkipsWeekendOnRequest() {
        assertThat(service.expandDates(FRIDAY, MONDAY, false, false)).hasSize(4);
        assertThat(service.expandDates(FRIDAY, MONDAY, true, true)).containsExactly(FRIDAY, MONDAY);
    }

    @Test
    void rejectsRangeEndingBeforeStart() {
        assertThatThrownBy(() -> service.expandDates(MONDAY, FRIDAY, false, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("error.attendance.range.endBeforeStart");
    }

    @Test
    void previewCountsDayFactorsAndWages() {
        List<LocalDate> dates = List.of(FRIDAY, FRIDAY.plusDays(1), FRIDAY.plusDays(2));
        List<AttendanceEntry> entries = List.of(
                new AttendanceEntry(MUSTAFA_ID, WAGE, BigDecimal.ONE),
                new AttendanceEntry(ALI_ID, new BigDecimal("2000.00"), new BigDecimal("0.5")));

        AttendancePreview preview = service.preview(dates, entries);

        assertThat(preview.dateCount()).isEqualTo(3);
        assertThat(preview.personCount()).isEqualTo(2);
        assertThat(preview.dayCount()).isEqualByComparingTo("4.5");
        assertThat(preview.amount()).isEqualByComparingTo("10500.00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesOneRowPerDateAndEmployeeSkippingExistingOnesEvenOnCompletedJob() {
        List<LocalDate> dates = List.of(FRIDAY, MONDAY);
        when(attendanceRepository.findByJobAndDates(eq(JOB_ID), any()))
                .thenReturn(List.of(new Attendance(completedJob, mustafa, FRIDAY, WAGE)));

        AttendanceSaveResult result = service.saveBatch(JOB_ID, dates, List.of(
                new AttendanceEntry(MUSTAFA_ID, WAGE), new AttendanceEntry(ALI_ID, WAGE)));

        ArgumentCaptor<List<Attendance>> saved = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(3);
        assertThat(result.createdCount()).isEqualTo(3);
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidDayFactor() {
        List<AttendanceEntry> entries = List.of(new AttendanceEntry(MUSTAFA_ID, WAGE, new BigDecimal("0.75")));

        assertThatThrownBy(() -> service.saveBatch(JOB_ID, List.of(FRIDAY), entries))
                .hasMessage("error.attendance.dayFactor.invalid");
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void rejectsNegativeWage() {
        List<AttendanceEntry> entries = List.of(new AttendanceEntry(MUSTAFA_ID, new BigDecimal("-1")));

        assertThatThrownBy(() -> service.saveBatch(JOB_ID, List.of(FRIDAY), entries))
                .hasMessage("error.attendance.wage.negative");
    }

    @Test
    void rejectsBatchWithoutEmployees() {
        assertThatThrownBy(() -> service.saveBatch(JOB_ID, List.of(FRIDAY), List.of()))
                .hasMessage("error.attendance.employee.required");
    }

    @Test
    void warnsWhenAnotherJobPushesTheDayOverOneFullDay() {
        Job otherJob = new Job(new Customer("Mehmet Bey", null, null, null, null), JobType.SERVICE, "Pano arızası",
                null, FRIDAY, null, JobStatus.ACTIVE, null, null, false, null);
        when(attendanceRepository.findOnOtherJobs(eq(JOB_ID), any(), any())).thenReturn(List.of(
                new Attendance(otherJob, mustafa, FRIDAY, WAGE, new BigDecimal("0.5"), null)));

        List<AttendanceConflict> fullDay = service.findConflicts(JOB_ID, List.of(FRIDAY),
                List.of(new AttendanceEntry(MUSTAFA_ID, WAGE, BigDecimal.ONE)));
        List<AttendanceConflict> halfDay = service.findConflicts(JOB_ID, List.of(FRIDAY),
                List.of(new AttendanceEntry(MUSTAFA_ID, WAGE, new BigDecimal("0.5"))));

        assertThat(fullDay).singleElement().satisfies(conflict -> {
            assertThat(conflict.jobLabel()).isEqualTo("Mehmet Bey – Pano arızası");
            assertThat(conflict.dayFactorTotal()).isEqualByComparingTo("1.5");
            assertThat(conflict.exceedsFullDay()).isTrue();
        });
        assertThat(halfDay).singleElement().satisfies(conflict -> assertThat(conflict.exceedsFullDay()).isFalse());
    }

    @Test
    void findsDatesOutsideTheJobRange() {
        Job boundedJob = new Job(completedJob.getCustomer(), JobType.SITE, "Blok B", null, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 12), JobStatus.ACTIVE, null, null, false, null);
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(boundedJob));

        List<LocalDate> outside = service.findDatesOutsideJob(JOB_ID, List.of(
                LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 5), LocalDate.of(2026, 8, 31)));

        assertThat(outside).containsExactly(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 13));
    }

    @Test
    void detectsDayFactorUpdateThatExceedsOneFullDay() {
        Attendance edited = attendance(10L, "0.5");
        Attendance otherJobRow = attendance(11L, "0.5");
        when(attendanceRepository.findById(10L)).thenReturn(Optional.of(edited));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(MUSTAFA_ID, FRIDAY))
                .thenReturn(List.of(edited, otherJobRow));

        assertThat(service.exceedsFullDayAfterUpdate(10L, BigDecimal.ONE)).isTrue();
        assertThat(service.exceedsFullDayAfterUpdate(10L, new BigDecimal("0.5"))).isFalse();
    }

    private Attendance attendance(Long id, String dayFactor) {
        Attendance attendance = mock(Attendance.class);
        when(attendance.getId()).thenReturn(id);
        when(attendance.getEmployee()).thenReturn(mustafa);
        when(attendance.getAttendanceDate()).thenReturn(FRIDAY);
        when(attendance.getDayFactor()).thenReturn(new BigDecimal(dayFactor));
        return attendance;
    }

    private static Employee employee(Long id, String name) {
        Employee employee = mock(Employee.class);
        when(employee.getId()).thenReturn(id);
        when(employee.getName()).thenReturn(name);
        return employee;
    }
}
