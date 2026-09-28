package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.RecentActivity;
import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job figures ("IsOzeti") for the screens. Everything returned passes through
 * {@link FinancialDataMasker}, so a MANAGER never receives cost, profit,
 * receivable, payment, purchase or wage figures. The dashboard and every
 * site are ADMIN only; a MANAGER only loads services.
 */
@Service
public class JobSummaryService {

    private static final Set<RecentActivity.Kind> NON_FINANCIAL_ACTIVITIES =
            EnumSet.of(RecentActivity.Kind.JOB_OPENED, RecentActivity.Kind.MATERIAL);

    private final JobRepository jobRepository;
    private final MaterialItemRepository materialItemRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final FinancialDataMasker masker;
    private final AccessControl accessControl;
    private final JobSummaryCalculator calculator = new JobSummaryCalculator();
    private final DashboardCalculator dashboardCalculator = new DashboardCalculator();

    public JobSummaryService(JobRepository jobRepository, MaterialItemRepository materialItemRepository,
            AttendanceRepository attendanceRepository, PaymentRepository paymentRepository,
            FinancialDataMasker masker, AccessControl accessControl) {
        this.jobRepository = jobRepository;
        this.materialItemRepository = materialItemRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.masker = masker;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public JobSummary summarize(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        accessControl.requireAccess(job.getType());

        return masker.mask(calculator.calculate(
                job,
                materialItemRepository.findByJobIdOrderByItemDateAsc(jobId),
                attendanceRepository.findByJobIdOrderByAttendanceDateAsc(jobId),
                paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId)));
    }

    /**
     * What the customer still owes on a job (zero when fully paid or
     * overpaid); empty for a user who may not see receivables.
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> outstandingBalance(Long jobId) {
        BigDecimal remaining = summarize(jobId).remaining();
        if (remaining == null) {
            return Optional.empty();
        }
        return Optional.of(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
    }

    /** Reloads a single job (4 queries) so only its panel needs refreshing. */
    @Transactional(readOnly = true)
    public JobDetail loadJobDetail(Long jobId) {
        Job job = jobRepository.findWithCustomerById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        accessControl.requireAccess(job.getType());
        List<MaterialItem> materials = materialItemRepository.findByJobIdOrderByItemDateAsc(jobId);
        List<Attendance> attendances = attendanceRepository.findByJobIdOrderByAttendanceDateAsc(jobId);
        List<Payment> payments = paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId);
        return masker.mask(new JobDetail(job, calculator.calculate(job, materials, attendances, payments), materials,
                attendances, payments));
    }

    /**
     * VAT split of a job that is still being filled in (not saved yet), for
     * live totals in the service form.
     */
    public VatBreakdown previewSale(Job draftJob, List<MaterialItem> draftMaterials) {
        return calculator.saleBreakdown(draftJob, draftMaterials);
    }

    /**
     * Loads every job (sites and services) together with its summary in a
     * fixed number of queries (one per table), regardless of how many jobs
     * exist, so no screen ever queries per job. ADMIN only.
     */
    @Transactional(readOnly = true)
    public JobBoard loadBoard() {
        accessControl.requireAdmin();
        return masker.mask(loadFullBoard(job -> true));
    }

    /** Like {@link #loadBoard()} but services only, for both roles. */
    @Transactional(readOnly = true)
    public JobBoard loadServiceBoard() {
        return masker.mask(loadFullBoard(job -> job.getType() == JobType.SERVICE));
    }

    /** Main-screen cards and latest activities (ADMIN only). */
    @Transactional(readOnly = true)
    public DashboardFigures loadDashboard(YearMonth month) {
        accessControl.requireAdmin();
        Set<RecentActivity.Kind> kinds = masker.isMasking()
                ? NON_FINANCIAL_ACTIVITIES : EnumSet.allOf(RecentActivity.Kind.class);
        return masker.mask(dashboardCalculator.calculate(loadFullBoard(job -> true), month, kinds));
    }

    /** "Bekleyen servis ödemesi" of an already-loaded board; {@code null} when it was masked. */
    public BigDecimal pendingServicePayment(JobBoard board) {
        return masker.isMasking() ? null : dashboardCalculator.pendingServicePayment(board);
    }

    private JobBoard loadFullBoard(Predicate<Job> included) {
        List<Job> jobs = jobRepository.findAll().stream().filter(included).toList();
        Set<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toSet());
        Map<Long, List<MaterialItem>> materialsByJob = groupByJobId(materialItemRepository.findAll(),
                item -> item.getJob().getId(), jobIds);
        Map<Long, List<Attendance>> attendancesByJob = groupByJobId(attendanceRepository.findAll(),
                attendance -> attendance.getJob().getId(), jobIds);
        Map<Long, List<Payment>> paymentsByJob = groupByJobId(paymentRepository.findAll(),
                payment -> payment.getJob().getId(), jobIds);

        Map<Long, JobSummary> summaries = jobs.stream().collect(Collectors.toMap(
                Job::getId,
                job -> calculator.calculate(
                        job,
                        materialsByJob.getOrDefault(job.getId(), List.of()),
                        attendancesByJob.getOrDefault(job.getId(), List.of()),
                        paymentsByJob.getOrDefault(job.getId(), List.of()))));

        return new JobBoard(jobs, summaries, materialsByJob, attendancesByJob, paymentsByJob);
    }

    private <T> Map<Long, List<T>> groupByJobId(List<T> items, Function<T, Long> jobIdExtractor, Set<Long> jobIds) {
        return items.stream()
                .filter(item -> jobIds.contains(jobIdExtractor.apply(item)))
                .collect(Collectors.groupingBy(jobIdExtractor));
    }
}
