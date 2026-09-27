package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.AttendanceConflict;
import com.electrician.tracker.dto.AttendanceEntry;
import com.electrician.tracker.dto.AttendanceMatrix;
import com.electrician.tracker.dto.AttendancePreview;
import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.dto.EmployeeAttendanceReport;
import com.electrician.tracker.dto.EmployeeJobAttendance;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.EmployeeRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attendance ("puantaj"): every worked day is its own row, for any job type
 * and status. Batches expand to one row per date × employee.
 */
@Service
public class AttendanceService {

    private static final long MAX_RANGE_DAYS = 366;

    private final AttendanceRepository attendanceRepository;
    private final JobRepository jobRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, JobRepository jobRepository,
            EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.jobRepository = jobRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<Attendance> findByJob(Long jobId) {
        return attendanceRepository.findByJobIdOrderByAttendanceDateAsc(jobId);
    }

    /** Dates from {@code from} to {@code to} inclusive, optionally skipping weekend days. */
    public List<LocalDate> expandDates(LocalDate from, LocalDate to, boolean skipSaturday, boolean skipSunday) {
        if (from == null || to == null) {
            throw new ValidationException("error.attendance.date.required");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("error.attendance.range.endBeforeStart");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new ValidationException("error.attendance.range.tooLong");
        }
        return from.datesUntil(to.plusDays(1))
                .filter(date -> !(skipSaturday && date.getDayOfWeek() == DayOfWeek.SATURDAY))
                .filter(date -> !(skipSunday && date.getDayOfWeek() == DayOfWeek.SUNDAY))
                .toList();
    }

    public AttendancePreview preview(List<LocalDate> dates, List<AttendanceEntry> entries) {
        BigDecimal dateCount = BigDecimal.valueOf(dates.size());
        BigDecimal factorPerDate = BigDecimal.ZERO;
        BigDecimal amountPerDate = BigDecimal.ZERO;
        for (AttendanceEntry entry : entries) {
            BigDecimal factor = factorOf(entry);
            factorPerDate = factorPerDate.add(factor);
            if (entry.dailyWage() != null) {
                amountPerDate = amountPerDate.add(entry.dailyWage().multiply(factor));
            }
        }
        return new AttendancePreview(dates.size(), entries.size(), factorPerDate.multiply(dateCount),
                amountPerDate.multiply(dateCount));
    }

    /**
     * Entries the batch would place on employees already recorded on another
     * job that day, with the day-factor total the date would reach.
     */
    @Transactional(readOnly = true)
    public List<AttendanceConflict> findConflicts(Long jobId, List<LocalDate> dates, List<AttendanceEntry> entries) {
        if (dates.isEmpty() || entries.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> newFactorByEmployee = entries.stream()
                .collect(Collectors.toMap(AttendanceEntry::employeeId, AttendanceService::factorOf, BigDecimal::add));
        List<Attendance> others = attendanceRepository.findOnOtherJobs(jobId, newFactorByEmployee.keySet(), dates);
        Map<String, BigDecimal> otherFactorByDay = others.stream().collect(Collectors.toMap(
                a -> key(a.getEmployee().getId(), a.getAttendanceDate()), AttendanceMath::factorOf, BigDecimal::add));
        return others.stream()
                .map(a -> new AttendanceConflict(a.getEmployee().getName(), a.getAttendanceDate(),
                        JobLabels.full(a.getJob()),
                        otherFactorByDay.get(key(a.getEmployee().getId(), a.getAttendanceDate()))
                                .add(newFactorByEmployee.get(a.getEmployee().getId()))))
                .toList();
    }

    /** Dates outside the job's start–end range (warned, not blocked); an open end never limits. */
    @Transactional(readOnly = true)
    public List<LocalDate> findDatesOutsideJob(Long jobId, List<LocalDate> dates) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        return dates.stream()
                .filter(date -> (job.getStartDate() != null && date.isBefore(job.getStartDate()))
                        || (job.getEndDate() != null && date.isAfter(job.getEndDate())))
                .sorted()
                .toList();
    }

