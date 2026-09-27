package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.EnumLabels;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes one row per job (site or service) to a single-sheet workbook, for
 * the "tarih aralığına göre Excel dışa aktarım" requirement.
 */
@Component
public class JobRangeExcelExportGenerator {

    private static final String[] HEADERS = {
            "Tarih", "Müşteri", "İş Adı", "Tip", "Durum", "Satış (KDV'siz)", "KDV Oranı", "KDV Tutarı",
            "Genel Toplam (KDV'li)", "Puantaj Günü", "Yevmiye Toplamı", "Maliyet (KDV'siz)", "Kâr", "Tahsil Edilen",
            "Kalan"
    };

    public void export(List<Job> jobs, Map<Long, JobSummary> summaries, Path outputFile) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            Sheet sheet = workbook.createSheet("İşler");
            writeHeaderRow(sheet);

            int rowIndex = 1;
            for (Job job : jobs) {
                JobSummary summary = summaries.get(job.getId());
                writeJobRow(sheet, rowIndex++, job, summary);
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeHeaderRow(Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            row.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    private void writeJobRow(Sheet sheet, int rowIndex, Job job, JobSummary summary) {
        Row row = sheet.createRow(rowIndex);
        setCell(row, 0, job.getStartDate() == null ? "" : job.getStartDate().format(Bicimlendirici.DATE));
        setCell(row, 1, job.getCustomer().getName());
        setCell(row, 2, job.getName());
        setCell(row, 3, EnumLabels.label(job.getType()));
        setCell(row, 4, EnumLabels.label(job.getStatus()));
        setCell(row, 5, summary.saleExcludingVat());
        setCell(row, 6, vatRatesText(summary.saleVat()));
        setCell(row, 7, summary.saleVatAmount());
        setCell(row, 8, summary.saleIncludingVat());
        setCell(row, 9, summary.attendanceDayCount());
        setCell(row, 10, summary.attendanceTotal());
        setCell(row, 11, summary.costExcludingVat());
        setCell(row, 12, summary.profit());
        setCell(row, 13, summary.collectedTotal());
        setCell(row, 14, summary.remaining());
    }

    private String vatRatesText(VatBreakdown vat) {
        return vat.rates().stream()
                .map(rate -> "%" + rate.rate())
                .collect(Collectors.joining(", "));
    }

    private void setCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value);
    }

    private void setCell(Row row, int column, java.math.BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }
}
