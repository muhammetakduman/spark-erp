package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobSummaryService {

    private final JobRepository jobRepository;
    private final MaterialItemRepository materialItemRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final JobSummaryCalculator calculator = new JobSummaryCalculator();
    private final DashboardCalculator dashboardCalculator = new DashboardCalculator();

    public JobSummaryService(JobRepository jobRepository, MaterialItemRepository materialItemRepository,
            AttendanceRepository attendanceRepository, PaymentRepository paymentRepository) {
        this.jobRepository = jobRepository;
        this.materialItemRepository = materialItemRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public JobSummary summarize(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));

        return calculator.calculate(
                job,
                materialItemRepository.findByJobIdOrderByItemDateAsc(jobId),
                attendanceRepository.findByJobIdOrderByAttendanceDateAsc(jobId),
                paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId));
    }

    /** What the customer still owes on a job (zero when fully paid or overpaid). */
    @Transactional(readOnly = true)
    public BigDecimal outstandingBalance(Long jobId) {
        BigDecimal remaining = summarize(jobId).remaining();
        return remaining == null || remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    /** Reloads a single job (4 queries) so only its panel needs refreshing. */
    @Transactional(readOnly = true)
    public JobDetail loadJobDetail(Long jobId) {
        Job job = jobRepository.findWithCustomerById(jobId)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        List<MaterialItem> materials = materialItemRepository.findByJobIdOrderByItemDateAsc(jobId);
        List<Attendance> attendances = attendanceRepository.findByJobIdOrderByAttendanceDateAsc(jobId);
        List<Payment> payments = paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId);
        return new JobDetail(job, calculator.calculate(job, materials, attendances, payments), materials, attendances,
                payments);
    }

    /**
     * VAT split of a job that is still being filled in (not saved yet), for
     * live totals in the service form.
     */
    public VatBreakdown previewSale(Job draftJob, List<MaterialItem> draftMaterials) {
        return calculator.saleBreakdown(draftJob, draftMaterials);
    }

    /**
     * Loads every job together with its summary in a fixed number of
     * queries (one per table), regardless of how many jobs exist, so the
     * main screen never queries per job.
     */
    @Transactional(readOnly = true)
    public JobBoard loadBoard() {
        List<Job> jobs = jobRepository.findAll();
        Map<Long, List<MaterialItem>> materialsByJob = groupByJobId(materialItemRepository.findAll(),
                item -> item.getJob().getId());
        Map<Long, List<Attendance>> attendancesByJob = groupByJobId(attendanceRepository.findAll(),
                attendance -> attendance.getJob().getId());
        Map<Long, List<Payment>> paymentsByJob = groupByJobId(paymentRepository.findAll(),
                payment -> payment.getJob().getId());

        Map<Long, JobSummary> summaries = jobs.stream().collect(Collectors.toMap(
                Job::getId,
                job -> calculator.calculate(
                        job,
                        materialsByJob.getOrDefault(job.getId(), List.of()),
                        attendancesByJob.getOrDefault(job.getId(), List.of()),
                        paymentsByJob.getOrDefault(job.getId(), List.of()))));

        return new JobBoard(jobs, summaries, materialsByJob, attendancesByJob, paymentsByJob);
    }

    /** Main-screen cards for an already-loaded board; runs no queries. */
    public DashboardFigures dashboard(JobBoard board, YearMonth month) {
        return dashboardCalculator.calculate(board, month);
    }

    private <T> Map<Long, List<T>> groupByJobId(List<T> items, Function<T, Long> jobIdExtractor) {
        return items.stream().collect(Collectors.groupingBy(jobIdExtractor));
    }
}
