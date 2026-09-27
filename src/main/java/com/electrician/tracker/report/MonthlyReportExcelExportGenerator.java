package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;

import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.EnumLabels;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Writes a monthly report to three sheets (jobs with a total row, material
 * summary, attendance summary); amounts are numeric cells, as on screen.
 */
@Component
public class MonthlyReportExcelExportGenerator {

    private static final String[] JOB_HEADERS = {
            "Tarih", "Müşteri", "İş", "Tip", "Malzeme (alış)", "Puantaj Günü", "Yevmiye (kâra dahil değil)",
            "Ciro (KDV'siz)", "KDV", "Maliyet", "Kâr", "Tahsil Edilen", "Tahsil Edilmeyen"
    };
    private static final String[] MATERIAL_HEADERS = { "Ürün", "Miktar", "Birim", "Toplam Alış", "Toplam Satış" };
    private static final String[] WAGE_HEADERS = { "Personel", "Gün", "Toplam Yevmiye" };
    private static final int FIRST_FIGURE_COLUMN = 4;

    public void export(MonthlyReport report, Path outputFile) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            writeJobs(workbook.createSheet("İşler"), report);
            writeMaterials(workbook.createSheet("Malzeme Özeti"), report);
            writeWages(workbook.createSheet("Yevmiye Özeti"), report);
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeJobs(Sheet sheet, MonthlyReport report) {
        writeHeader(sheet, JOB_HEADERS);
        int rowIndex = 1;
        for (MonthlyReportRow job : report.rows()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(Bicimlendirici.date(job.date()));
            row.createCell(1).setCellValue(job.customerName());
            row.createCell(2).setCellValue(job.jobName() == null ? "" : job.jobName());
            row.createCell(3).setCellValue(EnumLabels.label(job.jobType()));
            writeFigures(row, job.figures());
        }
        Row totalRow = sheet.createRow(rowIndex);
        totalRow.createCell(0).setCellValue("TOPLAM");
        writeFigures(totalRow, report.totals());
        autoSize(sheet, JOB_HEADERS.length);
    }

    private void writeFigures(Row row, ReportTotals figures) {
        BigDecimal[] values = { figures.materialCost(), figures.attendanceDays(), figures.wageTotal(),
                figures.revenue(), figures.vatAmount(), figures.cost(), figures.profit(), figures.collected(),
                figures.uncollected() };
        for (int i = 0; i < values.length; i++) {
            setNumber(row, FIRST_FIGURE_COLUMN + i, values[i]);
        }
    }

    private void writeMaterials(Sheet sheet, MonthlyReport report) {
        writeHeader(sheet, MATERIAL_HEADERS);
        int rowIndex = 1;
        for (MaterialSummaryLine line : report.materials()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(line.productName());
            setNumber(row, 1, line.quantity());
            row.createCell(2).setCellValue(EnumLabels.label(line.unit()));
            setNumber(row, 3, line.purchaseTotal());
            setNumber(row, 4, line.saleTotal());
        }
        autoSize(sheet, MATERIAL_HEADERS.length);
    }

    private void writeWages(Sheet sheet, MonthlyReport report) {
        writeHeader(sheet, WAGE_HEADERS);
        int rowIndex = 1;
        for (EmployeeWageSummary wage : report.wages()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(wage.employeeName());
            setNumber(row, 1, wage.dayCount());
            setNumber(row, 2, wage.totalWage());
        }
        autoSize(sheet, WAGE_HEADERS.length);
    }

    private void writeHeader(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private void setNumber(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
