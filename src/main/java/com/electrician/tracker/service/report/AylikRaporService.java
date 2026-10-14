package com.electrician.tracker.service.report;

import java.nio.file.Path;
import java.time.YearMonth;

import com.electrician.tracker.dto.job.JobBoard;
import com.electrician.tracker.dto.report.MonthlyReport;
import com.electrician.tracker.dto.report.ReportJobFilter;
import com.electrician.tracker.dto.report.YearlyReport;
import com.electrician.tracker.report.excel.MonthlyReportExcelExportGenerator;
import com.electrician.tracker.report.pdf.MonthlyReportPdfGenerator;
import com.electrician.tracker.service.job.JobSummaryService;
import com.electrician.tracker.service.user.AccessControl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Aylık Rapor": revenue, cost, profit and uncollected money per month and
 * per year. Built on {@link JobSummaryService#loadBoard()} (one query per
 * table) and its job summaries, so the figures match the main screen and
 * are never calculated a second way. The whole report is ADMIN only.
 */
@Service
public class AylikRaporService {

    private final JobSummaryService jobSummaryService;
    private final MonthlyReportExcelExportGenerator excelGenerator;
    private final MonthlyReportPdfGenerator pdfGenerator;
    private final AccessControl accessControl;
    private final AylikRaporHesaplayici hesaplayici = new AylikRaporHesaplayici();

    public AylikRaporService(JobSummaryService jobSummaryService, MonthlyReportExcelExportGenerator excelGenerator,
            MonthlyReportPdfGenerator pdfGenerator, AccessControl accessControl) {
        this.jobSummaryService = jobSummaryService;
        this.excelGenerator = excelGenerator;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
    }

    /** Loads everything once; the screen then switches months and filters without querying again. */
    @Transactional(readOnly = true)
    public JobBoard loadBoard() {
        accessControl.requireAdmin();
        return jobSummaryService.loadBoard();
    }

    public MonthlyReport monthly(JobBoard board, YearMonth month, ReportJobFilter filter) {
        accessControl.requireAdmin();
        return hesaplayici.monthly(board, month, filter);
    }

    public YearlyReport yearly(JobBoard board, int year, ReportJobFilter filter) {
        accessControl.requireAdmin();
        return hesaplayici.yearly(board, year, filter);
    }

    @Transactional(readOnly = true)
    public MonthlyReport monthly(YearMonth month, ReportJobFilter filter) {
        return monthly(loadBoard(), month, filter);
    }

    @Transactional(readOnly = true)
    public YearlyReport yearly(int year, ReportJobFilter filter) {
        return yearly(loadBoard(), year, filter);
    }

    public void exportExcel(MonthlyReport report, Path outputFile) {
        accessControl.requireAdmin();
        excelGenerator.export(report, outputFile);
    }

    public void exportPdf(MonthlyReport report, Path outputFile) {
        accessControl.requireAdmin();
        pdfGenerator.generate(report, outputFile);
    }
}
