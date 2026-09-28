package com.electrician.tracker.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.report.AttendanceMatrixExcelExportGenerator;
import com.electrician.tracker.report.JobPdfReportGenerator;
import com.electrician.tracker.report.JobRangeExcelExportGenerator;
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
        boolean includePayments = accessControl.canViewFinancials();
        List<Payment> payments = includePayments ? paymentService.findByJob(jobId) : List.of();
        jobPdfReportGenerator.generate(job, summary, materials, payments, companyService.get(), includePayments,
                outputFile);
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
        jobRangeExcelExportGenerator.export(jobsInRange, board.summaries(), board.materialsByJob(), outputFile,
                accessControl.canViewFinancials());
    }
}