    /** Whether changing a row's day factor would put its employee over one full day on that date. */
    @Transactional(readOnly = true)
    public boolean exceedsFullDayAfterUpdate(Long id, BigDecimal newDayFactor) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.attendance.notFound"));
        BigDecimal others = AttendanceMath.dayCount(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(attendance.getEmployee().getId(), attendance.getAttendanceDate())
                .stream()
                .filter(other -> !other.getId().equals(id))
                .toList());
        return others.add(newDayFactor).compareTo(AttendanceMath.FULL_DAY) > 0;
    }

    /**
     * Creates one row per date × employee. Rows already present for the same
     * job, employee and date are skipped silently and counted.
     */
    @Transactional
    public AttendanceSaveResult saveBatch(Long jobId, List<LocalDate> dates, List<AttendanceEntry> entries) {
        validateBatch(dates, entries);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        Map<Long, Employee> employees = loadEmployees(entries);
        Set<String> existingKeys = attendanceRepository.findByJobAndDates(jobId, dates).stream()
                .map(a -> key(a.getEmployee().getId(), a.getAttendanceDate()))
                .collect(Collectors.toSet());

        List<Attendance> toCreate = new ArrayList<>();
        int skipped = 0;
        for (LocalDate date : dates) {
            for (AttendanceEntry entry : entries) {
                if (existingKeys.contains(key(entry.employeeId(), date))) {
                    skipped++;
                } else {
                    toCreate.add(new Attendance(job, employees.get(entry.employeeId()), date,
                            entry.dailyWage(), entry.dayFactor(), null));
                }
            }
        }
        attendanceRepository.saveAll(toCreate);
        return new AttendanceSaveResult(toCreate.size(), skipped);
    }

    @Transactional
    public Attendance update(Long id, BigDecimal dailyWage, BigDecimal dayFactor, String note) {
        validateWage(dailyWage);
        validateFactor(dayFactor);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.attendance.notFound"));
        attendance.setDailyWage(dailyWage);
        attendance.setDayFactor(dayFactor);
        attendance.setNote(note == null || note.isBlank() ? null : note.trim());
        return attendance;
    }

    @Transactional
    public void delete(Long id) {
        attendanceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public EmployeeAttendanceReport reportForEmployee(Long employeeId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Attendance> rows = attendanceRepository.findForEmployeeBetween(employeeId, from, to);
        List<EmployeeJobAttendance> jobs = AttendanceMath.groupBy(rows, Attendance::getJob).entrySet().stream()
                .map(entry -> toJobAttendance(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(group -> group.days().get(0).date()))
                .toList();
        return new EmployeeAttendanceReport(jobs, AttendanceMath.dayCount(rows), AttendanceMath.totalWage(rows));
    }

    @Transactional(readOnly = true)
    public AttendanceMatrix matrix(LocalDate from, LocalDate to) {
        List<LocalDate> dates = expandDates(from, to, false, false);
        List<Attendance> rows = attendanceRepository.findAllBetween(from, to);
        List<AttendanceMatrix.Row> matrixRows = AttendanceMath.groupBy(rows, Attendance::getEmployee).entrySet()
                .stream()
                .map(entry -> new AttendanceMatrix.Row(entry.getKey().getId(), entry.getKey().getName(),
                        cellsByDate(entry.getValue())))
                .toList();
        return new AttendanceMatrix(dates, matrixRows);
    }

    private Map<LocalDate, List<AttendanceMatrix.Cell>> cellsByDate(List<Attendance> rows) {
        return rows.stream().collect(Collectors.groupingBy(Attendance::getAttendanceDate,
                Collectors.mapping(a -> new AttendanceMatrix.Cell(JobLabels.shortName(a.getJob()), a.getDayFactor()),
                        Collectors.toList())));
    }

    private EmployeeJobAttendance toJobAttendance(Job job, List<Attendance> rows) {
        return new EmployeeJobAttendance(job.getId(), job.getType(), JobLabels.full(job),
                AttendanceMath.dayCount(rows), AttendanceMath.totalWage(rows), AttendanceMath.workedDays(rows));
    }

    private Map<Long, Employee> loadEmployees(List<AttendanceEntry> entries) {
        List<Long> ids = entries.stream().map(AttendanceEntry::employeeId).toList();
        Map<Long, Employee> employees = employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        if (employees.size() != Set.copyOf(ids).size()) {
            throw new NotFoundException("error.employee.notFound");
        }
        return employees;
    }

    private void validateBatch(List<LocalDate> dates, List<AttendanceEntry> entries) {
        if (dates == null || dates.isEmpty()) {
            throw new ValidationException("error.attendance.date.required");
        }
        if (entries == null || entries.isEmpty()) {
            throw new ValidationException("error.attendance.employee.required");
        }
        for (AttendanceEntry entry : entries) {
            validateWage(entry.dailyWage());
            validateFactor(entry.dayFactor());
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("error.attendance.date.required");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("error.attendance.range.endBeforeStart");
        }
    }

    private void validateWage(BigDecimal wage) {
        if (wage == null || wage.signum() < 0) {
            throw new ValidationException("error.attendance.wage.negative");
        }
    }

    private void validateFactor(BigDecimal factor) {
        if (!AttendanceMath.isAllowedFactor(factor)) {
            throw new ValidationException("error.attendance.dayFactor.invalid");
        }
    }

    private static BigDecimal factorOf(AttendanceEntry entry) {
        return entry.dayFactor() == null ? AttendanceMath.FULL_DAY : entry.dayFactor();
    }

    private static String key(Long employeeId, LocalDate date) {
        return employeeId + "|" + date;
    }
}
