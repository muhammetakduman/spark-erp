package com.electrician.tracker.service.report;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.job.JobType;
import com.electrician.tracker.domain.job.Payment;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.dto.job.JobBoard;
import com.electrician.tracker.dto.job.JobSummary;
import com.electrician.tracker.dto.material.MaterialGroup;
import com.electrician.tracker.report.excel.AttendanceMatrixExcelExportGenerator;
import com.electrician.tracker.report.excel.JobRangeExcelExportGenerator;
import com.electrician.tracker.report.pdf.JobPdfReportGenerator;
import com.electrician.tracker.service.employee.AttendanceService;
import com.electrician.tracker.service.job.JobService;
import com.electrician.tracker.service.job.JobSummaryService;
import com.electrician.tracker.service.job.PaymentService;
import com.electrician.tracker.service.material.MaterialService;
import com.electrician.tracker.service.settings.CompanyService;
import com.electrician.tracker.service.user.AccessControl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gathers the already-fetched data the {@code report} package's generators
 * need, keeping repository access inside the service layer.
 */
@Service
public class ReportService {

    private final JobService jobService;
    private final MaterialService materialService;
    private final PaymentService paymentService;
    private final JobSummaryService jobSummaryService;
    private final JobPdfReportGenerator jobPdfReportGenerator;
    private final JobRangeExcelExportGenerator jobRangeExcelExportGenerator;
    private final AttendanceService attendanceService;
    private final AttendanceMatrixExcelExportGenerator attendanceMatrixExcelExportGenerator;
    private final CompanyService companyService;
    private final AccessControl accessControl;

    public ReportService(JobService jobService, MaterialService materialService, PaymentService paymentService,
            JobSummaryService jobSummaryService, JobPdfReportGenerator jobPdfReportGenerator,
            JobRangeExcelExportGenerator jobRangeExcelExportGenerator, AttendanceService attendanceService,
            AttendanceMatrixExcelExportGenerator attendanceMatrixExcelExportGenerator, CompanyService companyService,
            AccessControl accessControl) {
        this.jobService = jobService;
        this.materialService = materialService;
        this.paymentService = paymentService;
        this.jobSummaryService = jobSummaryService;
        this.jobPdfReportGenerator = jobPdfReportGenerator;
        this.jobRangeExcelExportGenerator = jobRangeExcelExportGenerator;
        this.attendanceService = attendanceService;
        this.attendanceMatrixExcelExportGenerator = attendanceMatrixExcelExportGenerator;
        this.companyService = companyService;
        this.accessControl = accessControl;
    }

    /** The customer's job handout; payments and the remaining balance only when the user may see them. */
    @Transactional(readOnly = true)
    public void generateJobPdf(Long jobId, Path outputFile) {
        Job job = jobService.findById(jobId);
        JobSummary summary = jobSummaryService.summarize(jobId);
        List<MaterialItem> materials = materialService.findByJob(jobId);
        // A site's materials are printed by group; a service keeps its short flat list.
        List<MaterialGroup> groups = job.getType() == JobType.SITE
                ? materialService.groupBreakdown(materials).groups() : List.of();
        boolean includePayments = accessControl.canViewFinancials();
        List<Payment> payments = includePayments ? paymentService.findByJob(jobId) : List.of();
        jobPdfReportGenerator.generate(job, summary, materials, groups, payments, companyService.get(),
                includePayments, outputFile);
    }

    @Transactional(readOnly = true)
    public void exportAttendanceMatrix(LocalDate from, LocalDate to, Path outputFile) {
        attendanceMatrixExcelExportGenerator.export(attendanceService.matrix(from, to), outputFile);
    }

    @Transactional(readOnly = true)
    public void exportJobsByDateRange(LocalDate start, LocalDate end, Path outputFile) {
        JobBoard board = jobSummaryService.loadBoard();
        List<Job> jobsInRange = board.jobs().stream()
                .filter(job -> job.getStartDate() != null
                        && !job.getStartDate().isBefore(start)
                        && !job.getStartDate().isAfter(end))
                .sorted(Comparator.comparing(Job::getStartDate))
                .toList();
        Map<Long, List<MaterialGroup>> groupsByJob = jobsInRange.stream().collect(Collectors.toMap(Job::getId,
                job -> materialService.groupBreakdown(board.materialsFor(job.getId())).groups()));
        jobRangeExcelExportGenerator.export(jobsInRange, board.summaries(), groupsByJob, outputFile,
                accessControl.canViewFinancials());
    }
}
